package kpi.mazchenko;

import kpi.mazchenko.model.Document;
import kpi.mazchenko.model.ResemblanceScore;
import kpi.mazchenko.sequential.SequentialAnalyzer;
import kpi.mazchenko.util.DocumentGenerator;

import java.util.List;

public class SequentialAnalyzerTest {

    private static final int SHINGLE_SIZE = 3;
    private static final int WARMUP_RUNS = 10;
    private static final int MEASURE_RUNS = 20;
    private static final int WORDS_PER_DOC = 1000;
    private static final double GEN_OVERLAP = 0.2;
    private static final int[] BENCHMARK_SIZES = { 50, 100, 200, 300, 500, 1000 };

    private static final SequentialAnalyzer analyzer = new SequentialAnalyzer();
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("=== Correctness Tests ===");
        testIdenticalDocumentsScoreOne();
        testDisjointDocumentsScoreZero();
        testPartialOverlapIntermediateScore();
        testManualJaccardVerification();
        testShingleSizeLargerThanDocumentLength();
        testSingleDocumentNoPairs();
        testPairCountBound();
        testDuplicateBookDetection();
        testScoreSymmetryAndRange();
        testShingleSizeOne();

        System.out.println("\n=== Results: " + passed + " passed, " + failed + " failed ===");
        if (failed > 0) {
            System.exit(1);
        }

        System.out.println("\n=== Performance Benchmark ===");
        runBenchmark();
    }

    static void testIdenticalDocumentsScoreOne() {
        String text = "the cat sat on the mat near the window today";
        List<Document> docs = List.of(new Document(1, text), new Document(2, text));
        List<ResemblanceScore> results = analyzer.analyze(docs, 3);
        assertEquals("identicalDocumentsScoreOne: size", 1, results.size());
        assertClose("identicalDocumentsScoreOne: score", 1.0, results.get(0).getScore());
    }

    static void testDisjointDocumentsScoreZero() {
        List<Document> docs = List.of(
                new Document(1, "apple banana cherry date elderberry fig grape"),
                new Document(2, "one two three four five six seven eight nine"));
        List<ResemblanceScore> results = analyzer.analyze(docs, 3);
        assertTrue("disjointDocumentsScoreZero: should be empty", results.isEmpty());
    }

    static void testPartialOverlapIntermediateScore() {
        List<Document> docs = List.of(
                new Document(1, "the cat sat on the mat near the window"),
                new Document(2, "the cat sat on the mat by the door"));
        List<ResemblanceScore> results = analyzer.analyze(docs, 3);
        assertEquals("partialOverlap: size", 1, results.size());
        double score = results.get(0).getScore();
        assertTrue("partialOverlap: score in (0,1) but was " + score,
                score > 0.0 && score < 1.0);
    }

    static void testManualJaccardVerification() {
        List<Document> docs = List.of(
                new Document(1, "the cat sat on"),
                new Document(2, "the cat sat by"));
        List<ResemblanceScore> results = analyzer.analyze(docs, 2);
        assertEquals("manualJaccard: size", 1, results.size());
        assertClose("manualJaccard: score should be 0.5", 0.5, results.get(0).getScore());
    }

    static void testShingleSizeLargerThanDocumentLength() {
        List<Document> docs = List.of(
                new Document(1, "hello world"),
                new Document(2, "hello world"));
        List<ResemblanceScore> results = analyzer.analyze(docs, 10);
        assertTrue("shingleSizeLargerThanDoc: should produce no pairs", results.isEmpty());
    }

    static void testSingleDocumentNoPairs() {
        List<Document> docs = List.of(
                new Document(1, "the quick brown fox jumps over the lazy dog"));
        List<ResemblanceScore> results = analyzer.analyze(docs, 3);
        assertTrue("singleDocument: should produce no pairs", results.isEmpty());
    }

    static void testPairCountBound() {
        int n = 10;
        List<Document> docs = DocumentGenerator.generate(n, 200, 0.7, 1L);
        List<ResemblanceScore> results = analyzer.analyze(docs, 3);
        int maxPairs = n * (n - 1) / 2;
        assertTrue("pairCountBound: " + results.size() + " <= " + maxPairs,
                results.size() <= maxPairs);
    }

    static void testDuplicateBookDetection() {
        List<Document> bookBase = DocumentGenerator.generate(1, 1000, 0.0, 99L);
        String bookText = bookBase.get(0).getText();
        List<Document> docs = List.of(
                new Document(1, bookText),
                new Document(2, bookText),
                new Document(3, DocumentGenerator.generate(1, 1000, 0.0, 777L).get(0).getText()));
        List<ResemblanceScore> results = analyzer.analyze(docs, 3);
        double score12 = results.stream()
                .filter(r -> r.getDocId1() == 1 && r.getDocId2() == 2)
                .mapToDouble(ResemblanceScore::getScore)
                .findFirst()
                .orElse(-1.0);
        assertClose("duplicateBook: docs 1&2 should be 1.0, got " + score12, 1.0, score12);
        System.out.println("  [INFO] Duplicate book score: " + score12
                + " | Total pairs with score>0: " + results.size());
    }

    static void testScoreSymmetryAndRange() {
        List<Document> docs = DocumentGenerator.generate(8, 200, 0.4, 55L);
        List<ResemblanceScore> results = analyzer.analyze(docs, 3);
        boolean allInRange = results.stream().allMatch(r -> r.getScore() > 0.0 && r.getScore() <= 1.0);
        assertTrue("scoreRange: all scores in (0, 1]", allInRange);
        boolean noDuplicatePairs = true;
        for (int i = 0; i < results.size(); i++) {
            for (int j = i + 1; j < results.size(); j++) {
                ResemblanceScore a = results.get(i);
                ResemblanceScore b = results.get(j);
                if (a.getDocId1() == b.getDocId1() && a.getDocId2() == b.getDocId2()) {
                    noDuplicatePairs = false;
                    break;
                }
            }
        }
        assertTrue("noDuplicatePairs: each pair appears at most once", noDuplicatePairs);
    }

    static void testShingleSizeOne() {
        List<Document> docs = List.of(
                new Document(1, "alpha beta gamma"),
                new Document(2, "alpha delta gamma"));
        List<ResemblanceScore> results = analyzer.analyze(docs, 1);
        assertEquals("shingleSizeOne: size", 1, results.size());
        double expected = 2.0 / 4.0;
        assertClose("shingleSizeOne: score should be 0.5", expected, results.get(0).getScore());
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
        System.out.println("Documents: " + WORDS_PER_DOC + " words each, overlap fraction: " + GEN_OVERLAP
                + ", shingle size: " + SHINGLE_SIZE + ".");
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