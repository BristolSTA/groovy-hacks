package org.example.util;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Util {

    public enum OutputFormat {STDOUT, ERROR}

    /**
     * This prints output either to the stdout or errout, depending on the outputFormat enum. This also adds
     * formatting, such as adding the date and time.
     *
     * @param output       The string to be formatted and output
     * @param outputFormat Whether to print to stdout or errout
     */
    public static void printOutput(@NotNull String output, @NotNull OutputFormat outputFormat) {

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();

        String formatted = dtf.format(now) + " - " + output;

        if (outputFormat == OutputFormat.ERROR) {
            System.err.println(formatted);
        } else {
            System.out.println(formatted);
        }

    }

}
