package kpi.mazchenko.util;

import kpi.mazchenko.model.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generates synthetic document collections for benchmarking and testing.
 *
 * Strategy: each document is built from a shared word pool so that
 * neighbouring documents share a configurable fraction of their shingles,
 * producing realistic resemblance scores rather than all-zero results.
 */
public class DocumentGenerator {

    private static final String[] WORD_POOL = {
            "the", "a", "is", "in", "it", "of", "and", "to", "was", "for",
            "on", "are", "as", "with", "his", "they", "be", "at", "one", "have",
            "this", "from", "or", "had", "by", "not", "but", "what", "all", "were",
            "we", "when", "your", "can", "said", "there", "use", "an", "each",
            "which", "she", "do", "how", "their", "if", "will", "up", "other",
            "about", "out", "many", "then", "them", "these", "so", "some", "her",
            "would", "make", "like", "him", "into", "time", "has", "look", "two",
            "more", "write", "go", "see", "number", "no", "way", "could", "people",
            "my", "than", "first", "water", "been", "call", "who", "oil", "its",
            "now", "find", "long", "down", "day", "did", "get", "come", "made",
            "may", "part", "over", "new", "sound", "take", "only", "little", "work",
            "know", "place", "year", "live", "me", "back", "give", "most", "very",
            "after", "thing", "our", "just", "name", "good", "sentence", "man",
            "think", "say", "great", "where", "help", "through", "much", "before",
            "line", "right", "too", "mean", "old", "any", "same", "tell", "boy",
            "follow", "came", "want", "show", "also", "around", "form", "three",
            "small", "set", "put", "end", "does", "another", "well", "large",
            "need", "big", "high", "such", "turn", "here", "why", "ask", "went",
            "men", "read", "land", "different", "home", "us", "move", "try", "kind",
            "hand", "picture", "again", "change", "off", "play", "spell", "air",
            "away", "animal", "house", "point", "page", "letter", "mother", "answer",
            "found", "study", "still", "learn", "plant", "cover", "food", "sun",
            "four", "between", "state", "keep", "eye", "never", "last", "let",
            "thought", "city", "tree", "cross", "farm", "hard", "start", "might",
            "story", "saw", "far", "sea", "draw", "left", "late", "run", "while",
            "press", "close", "night", "real", "life", "few", "north", "open",
            "seem", "together", "next", "white", "children", "begin", "got", "walk",
            "example", "ease", "paper", "group", "always", "music", "those", "both",
            "mark", "book", "carry", "took", "science", "eat", "room", "friend",
            "began", "idea", "fish", "mountain", "stop", "once", "base", "hear",
            "horse", "cut", "sure", "watch", "color", "face", "wood", "main",
            "enough", "plain", "girl", "usual", "young", "ready", "above", "ever",
            "red", "list", "though", "feel", "talk", "bird", "soon", "body", "dog",
            "family", "direct", "pose", "leave", "song", "measure", "door", "product",
            "black", "short", "numeral", "class", "wind", "question", "happen",
            "complete", "ship", "area", "half", "rock", "order", "fire", "south",
            "problem", "piece", "told", "knew", "pass", "since", "top", "whole",
            "king", "space", "heard", "best", "hour", "better", "during", "hundred"
    };

    /**
     * Generates a list of documents with controlled overlap.
     *
     * @param count           number of documents to generate
     * @param wordsPerDoc     approximate number of words in each document
     * @param overlapFraction fraction of words taken from the previous
     *                        document's content (0.0 = all unique,
     *                        0.5 = 50% shared with neighbour)
     * @param seed            random seed for reproducibility
     */
    public static List<Document> generate(int count, int wordsPerDoc,
            double overlapFraction, long seed) {
        Random random = new Random(seed);
        List<Document> docs = new ArrayList<>(count);
        String[] previousWords = null;

        for (int i = 0; i < count; i++) {
            String[] words = buildWordArray(wordsPerDoc, previousWords,
                    overlapFraction, random);
            docs.add(new Document(i + 1, String.join(" ", words)));
            previousWords = words;
        }
        return docs;
    }

    /** Generates documents with no guaranteed overlap (fully random). */
    public static List<Document> generate(int count, int wordsPerDoc, long seed) {
        return generate(count, wordsPerDoc, 0.0, seed);
    }

    private static String[] buildWordArray(int length, String[] previous,
            double overlapFraction, Random rng) {
        String[] result = new String[length];
        int overlapCount = (previous == null) ? 0
                : (int) (length * overlapFraction);

        // Copy a random contiguous segment from the previous document
        if (overlapCount > 0 && previous.length >= overlapCount) {
            int startSrc = rng.nextInt(previous.length - overlapCount + 1);
            int startDst = rng.nextInt(length - overlapCount + 1);
            System.arraycopy(previous, startSrc, result, startDst, overlapCount);
        }

        // Fill remaining positions with random words from pool
        for (int i = 0; i < length; i++) {
            if (result[i] == null) {
                result[i] = WORD_POOL[rng.nextInt(WORD_POOL.length)];
            }
        }
        return result;
    }
}