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
                if (j > 0)
                    shingle.append(' ');
                shingle.append(words[i + j]);
            }
            shingles.add(shingle.toString());
        }
        return shingles;
    }

    public static double calculateJaccard(Set<String> setA, Set<String> setB) {
        if (setA.isEmpty() || setB.isEmpty())
            return 0.0;
        Set<String> smaller = setA.size() <= setB.size() ? setA : setB;
        Set<String> larger = setA.size() <= setB.size() ? setB : setA;
        int intersectionSize = 0;
        for (String s : smaller) {
            if (larger.contains(s))
                intersectionSize++;
        }
        int unionSize = setA.size() + setB.size() - intersectionSize;
        return unionSize == 0 ? 0.0 : (double) intersectionSize / unionSize;
    }
}