package kpi.mazchenko.util;

import kpi.mazchenko.model.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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

    private static final int MAX_OVERLAP_SOURCES = 3;

    public static List<Document> generate(int count, int wordsPerDoc, double overlapFraction, long seed) {
        Random random = new Random(seed);
        List<String[]> generatedWords = new ArrayList<>(count);
        List<Document> docs = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            String[] words = buildWordArray(wordsPerDoc, generatedWords, overlapFraction, random);
            generatedWords.add(words);
            docs.add(new Document(i + 1, String.join(" ", words)));
        }
        return docs;
    }

    private static String[] buildWordArray(int length, List<String[]> previous, double overlapFraction, Random rng) {
        String[] result = new String[length];

        if (!previous.isEmpty() && overlapFraction > 0) {
            int sourcesCount = Math.min(previous.size(), MAX_OVERLAP_SOURCES);
            int overlapPerSource = (int) (length * overlapFraction / sourcesCount);

            for (int s = 0; s < sourcesCount; s++) {
                if (overlapPerSource == 0)
                    break;
                String[] source = previous.get(previous.size() - 1 - rng.nextInt(previous.size()));
                if (source.length < overlapPerSource)
                    continue;

                int startSrc = rng.nextInt(source.length - overlapPerSource + 1);
                int startDst;
                int attempts = 0;
                do {
                    startDst = rng.nextInt(length - overlapPerSource + 1);
                    attempts++;
                } while (hasOverlap(result, startDst, overlapPerSource) && attempts < 10);

                if (!hasOverlap(result, startDst, overlapPerSource)) {
                    System.arraycopy(source, startSrc, result, startDst, overlapPerSource);
                }
            }
        }

        for (int i = 0; i < length; i++) {
            if (result[i] == null) {
                result[i] = WORD_POOL[rng.nextInt(WORD_POOL.length)];
            }
        }
        return result;
    }

    private static boolean hasOverlap(String[] result, int start, int length) {
        for (int i = start; i < start + length; i++) {
            if (result[i] != null)
                return true;
        }
        return false;
    }
}