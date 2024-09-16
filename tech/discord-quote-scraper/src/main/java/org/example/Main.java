package org.example;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.jetbrains.annotations.NotNull;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.example.apiCalls.API.*;
import static org.example.util.Util.OutputFormat.ERROR;
import static org.example.util.Util.OutputFormat.STDOUT;
import static org.example.util.Util.printOutput;


public class Main {
    private static final Dotenv ENV = Dotenv.configure().load();
    private static final String OPENING_QUOTE_REGEX = "[“\"]";
    private static final String CLOSING_QUOTE_REGEX = "[”\"]";
    private static final String INSIDE_QUOTE_REGEX = "[^“\"”]+";
    private static final String FULL_QUOTE_REGEX =
            ".*" + OPENING_QUOTE_REGEX + INSIDE_QUOTE_REGEX + CLOSING_QUOTE_REGEX + ".*";

    public static void main(String[] args) {

        // Start the JDA, and wait for it to be ready
        JDA jda = startBot();

        MessageChannel channel = jda.getChannelById(MessageChannel.class, ENV.get("CHANNEL_ID"));

        if (channel == null) {
            printOutput(String.format("Could not get channel with ID %s. Exiting...", ENV.get("CHANNEL_ID")), ERROR);
            System.exit(1);
        }

        // Get the maximum number of quotes we need
        int maxMessages = Integer.parseInt(ENV.get("MAX_MESSAGES"));

        // Get a list of the max quotes we want
        List<Message> messages = getListOfMessages(channel, maxMessages);

        printOutput(String.format("%d messages fetched.", messages.size()), STDOUT);

        // Get only those messages which follow the quote format
        messages = messages.parallelStream().filter(x -> x.getContentRaw().matches(FULL_QUOTE_REGEX)).toList();

        // Filter quotes only to be those where people who've opted-in are mentioned
        messages = filterOptIn(messages);

        printOutput(String.format("%d quotes fetched.", messages.size()), STDOUT);

        // Get a random quote from the list
        Message randQuote = messages.get(new Random().nextInt(messages.size()));

        // Print for logging
        printOutput("Quote is: " + cleanContent(randQuote), STDOUT);

        // Try to send the quote as a push notification.
        // If this is true, then the message failed to send. In that case, try to send again and if it still
        // fails, just print the error and exit.
        if (pushMessage(randQuote)) {
            if (pushMessage(randQuote)) {
                printOutput("Failed to send the push notification; tried twice.", ERROR);
            } else {
                printOutput("Successfully sent push notification on the second attempt.", STDOUT);
            }
        } else {
            printOutput("Successfully sent push notification.", STDOUT);
        }

        // Exit without error
        System.exit(0);
    }

    /**
     * Given a message, we 'clean' its content, by replacing all IDs with the server nickname of the respective user
     *
     * @param message The message to be 'cleaned'
     * @return The 'clean' content of the message
     */
    private static @NotNull String cleanContent(@NotNull Message message) {
        String content = message.getContentRaw();

        // This matches things of the form <@ID> or <@!ID>
        String reString = "<@!?\\d{15,20}>";
        Pattern regex = Pattern.compile(reString);

        // Check there are any matches
        if (regex.matcher(content).find()) {
            Map<String, String> mentionsMap = new HashMap<>();

            // Create a map from IDs to names. If these name can't be fetched from the server, just use their username.
            for (User user : message.getMentions().getUsers()
            ) {
                mentionsMap.put(user.getId(), getServerNickname(message.getJDA(), user.getId(), Objects.requireNonNull(message.getGuildId())));
            }

            Matcher matcher = regex.matcher(content);

            // Perform the replacements
            while (matcher.find()) {
                String group = matcher.group();
                String id = group.replace("<@", "").replace("!", "").replace(">", "");
                content = content.replace(group, mentionsMap.get(id));
            }
        }

        return content;
    }

    /**
     * Send a notification through NTFY with the passed in quote message. Uses the key specified in .env
     *
     * @param message The message with the quote in
     * @return A boolean, true if an error occurred, false otherwise
     */
    private static boolean pushMessage(Message message) {

        try {
            // Form the URL from the NTFY URL
            String urlString = ENV.get("NTFY_SERVER");

            URL url = new URL(urlString);

            // Create the connection
            HttpURLConnection conn = createNTFYConn(url);

            // Add the content
            // Remove newlines from the title
            conn.setRequestProperty("Title", "\uD83D\uDD38" + cleanContent(message).replace("\n", "\uD83D\uDD38"));
            conn.setRequestProperty("Click", String.format("https://discord.com/channels/%s/%s/%s",
                    message.getGuild().getId(),
                    message.getChannelId(), message.getId()));

            // Add the user's avatar as an image
            conn.setRequestProperty("Attach", getAvatarURL(message.getMentions().getUsers().get(0)));

            // Write the body of the message (which in this case, is just the "Quoted by" string)
            OutputStream os = conn.getOutputStream();
            OutputStreamWriter osw = new OutputStreamWriter(os, StandardCharsets.UTF_8);

            // Get the name of the person who saved the quote
            osw.write("Quoted by: " + getServerNickname(message.getJDA(), message.getAuthor().getId(), Objects.requireNonNull(message.getGuildId())));
            osw.flush();
            osw.close();
            os.close();  //don't forget to close the OutputStream

            // Make the request
            makeRequest(conn);

            // Return false to indicate success
            return false;

        } catch (Exception e) {
            printOutput("An error occurred when calling pushMessage: " + e.getMessage(), ERROR);
            return true;
        }
    }

    /**
     * Given a User object, return the URL that points to their avatar. If it cannot be found, return a URL to the
     * STA logo (stored in the .env file).
     *
     * @param user The user to get the avatar of
     * @return A string, representing the URL of the user's avatar. If it cannot be found, return a URL to the STA logo.
     */

    private static @NotNull String getAvatarURL(@NotNull User user) {

        // Return the user's URL. If it can't be found, return a link to the STA logo
        return Objects.requireNonNullElse(user.getAvatarUrl(), ENV.get("STA_LOGO"));
    }


    /**
     * Get the most recent messages in a channel as a list and return them
     *
     * @param channel     The channel to get the messages from
     * @param maxMessages The maximum number of messages to fetch
     * @return The list of messages
     */
    private static @NotNull List<Message> getListOfMessages(@NotNull MessageChannel channel,
                                                            int maxMessages) {
        // Wait for this to complete before we return
        CompletableFuture<List<Message>> messageFutures =
                channel.getIterableHistory().takeAsync(maxMessages);

        // Wait for all messages to be retrieved before returning
        return messageFutures.join();

    }

    /**
     * This takes in a list of messages, and filters them to only be ones which directly mention only people who've
     * opted-in. This reads in the comma-separated list of IDs in the .env file.
     *
     * @param allMessages The list of all messages
     * @return A filtered list of messages from only those who've opted-in
     */
    private static @NotNull List<Message> filterOptIn(@NotNull List<Message> allMessages) {

        List<String> optIns = List.of(ENV.get("OPT_INS").split(","));

        // Only take messages which mention people, because they're the easiest to filter.
        // Then, filter only those where all mentions have opted-in

        return allMessages.parallelStream()
                // Make sure a message actually has some mentions
                .filter(m -> !m.getMentions().getUsers().isEmpty())
                // Make sure all mentioned people are in the opt-in list
                .filter(m -> new HashSet<>(optIns).containsAll(
                        m.getMentions().getUsers().stream().map(ISnowflake::getId)
                                .collect(Collectors.toSet())))
                .collect(Collectors.toList());
    }
}