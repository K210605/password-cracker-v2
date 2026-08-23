package com.khushi.passwordcracker;

import com.khushi.passwordcracker.algorithms.BruteForceCracker;
import com.khushi.passwordcracker.algorithms.DictionaryAttackCracker;
import com.khushi.passwordcracker.algorithms.HybridAttackCracker;
import com.khushi.passwordcracker.algorithms.MaskAttackCracker;
import com.khushi.passwordcracker.algorithms.MarkovChainCracker;

import java.util.Arrays;
import java.util.List;
import com.khushi.passwordcracker.utils.HashUtil;

public class Main {

    public static void main(String[] args) throws Exception {

        // Original Password
        String password = "cat";

        // Generate SHA-256 Hash
        String hash = HashUtil.generateSHA256(password);

        System.out.println("Target Hash:");
        System.out.println(hash);

        // ==========================
        // BRUTE FORCE ATTACK
        // ==========================

        System.out.println("\nBrute Force Attack...");

        long bruteStart = System.nanoTime();

        String bruteResult = BruteForceCracker.crackPassword(hash, 3);

        long bruteEnd = System.nanoTime();

        if (bruteResult != null) {
            System.out.println("Password Found: " + bruteResult);
        } else {
            System.out.println("Password Not Found");
        }

        System.out.println("Brute Force Time: "
                + ((bruteEnd - bruteStart) / 1_000_000.0)
                + " ms");

        // ==========================
        // DICTIONARY ATTACK
        // ==========================

        System.out.println("\nDictionary Attack...");

        long dictionaryStart = System.nanoTime();

        String dictionaryResult = DictionaryAttackCracker.crackPassword(hash);

        long dictionaryEnd = System.nanoTime();

        if (dictionaryResult != null) {
            System.out.println("Password Found: " + dictionaryResult);
        } else {
            System.out.println("Password Not Found");
        }

        System.out.println("Dictionary Attack Time: "
                + ((dictionaryEnd - dictionaryStart) / 1_000_000.0)
                + " ms");

        // ==========================
        // HYBRID ATTACK
        // ==========================

        System.out.println("\nHybrid Attack...");

        HybridAttackCracker hybrid = new HybridAttackCracker();

        long hybridStart = System.nanoTime();

        String hybridResult = hybrid.crack(hash);


        long hybridEnd = System.nanoTime();

        if (!hybridResult.equals("Not Found")) {
            System.out.println("Password Found: " + hybridResult);
        } else {
            System.out.println("Password Not Found");
        }

        System.out.println("Attempts: " + hybrid.getAttemptsCount());

        System.out.println("Hybrid Attack Time: "
                + ((hybridEnd - hybridStart) / 1_000_000.0)
                + " ms");

// ==========================
// MASK ATTACK
// ==========================

        System.out.println("\nMask Attack...");

        MaskAttackCracker mask = new MaskAttackCracker();

        String maskPattern = "?l?l?l";   // "cat" ke liye

        long maskStart = System.nanoTime();

        String maskResult = mask.crack(hash, maskPattern);

        long maskEnd = System.nanoTime();

        if (!maskResult.equals("Not Found")) {
            System.out.println("Password Found: " + maskResult);
        } else {
            System.out.println("Password Not Found");
        }

        System.out.println("Attempts: " + mask.getAttemptsCount());

        System.out.println("Mask Attack Time: "
                + ((maskEnd - maskStart) / 1_000_000.0)
                + " ms");

        // =========================
// MARKOV CHAIN ATTACK
// =========================

        System.out.println("\nMarkov Chain Attack...");

        MarkovChainCracker markov = new MarkovChainCracker();

// Training passwords
        List<String> trainingPasswords = Arrays.asList(
                "password",
                "admin",
                "welcome",
                "hello",
                "abc123",
                "admin123",
                "password123",
                "cat",
                "dog",
                "test123"
        );

// Train the model
        markov.train(trainingPasswords);

        long markovStart = System.nanoTime();

        String markovResult = markov.crack(hash, 6);

        long markovEnd = System.nanoTime();

        if (!markovResult.equals("Not Found")) {
            System.out.println("Password Found: " + markovResult);
        } else {
            System.out.println("Password Not Found");
        }

        System.out.println("Attempts: " + markov.getAttemptsCount());

        System.out.println("Markov Attack Time: "
                + ((markovEnd - markovStart) / 1_000_000.0)
                + " ms");
    }


}
