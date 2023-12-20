package org.example.apiCalls;

import io.github.cdimascio.dotenv.Dotenv;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import static org.example.util.Util.OutputFormat.ERROR;
import static org.example.util.Util.printOutput;


public class API {

    private static final Dotenv ENV = Dotenv.configure().load();

    /**
     * Given the user's ID and the server's ID, get the nickname of the user within the server. If no username for
     * them is defined in the server, return either their global name, or username (in that order of preference)
     *
     * @param userId   The user's ID
     * @param serverId The server's ID
     * @return The user's nickname/global name/username
     */
    public static @NotNull String getServerNickname(@NotNull JDA jda, @NotNull String userId, @NotNull String serverId) {
        try {

            // Try to get the user's server-specific nickname
            String nickname =
                    Objects.requireNonNull(jda.getGuildById(serverId)).retrieveMemberById(userId).complete().getNickname();

            // If the user has no server nickname, then return their global nickname instead. Failing that, get their
            // username.
            return Objects.requireNonNullElseGet(nickname,
                    () -> Objects.requireNonNullElse(
                            jda.retrieveUserById(userId).complete().getGlobalName(),
                            jda.retrieveUserById(userId).complete().getName()));
        } catch (NullPointerException e) {
            printOutput(String.format("An error occurred when trying to get the name of a user with ID %s: %s", userId,
                    e.getMessage()), ERROR);
            return "";
        }

    }

    /**
     * Given a URL, set the POST method on it and add the NTFY token from .env for authorisation. Then return it
     * as a new HttpURLConnection
     *
     * @param url The URL to connect to
     * @return An HttpURLConnection to said URL
     */
    public static @NotNull HttpURLConnection createNTFYConn(@NotNull URL url) throws Exception {

        try {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");

            conn.setDoOutput(true);
            conn.setRequestProperty("Authorization", "Bearer " + ENV.get("NTFY_TOKEN"));

            return conn;
        } catch (IOException e) {
            throw new Exception("An error occurred when configuring a URL in createNTFYConn: " + e.getMessage());
        }
    }

    /**
     * Make the request on a given HttpURLConnection, and return the response
     *
     * @param conn The connection to make the request from
     * @return The response from the request
     */
    
    // It's good to keep this returning something, for the future & for the error-handling, even if we don't use it now
    @SuppressWarnings("UnusedReturnValue")
    public static @NotNull String makeRequest(@NotNull HttpURLConnection conn) {

        // Read the response as JSon
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();

            String responseLine;

            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }

            return String.valueOf(response);
        } catch (IOException e) {
            printOutput("An error occurred when calling makeRequest: " + e.getMessage(), ERROR);
            return "";
        }
    }


    /**
     * Start up the bot, using the "DISCORD_TOKEN" in the .env file
     *
     * @return A new JDA object, which is the instance of the bot
     */
    public static @NotNull JDA startBot() {

        JDABuilder builder = JDABuilder.createDefault(ENV.get("DISCORD_TOKEN"));

        // Set activity
        builder.setActivity(Activity.customStatus("Stealing your quotes"));

        // Set the intent to read message content
        builder.setEnabledIntents(GatewayIntent.MESSAGE_CONTENT);

        // Disable a load of stuff we don't need, to save bandwidth
        builder.disableCache(List.of(CacheFlag.VOICE_STATE, CacheFlag.EMOJI, CacheFlag.STICKER, CacheFlag.SCHEDULED_EVENTS));

        JDA jda = builder.build();

        try {
            jda.awaitReady();
        } catch (InterruptedException e) {
            printOutput("Thread was interrupted whilst trying to wait for JDA to be ready: " +
                    e.getMessage() + "\nExiting...", ERROR);
            System.exit(1);
        }

        return jda;
    }

}
