package kpi.mazchenko;

import kpi.mazchenko.model.Document;
import kpi.mazchenko.model.ResemblanceScore;
import kpi.mazchenko.sequential.SequentialAnalyzer;
import kpi.mazchenko.util.DocumentGenerator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class SequentialAnalyzerTest {

    private static final int SHINGLE_SIZE = 3;
    private static final int WARMUP_RUNS = 10;
    private static final int MEASURE_RUNS = 20;
    private static final int WORDS_PER_DOC = 1000;
    private static final double GEN_OVERLAP = 0.2;
    private static final int[] BENCHMARK_SIZES = { 50, 100, 200, 300, 500, 1000 };
    private static final int PREVIEW_WORDS = 20;

    private static final SequentialAnalyzer analyzer = new SequentialAnalyzer();
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("=== Correctness Tests ===");
        testIdenticalDocuments();
        testDisjointDocuments();
        testPartialOverlapWithManualJaccard();
        testShingleSizeLargerThanDocument();
        testSingleDocumentNoPairs();
        testScoreRange();

        System.out.println("\n=== Results: " + passed + " passed, " + failed + " failed ===");
        if (failed > 0) {
            System.exit(1);
        }

        System.out.println("\n=== Real Document Test (doc1.txt vs doc2.txt) ===");
        testRealDocumentIdentity();

        System.out.println("\n=== Performance Benchmark ===");
        runBenchmark();
    }

    static void testIdenticalDocuments() {
        String text = "the cat sat on the mat near the window today morning";
        List<Document> docs = List.of(new Document(1, text), new Document(2, text));
        List<ResemblanceScore> results = analyzer.analyze(docs, SHINGLE_SIZE);
        printFragments(docs, results);
        assertEquals("identicalDocuments: pair count", 1, results.size());
        assertClose("identicalDocuments: score must be 1.0", 1.0, results.get(0).getScore());
    }

    static void testDisjointDocuments() {
        List<Document> docs = List.of(
                new Document(1, "apple banana cherry date elderberry fig grape"),
                new Document(2, "one two three four five six seven eight nine"));
        List<ResemblanceScore> results = analyzer.analyze(docs, SHINGLE_SIZE);
        printFragments(docs, results);
        assertTrue("disjointDocuments: no pairs expected", results.isEmpty());
    }

    static void testPartialOverlapWithManualJaccard() {
        List<Document> docs = List.of(
                new Document(1, "the cat sat on"),
                new Document(2, "the cat sat by"));
        List<ResemblanceScore> results = analyzer.analyze(docs, 2);
        printFragments(docs, results);
        assertEquals("partialOverlap: pair count", 1, results.size());
        assertClose("partialOverlap: Jaccard must be 0.5", 0.5, results.get(0).getScore());
    }

    static void testShingleSizeLargerThanDocument() {
        List<Document> docs = List.of(
                new Document(1, "hello world"),
                new Document(2, "hello world"));
        List<ResemblanceScore> results = analyzer.analyze(docs, 10);
        printFragments(docs, results);
        assertTrue("shingleSizeLargerThanDoc: no pairs expected", results.isEmpty());
    }

    static void testSingleDocumentNoPairs() {
        List<Document> docs = List.of(
                new Document(1, "the quick brown fox jumps over the lazy dog"));
        List<ResemblanceScore> results = analyzer.analyze(docs, SHINGLE_SIZE);
        printFragments(docs, results);
        assertTrue("singleDocument: no pairs expected", results.isEmpty());
    }

    static void testScoreRange() {
        List<Document> docs = DocumentGenerator.generate(8, 200, 0.4, 55L);
        List<ResemblanceScore> results = analyzer.analyze(docs, SHINGLE_SIZE);
        printFragments(docs, results);
        boolean allInRange = results.stream().allMatch(r -> r.getScore() > 0.0 && r.getScore() <= 1.0);
        assertTrue("scoreRange: all scores in (0.0, 1.0]", allInRange);
    }

    static void testRealDocumentIdentity() {
        String path1 = "src/main/resources/docs/doc1.txt";
        String path2 = "src/main/resources/docs/doc2.txt";

        String text1, text2;
        try {
            text1 = Files.readString(Paths.get(path1));
            text2 = Files.readString(Paths.get(path2));
        } catch (IOException e) {
            System.out.println("  SKIP: could not read files (" + e.getMessage() + ")");
            return;
        }

        Document doc1 = new Document(1, text1);
        Document doc2 = new Document(2, text2);
        List<Document> docs = List.of(doc1, doc2);

        System.out.println("  Doc 1 preview: \"" + preview(text1) + "\"");
        System.out.println("  Doc 2 preview: \"" + preview(text2) + "\"");

        List<ResemblanceScore> results = analyzer.analyze(docs, SHINGLE_SIZE);

        if (results.isEmpty()) {
            System.out.println("  Result: no pairs found (score = 0)");
            assertTrue("realDocumentIdentity: expected score 1.0, got no results", false);
        } else {
            ResemblanceScore r = results.get(0);
            System.out.printf("  Result: Doc %d <-> Doc %d | Score = %.6f (%.2f%%)%n",
                    r.getDocId1(), r.getDocId2(), r.getScore(), r.getScorePercent());
            assertClose("realDocumentIdentity: score must be 1.0", 1.0, r.getScore());
        }
    }

    private static void printFragments(List<Document> docs, List<ResemblanceScore> results) {
        System.out.println("  --- fragments ---");
        for (Document doc : docs) {
            System.out.println("  Doc " + doc.getId() + ": \"" + preview(doc.getText()) + "\"");
        }
        if (results.isEmpty()) {
            System.out.println("  Result: no matches found (score = 0)");
        } else {
            for (ResemblanceScore r : results) {
                System.out.printf("  Result: Doc %d <-> Doc %d | Score = %.6f (%.2f%%)%n",
                        r.getDocId1(), r.getDocId2(), r.getScore(), r.getScorePercent());
            }
        }
    }

    private static String preview(String text) {
        String[] words = text.trim().split("\\s+");
        int limit = Math.min(PREVIEW_WORDS, words.length);
        return String.join(" ", Arrays.copyOf(words, limit))
                + (words.length > PREVIEW_WORDS ? "..." : "");
    }

    static void runBenchmark() {
        System.out.printf("%-10s | %-12s | %-12s | %-12s%n",
                "Docs (N)", "Pairs", "Avg time (ms)", "Time/pair (us)");
        System.out.println("-".repeat(54));

        for (int n : BENCHMARK_SIZES) {
            List<Document> docs = DocumentGenerator.generate(n, WORDS_PER_DOC, GEN_OVERLAP, 42L);
            int expectedPairs = n * (n - 1) / 2;

            for (int i = 0; i < WARMUP_RUNS; i++) {
                analyzer.analyze(docs, SHINGLE_SIZE);
            }

            long start = System.nanoTime();
            for (int i = 0; i < MEASURE_RUNS; i++) {
                analyzer.analyze(docs, SHINGLE_SIZE);
            }
            long elapsed = System.nanoTime() - start;

            double avgMs = elapsed / (MEASURE_RUNS * 1_000_000.0);
            double usPerPair = expectedPairs > 0 ? (elapsed / (MEASURE_RUNS * 1_000.0)) / expectedPairs : 0;

            System.out.printf("%-10d | %-12d | %-12.2f | %-12.3f%n",
                    n, expectedPairs, avgMs, usPerPair);
        }

        System.out.println("\nNote: each entry is the average of " + MEASURE_RUNS
                + " runs after " + WARMUP_RUNS + " warmup runs.");
        System.out.println("Documents: " + WORDS_PER_DOC + " words each, overlap fraction: "
                + GEN_OVERLAP + ", shingle size: " + SHINGLE_SIZE + ".");
    }

    static void assertTrue(String label, boolean condition) {
        if (condition) {
            System.out.println("  PASS: " + label);
            passed++;
        } else {
            System.out.println("  FAIL: " + label);
            failed++;
        }
    }

    static void assertEquals(String label, int expected, int actual) {
        assertTrue(label + " (expected=" + expected + ", got=" + actual + ")",
                expected == actual);
    }

    static void assertClose(String label, double expected, double actual) {
        assertTrue(label + " (expected=" + expected + ", got=" + actual + ")",
                Math.abs(expected - actual) < 1e-9);
    }
}