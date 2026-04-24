package kpi.mazchenko;

import kpi.mazchenko.model.Document;
import kpi.mazchenko.model.ResemblanceScore;
import kpi.mazchenko.parallel.ForkJoinAnalyzer;
import kpi.mazchenko.sequential.SequentialAnalyzer;
import kpi.mazchenko.util.DocumentGenerator;

import java.util.Comparator;
import java.util.List;

public class ForkJoinAnalyzerTest {

    private static final int SHINGLE_SIZE = 3;
    private static final int FORK_THRESHOLD = 50;
    private static final int WORDS_PER_DOC = 1000;
    private static final double GEN_OVERLAP = 0.3;

    private static final int STRESS_N = 800;
    private static final int STRESS_THREADS = Runtime.getRuntime().availableProcessors();

    private static final SequentialAnalyzer seq = new SequentialAnalyzer();
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("Available processors: " + STRESS_THREADS);
        System.out.println();

        System.out.println("=== Correctness Tests (small N, debug-level) ===");
        testIdenticalPairDetected();
        testDisjointProducesNoResults();
        testManualJaccardEqualsSequential();
        testResultCountMatchesSequential();
        testScoresMatchSequentialExactly();
        testShingleSizeLargerThanDoc();
        testSingleDocumentNoPairs();

        System.out.println();
        System.out.println("=== Stress Correctness Tests (N=" + STRESS_N + ") ===");
        testStressResultCountMatchesSequential();
        testStressAllScoresMatchSequential();
        testStressRepeatedRunsProduceSameResult();
        testStressMultipleThreadCountsMatchSequential();
        testStressHighOverlapMatchesSequential();
        testStressZeroOverlapMatchesSequential();

        System.out.println();
        System.out.println("=== Results: " + passed + " passed, " + failed + " failed ===");
        if (failed > 0) {
            System.exit(1);
        }

