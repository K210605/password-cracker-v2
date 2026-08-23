package com.khushi.passwordcracker.algorithms;

import com.khushi.passwordcracker.utils.HashUtil;

import java.util.concurrent.atomic.AtomicBoolean;

public class MaskAttackCracker {

    private int attemptsCount = 0;
    private String targetHashGlobal;
    private String foundResult = null;
    private AtomicBoolean cancelled = new AtomicBoolean(false);

    private static final char[] LOWERCASE = "abcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final char[] UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private static final char[] DIGITS = "0123456789".toCharArray();
    private static final char[] SYMBOLS = "!@#$%^&*".toCharArray();

    // mask example: "?u?l?l?l?d?d?d?d"
    public String crack(String targetHash, String mask) throws Exception {
        return crack(targetHash, mask, new AtomicBoolean(false));
    }

    // Cancellable overload - existing signature above still works unchanged.
    public String crack(String targetHash, String mask, AtomicBoolean cancelled) throws Exception {
        attemptsCount = 0;
        foundResult = null;
        this.targetHashGlobal = targetHash;
        this.cancelled = cancelled;

        String[] positions = parseMask(mask);
        generateFromMask(positions, new StringBuilder(), 0);

        return (foundResult != null) ? foundResult : "Not Found";
    }

    // Mask ko positions ke array mein todna, e.g. ["?u","?l","?l","?d"]
    private String[] parseMask(String mask) {
        java.util.List<String> parts = new java.util.ArrayList<>();
        for (int i = 0; i < mask.length(); i += 2) {
            parts.add(mask.substring(i, i + 2));
        }
        return parts.toArray(new String[0]);
    }

    private char[] getCharsetForType(String type) {
        switch (type) {
            case "?l": return LOWERCASE;
            case "?u": return UPPERCASE;
            case "?d": return DIGITS;
            case "?s": return SYMBOLS;
            default: throw new IllegalArgumentException("Unknown mask type: " + type);
        }
    }

    private void generateFromMask(String[] positions, StringBuilder current, int index) throws Exception {
        if (foundResult != null || cancelled.get()) return; // already found, or stopped by user

        if (index == positions.length) {
            attemptsCount++;
            String candidate = current.toString();
            String candidateHash = HashUtil.generateSHA256(candidate);
            if (candidateHash.equals(targetHashGlobal)) {
                foundResult = candidate;
            }
            return;
        }

        char[] charset = getCharsetForType(positions[index]);
        for (char c : charset) {
            if (foundResult != null || cancelled.get()) return;
            current.append(c);
            generateFromMask(positions, current, index + 1);
            current.deleteCharAt(current.length() - 1); // backtrack
        }
    }

    public int getAttemptsCount() {
        return attemptsCount;
    }
}