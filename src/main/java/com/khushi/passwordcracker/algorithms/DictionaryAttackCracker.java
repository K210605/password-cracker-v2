package com.khushi.passwordcracker.algorithms;

import com.khushi.passwordcracker.dictionary.Wordlist;
import com.khushi.passwordcracker.utils.HashUtil;

import java.util.concurrent.atomic.AtomicBoolean;

public class DictionaryAttackCracker {

    private static int attemptsCount = 0;

    public static String crackPassword(String targetHash) {
        return crackPassword(targetHash, new AtomicBoolean(false));
    }

    // Cancellable overload - existing signature above still works unchanged.
    public static String crackPassword(String targetHash, AtomicBoolean cancelled) {

        attemptsCount = 0;

        for (String word : Wordlist.PASSWORDS) {

            if (cancelled.get()) {
                return null;
            }

            attemptsCount++;

            String hash = HashUtil.generateSHA256(word);

            if (hash.equalsIgnoreCase(targetHash)) {
                return word;
            }
        }

        return null;
    }

    public static int getAttemptsCount() {
        return attemptsCount;
    }
}