        System.out.println();
        System.out.println("=== Algorithmic Validation ===");
        runValidationTest();
    }

    static void testIdenticalPairDetected() {
        String text = "the cat sat on the mat near the window today";
        List<Document> docs = List.of(new Document(1, text), new Document(2, text));
        ForkJoinAnalyzer par = new ForkJoinAnalyzer(STRESS_THREADS);
        List<ResemblanceScore> results = par.analyze(docs, SHINGLE_SIZE, FORK_THRESHOLD);
        par.shutdown();
        assertEquals("identicalPairDetected: size", 1, results.size());
        assertClose("identicalPairDetected: score", 1.0, results.get(0).getScore());
    }

    static void testDisjointProducesNoResults() {
        List<Document> docs = List.of(
                new Document(1, "apple banana cherry date elderberry fig grape"),
                new Document(2, "one two three four five six seven eight nine"));
        ForkJoinAnalyzer par = new ForkJoinAnalyzer(STRESS_THREADS);
        List<ResemblanceScore> results = par.analyze(docs, SHINGLE_SIZE, FORK_THRESHOLD);
        par.shutdown();
        assertTrue("disjointProducesNoResults", results.isEmpty());
    }

    static void testManualJaccardEqualsSequential() {
        List<Document> docs = List.of(
                new Document(1, "the cat sat on"),
                new Document(2, "the cat sat by"));
        ForkJoinAnalyzer par = new ForkJoinAnalyzer(STRESS_THREADS);
        List<ResemblanceScore> parResults = par.analyze(docs, 2, FORK_THRESHOLD);
        par.shutdown();
        List<ResemblanceScore> seqResults = seq.analyze(docs, 2);
        assertEquals("manualJaccard: size", 1, parResults.size());
        assertClose("manualJaccard: score matches sequential",
                seqResults.get(0).getScore(), parResults.get(0).getScore());
    }

    static void testResultCountMatchesSequential() {
        List<Document> docs = DocumentGenerator.generate(20, 200, 0.4, 1L);
        ForkJoinAnalyzer par = new ForkJoinAnalyzer(STRESS_THREADS);
        List<ResemblanceScore> parResults = par.analyze(docs, SHINGLE_SIZE, FORK_THRESHOLD);
        par.shutdown();
        List<ResemblanceScore> seqResults = seq.analyze(docs, SHINGLE_SIZE);
        assertEquals("resultCountMatchesSequential (N=20)",
                seqResults.size(), parResults.size());
    }

    static void testScoresMatchSequentialExactly() {
        List<Document> docs = DocumentGenerator.generate(30, 300, 0.35, 7L);
        ForkJoinAnalyzer par = new ForkJoinAnalyzer(STRESS_THREADS);
        List<ResemblanceScore> parResults = par.analyze(docs, SHINGLE_SIZE, FORK_THRESHOLD);
        par.shutdown();
        List<ResemblanceScore> seqResults = seq.analyze(docs, SHINGLE_SIZE);
        assertTrue("scoresMatchSequentialExactly (N=30): same result",
                resultsEqual(seqResults, parResults));
    }

    static void testShingleSizeLargerThanDoc() {
        List<Document> docs = List.of(
                new Document(1, "hello world"),
                new Document(2, "hello world"));
        ForkJoinAnalyzer par = new ForkJoinAnalyzer(STRESS_THREADS);
        List<ResemblanceScore> results = par.analyze(docs, 10, FORK_THRESHOLD);
        par.shutdown();
        assertTrue("shingleSizeLargerThanDoc: no pairs", results.isEmpty());
    }

    static void testSingleDocumentNoPairs() {
        List<Document> docs = List.of(
                new Document(1, "the quick brown fox jumps over the lazy dog"));
        ForkJoinAnalyzer par = new ForkJoinAnalyzer(STRESS_THREADS);
        List<ResemblanceScore> results = par.analyze(docs, SHINGLE_SIZE, FORK_THRESHOLD);
        par.shutdown();
        assertTrue("singleDocumentNoPairs", results.isEmpty());
    }

    static void testStressResultCountMatchesSequential() {
        List<Document> docs = DocumentGenerator.generate(STRESS_N, WORDS_PER_DOC, GEN_OVERLAP, 42L);
        ForkJoinAnalyzer par = new ForkJoinAnalyzer(STRESS_THREADS);
        List<ResemblanceScore> parResults = par.analyze(docs, SHINGLE_SIZE, FORK_THRESHOLD);
        par.shutdown();
        List<ResemblanceScore> seqResults = seq.analyze(docs, SHINGLE_SIZE);
        assertEquals("stress resultCountMatchesSequential (N=" + STRESS_N + ")",
                seqResults.size(), parResults.size());
    }

    static void testStressAllScoresMatchSequential() {
        List<Document> docs = DocumentGenerator.generate(STRESS_N, WORDS_PER_DOC, GEN_OVERLAP, 42L);
        ForkJoinAnalyzer par = new ForkJoinAnalyzer(STRESS_THREADS);
        List<ResemblanceScore> parResults = par.analyze(docs, SHINGLE_SIZE, FORK_THRESHOLD);
        par.shutdown();
        List<ResemblanceScore> seqResults = seq.analyze(docs, SHINGLE_SIZE);
        assertTrue("stress allScoresMatchSequential (N=" + STRESS_N + ")",
                resultsEqual(seqResults, parResults));
    }

    static void testStressRepeatedRunsProduceSameResult() {
        List<Document> docs = DocumentGenerator.generate(STRESS_N, WORDS_PER_DOC, GEN_OVERLAP, 13L);
        ForkJoinAnalyzer par = new ForkJoinAnalyzer(STRESS_THREADS);
        List<ResemblanceScore> first = par.analyze(docs, SHINGLE_SIZE, FORK_THRESHOLD);
        List<ResemblanceScore> second = par.analyze(docs, SHINGLE_SIZE, FORK_THRESHOLD);
        List<ResemblanceScore> third = par.analyze(docs, SHINGLE_SIZE, FORK_THRESHOLD);
        par.shutdown();
        boolean ok = resultsEqual(first, second) && resultsEqual(second, third);
        assertTrue("stress repeatedRunsProduceSameResult (N=" + STRESS_N + ")", ok);
    }

    static void testStressMultipleThreadCountsMatchSequential() {
        List<Document> docs = DocumentGenerator.generate(STRESS_N, WORDS_PER_DOC, GEN_OVERLAP, 77L);
        List<ResemblanceScore> seqResults = seq.analyze(docs, SHINGLE_SIZE);
        boolean allMatch = true;
        int[] threadCounts = buildThreadCounts(STRESS_THREADS);
        for (int threads : threadCounts) {
            ForkJoinAnalyzer par = new ForkJoinAnalyzer(threads);
            List<ResemblanceScore> parResults = par.analyze(docs, SHINGLE_SIZE, FORK_THRESHOLD);
            par.shutdown();
            if (!resultsEqual(seqResults, parResults)) {
                allMatch = false;
                System.out.println("  [DETAIL] Mismatch at thread count: " + threads);
            }
        }
        assertTrue("stress multipleThreadCountsMatchSequential (N=" + STRESS_N + ")", allMatch);
    }

    static void testStressHighOverlapMatchesSequential() {
        List<Document> docs = DocumentGenerator.generate(STRESS_N, WORDS_PER_DOC, 0.8, 55L);
        ForkJoinAnalyzer par = new ForkJoinAnalyzer(STRESS_THREADS);
        List<ResemblanceScore> parResults = par.analyze(docs, SHINGLE_SIZE, FORK_THRESHOLD);
        par.shutdown();
        List<ResemblanceScore> seqResults = seq.analyze(docs, SHINGLE_SIZE);
        assertTrue("stress highOverlapMatchesSequential (N=" + STRESS_N + ", overlap=0.8)",
                resultsEqual(seqResults, parResults));
    }

    static void testStressZeroOverlapMatchesSequential() {
        List<Document> docs = DocumentGenerator.generate(STRESS_N, WORDS_PER_DOC, 0.0, 99L);
        ForkJoinAnalyzer par = new ForkJoinAnalyzer(STRESS_THREADS);
        List<ResemblanceScore> parResults = par.analyze(docs, SHINGLE_SIZE, FORK_THRESHOLD);
        par.shutdown();
        List<ResemblanceScore> seqResults = seq.analyze(docs, SHINGLE_SIZE);
        assertTrue("stress zeroOverlapMatchesSequential (N=" + STRESS_N + ", overlap=0.0)",
                resultsEqual(seqResults, parResults));
    }

    static void runValidationTest() {
        int[] sizes = { 100, 200, 500, 800, 1000 };
        System.out.printf("%-10s | %-12s | %-12s | %-15s%n",
                "Docs (N)", "Seq Pairs", "Par Pairs", "Scores Match");
        System.out.println("-".repeat(58));

        for (int n : sizes) {
            List<Document> docs = DocumentGenerator.generate(n, WORDS_PER_DOC, GEN_OVERLAP, 42L);

            List<ResemblanceScore> seqResults = seq.analyze(docs, SHINGLE_SIZE);

            ForkJoinAnalyzer par = new ForkJoinAnalyzer(STRESS_THREADS);
            List<ResemblanceScore> parResults = par.analyze(docs, SHINGLE_SIZE, FORK_THRESHOLD);
            par.shutdown();

            int seqSize = seqResults.size();
            int parSize = parResults.size();
            boolean isMatch = resultsEqual(seqResults, parResults);

            System.out.printf("%-10d | %-12d | %-12d | %-15s%n",
                    n, seqSize, parSize, isMatch ? "YES" : "NO");
        }

        System.out.println();
        System.out.println("Documents: " + WORDS_PER_DOC + " words each, overlap fraction: "
                + GEN_OVERLAP + ", shingle size: " + SHINGLE_SIZE
                + ", threads: " + STRESS_THREADS + ", fork threshold: " + FORK_THRESHOLD + ".");
    }

    private static boolean resultsEqual(List<ResemblanceScore> a, List<ResemblanceScore> b) {
        if (a.size() != b.size())
            return false;
        Comparator<ResemblanceScore> comp = Comparator
                .comparingInt(ResemblanceScore::getDocId1)
                .thenComparingInt(ResemblanceScore::getDocId2);
        List<ResemblanceScore> sortedA = a.stream().sorted(comp).toList();
        List<ResemblanceScore> sortedB = b.stream().sorted(comp).toList();
        for (int i = 0; i < sortedA.size(); i++) {
            ResemblanceScore sa = sortedA.get(i);
            ResemblanceScore sb = sortedB.get(i);
            if (sa.getDocId1() != sb.getDocId1() || sa.getDocId2() != sb.getDocId2())
                return false;
            if (Math.abs(sa.getScore() - sb.getScore()) > 1e-9)
                return false;
        }
        return true;
    }

    private static int[] buildThreadCounts(int max) {
        if (max <= 4) {
            int[] result = new int[max];
            for (int i = 0; i < max; i++)
                result[i] = i + 1;
            return result;
        }
        return new int[] { 1, 2, max / 2, max };
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
        assertTrue(label + " (expected=" + expected + ", got=" + actual + ")", expected == actual);
    }

    static void assertClose(String label, double expected, double actual) {
        assertTrue(label + " (expected=" + expected + ", got=" + actual + ")",
                Math.abs(expected - actual) < 1e-9);
    }
}