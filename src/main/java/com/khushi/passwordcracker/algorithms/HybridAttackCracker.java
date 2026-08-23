package com.khushi.passwordcracker.algorithms;

import com.khushi.passwordcracker.utils.HashUtil;
import com.khushi.passwordcracker.dictionary.Wordlist;

import java.util.concurrent.atomic.AtomicBoolean;

public class HybridAttackCracker {

    private int attemptsCount = 0;

    public String crack(String targetHash, int maxLength) {
        return crack(targetHash, maxLength, new AtomicBoolean(false));
    }

    // Cancellable overload - existing signature above still works unchanged.
    public String crack(String targetHash, int maxLength, AtomicBoolean cancelled) {

        attemptsCount = 0;

        // ==============================
        // PHASE 1: DICTIONARY ATTACK
        // ==============================

        for (String word : Wordlist.PASSWORDS) {

            if (cancelled.get()) {
                return "Not Found";
            }

            attemptsCount++;

            String hash = HashUtil.generateSHA256(word);

            if (hash.equals(targetHash)) {
                return word;
            }
        }

        // ==============================
        // PHASE 2: BRUTE FORCE ATTACK
        // ==============================

        String result = bruteForce("", maxLength, targetHash, cancelled);

        if (result != null) {
            return result;
        }

        return "Not Found";
    }


    private String bruteForce(
            String current,
            int maxLength,
            String targetHash,
            AtomicBoolean cancelled) {

        if (cancelled.get()) {
            return null;
        }

        // Check current password
        if (!current.isEmpty()) {

            attemptsCount++;

            String hash = HashUtil.generateSHA256(current);

            if (hash.equals(targetHash)) {
                return current;
            }
        }

        // Stop when maximum length is reached
        if (current.length() == maxLength) {
            return null;
        }

        // Lowercase characters
        String characters = "abcdefghijklmnopqrstuvwxyz";

        for (int i = 0; i < characters.length(); i++) {

            if (cancelled.get()) {
                return null;
            }

            String result = bruteForce(
                    current + characters.charAt(i),
                    maxLength,
                    targetHash,
                    cancelled
            );

            if (result != null) {
                return result;
            }
        }

        return null;
    }
    
    // Backward-compatible method
public String crack(String targetHash) {
    return crack(targetHash, 3);
}


    public int getAttemptsCount() {
        return attemptsCount;
    }
}