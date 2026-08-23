package com.khushi.passwordcracker.algorithms;
import com.khushi.passwordcracker.utils.HashUtil;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class MarkovChainCracker {

    private int attemptsCount = 0;
    // model: currentChar -> (nextChar -> probability)
    private Map<Character, Map<Character, Double>> model = new HashMap<>();
    private Map<Character, Double> startProbabilities = new HashMap<>();

    // Training data se model banate hain
    public void train(List<String> trainingPasswords) {
        Map<Character, Map<Character, Integer>> transitionCounts = new HashMap<>();
        Map<Character, Integer> startCounts = new HashMap<>();

        for (String pwd : trainingPasswords) {
            if (pwd.isEmpty()) continue;
            char first = pwd.charAt(0);
            startCounts.merge(first, 1, Integer::sum);

            for (int i = 0; i < pwd.length() - 1; i++) {
                char current = pwd.charAt(i);
                char next = pwd.charAt(i + 1);
                transitionCounts.putIfAbsent(current, new HashMap<>());
                transitionCounts.get(current).merge(next, 1, Integer::sum);
            }
        }

        // Counts ko probabilities mein convert karo
        int totalStarts = startCounts.values().stream().mapToInt(Integer::intValue).sum();
        for (var entry : startCounts.entrySet()) {
            startProbabilities.put(entry.getKey(), (double) entry.getValue() / totalStarts);
        }

        for (var entry : transitionCounts.entrySet()) {
            char current = entry.getKey();
            int total = entry.getValue().values().stream().mapToInt(Integer::intValue).sum();
            Map<Character, Double> probs = new HashMap<>();
            for (var next : entry.getValue().entrySet()) {
                probs.put(next.getKey(), (double) next.getValue() / total);
            }
            model.put(current, probs);
        }
    }

    // Candidate class: string + uski cumulative probability
    private static class Candidate {
        String value;
        double probability;
        Candidate(String v, double p) { value = v; probability = p; }
    }

    public String crack(String targetHash, int maxLength) throws Exception {
        return crack(targetHash, maxLength, new AtomicBoolean(false));
    }

    // Cancellable overload - existing signature above still works unchanged.
    public String crack(String targetHash, int maxLength, AtomicBoolean cancelled) throws Exception {
        attemptsCount = 0;
        PriorityQueue<Candidate> pq = new PriorityQueue<>(
                (a, b) -> Double.compare(b.probability, a.probability) // max-heap
        );

        for (var entry : startProbabilities.entrySet()) {
            pq.add(new Candidate(String.valueOf(entry.getKey()), entry.getValue()));
        }

        while (!pq.isEmpty()) {

            if (cancelled.get()) {
                return "Not Found";
            }

            Candidate current = pq.poll();
            attemptsCount++;

            String candidateHash = HashUtil.generateSHA256(current.value);
            if (candidateHash.equals(targetHash)) {
                return current.value;
            }

            if (current.value.length() < maxLength) {
                char lastChar = current.value.charAt(current.value.length() - 1);
                Map<Character, Double> nextOptions = model.get(lastChar);
                if (nextOptions != null) {
                    for (var next : nextOptions.entrySet()) {
                        String newCandidate = current.value + next.getKey();
                        double newProb = current.probability * next.getValue();
                        pq.add(new Candidate(newCandidate, newProb));
                    }
                }
            }
        }

        return "Not Found";
    }

    public int getAttemptsCount() {
        return attemptsCount;
    }
}
