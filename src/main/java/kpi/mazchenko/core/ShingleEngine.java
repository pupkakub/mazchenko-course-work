package kpi.mazchenko.core;

import java.util.HashSet;
import java.util.Set;

public class ShingleEngine {
    public static Set<String> extractShingles(String text, int w) {
        Set<String> shingles = new HashSet<>();
        String[] words = text.toLowerCase().replaceAll("[^a-zа-яіїєґ0-9\\s]", "").split("\\s+");
        if (words.length < w)
            return shingles;
        for (int i = 0; i <= words.length - w; i++) {
            StringBuilder shingle = new StringBuilder();
            for (int j = 0; j < w; j++) {
                shingle.append(words[i + j]).append(j < w - 1 ? " " : "");
            }
            shingles.add(shingle.toString());
        }
        return shingles;
    }

    public static double calculateJaccard(Set<String> setA, Set<String> setB) {
        if (setA.isEmpty() || setB.isEmpty())
            return 0.0;
        Set<String> intersection = new HashSet<>(setA);
        intersection.retainAll(setB);
        int unionSize = setA.size() + setB.size() - intersection.size();
        return unionSize == 0 ? 0.0 : (double) intersection.size() / unionSize;
    }
}