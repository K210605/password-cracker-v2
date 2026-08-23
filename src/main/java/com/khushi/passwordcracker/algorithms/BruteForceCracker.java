package com.khushi.passwordcracker.algorithms;

import com.khushi.passwordcracker.utils.HashUtil;

import java.util.concurrent.atomic.AtomicBoolean;

public class BruteForceCracker {

    private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyz";

    private static int attemptsCount = 0;

    public static String crackPassword(String targetHash, int maxLength) {
        return crackPassword(targetHash, maxLength, new AtomicBoolean(false));
    }

    // Cancellable overload - existing signature above still works unchanged.
    public static String crackPassword(String targetHash, int maxLength, AtomicBoolean cancelled) {

        attemptsCount = 0;

        for (int length = 1; length <= maxLength; length++) {

            if (cancelled.get()) {
                return null;
            }

            String result = generate("", length, targetHash, cancelled);

            if (result != null) {
                return result;
            }
        }

        return null;
    }

    private static String generate(
            String current,
            int remainingLength,
            String targetHash,
            AtomicBoolean cancelled) {

        if (cancelled.get()) {
            return null;
        }

        if (remainingLength == 0) {

            attemptsCount++;

            String hash = HashUtil.generateSHA256(current);

            if (hash.equalsIgnoreCase(targetHash)) {
                return current;
            }

            return null;
        }

        for (int i = 0; i < CHARACTERS.length(); i++) {

            if (cancelled.get()) {
                return null;
            }

            String result = generate(
                    current + CHARACTERS.charAt(i),
                    remainingLength - 1,
                    targetHash,
                    cancelled);

            if (result != null) {
                return result;
            }
        }

        return null;
    }

    public static int getAttemptsCount() {
        return attemptsCount;
    }
}