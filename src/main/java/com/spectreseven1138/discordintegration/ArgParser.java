package com.spectreseven1138.discordintegration;

import java.util.List;

public class ArgParser {

    private int arg_count;
    private boolean pack;

    ArgParser(int arg_count) {
        this.pack = arg_count < 0;
        this.arg_count = arg_count;
        if (this.pack) {
            this.arg_count *= -1;
        }
    }

    String getInvalidAmountError() {
        if (pack) {
            return String.format("Got an invalid amount of arguments (expected at least %d)", arg_count);
        }
        return String.format("Got an invalid amount of arguments (expected %d)", arg_count);
    }

    String getInvalidAmountError(int got_count) {
        if (pack) {
            return String.format("Got an invalid amount of arguments (expected at least %d, got %d)", arg_count, got_count);
        }
        return String.format("Got an invalid amount of arguments (expected %d, got %d)", arg_count, got_count);
    }

    // Returns an error message on failure
    String parseArguments(String args, List<String> out) {

        if (arg_count == 0) {
            for (int i = 0; i < args.length(); i++) {
                if (args.charAt(i) != ' ') {
                    return getInvalidAmountError();
                }
            }
            return "";
        }

        if (pack) {
            String[] split = args.split(" ", arg_count);

            if (split.length != arg_count) {
                return getInvalidAmountError();
            }

            for (String arg : split) {
                out.add(arg);
            }

            return "";
        }

        String[] split = args.split(" ", arg_count);

        int split_length = 0;
        for (String arg : split) {
            boolean valid = false;
            for (int i = 0; i < arg.length(); i++) {
                if (arg.charAt(i) != ' ') {
                    valid = true;
                    break;
                }
            }
            if (valid) {
                split_length++;
                out.add(arg);
            }
        }

        if (split_length != arg_count) {
            return getInvalidAmountError(split_length);
        }

        String last = split[split.length - 1];
        for (int i = 0; i < last.length(); i++) {
            if (last.charAt(i) == ' ') {
                return getInvalidAmountError();
            }
        }

        return "";
    }
}
