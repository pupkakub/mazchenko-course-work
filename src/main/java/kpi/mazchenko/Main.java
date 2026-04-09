package kpi.mazchenko;

import kpi.mazchenko.model.Document;
import kpi.mazchenko.model.ResemblanceScore;
import kpi.mazchenko.sequential.SequentialAnalyzer;
import kpi.mazchenko.util.DocumentGenerator;

import java.util.ArrayList;
import java.util.List;

public class Main {

    private static final int SHINGLE_SIZE = 3;
    private static final int WARMUP_RUNS = 5;
    private static final int MEASURE_RUNS = 20;

    public static void main(String[] args) {
        runSmallExample();

        System.out.println("\n=== Sequential Benchmark ===");
        System.out.printf("%-15s %-20s %-12s%n", "Documents", "Avg time (ms)", "Pairs found");
        System.out.println("-".repeat(50));

        int[] sizes = { 50, 100, 200, 300, 500 };
        for (int n : sizes) {
            runBenchmark(n);
        }
    }

    private static void runSmallExample() {
        List<Document> docs = new ArrayList<>();
        try {
            docs.add(new Document(1, readResource("/docs/doc1.txt")));
            docs.add(new Document(2, readResource("/docs/doc2.txt")));
        } catch (Exception e) {
            System.err.println("Error loading files for small example: " + e.getMessage());
            return;
        }

        SequentialAnalyzer analyzer = new SequentialAnalyzer();
        List<ResemblanceScore> results = analyzer.analyze(docs, SHINGLE_SIZE);

        System.out.println("=== Small Example (resemblance check) ===");
        for (ResemblanceScore r : results) {
            System.out.println(r);
        }
    }

    private static String readResource(String resourcePath) throws Exception {
        java.io.InputStream is = Main.class.getResourceAsStream(resourcePath);
        if (is == null) {
            throw new RuntimeException("No resource found: " + resourcePath);
        }
        return new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
    }

    private static void runBenchmark(int docCount) {
        List<Document> docs = DocumentGenerator.generate(docCount, 300, 0.4, 42L);
        SequentialAnalyzer analyzer = new SequentialAnalyzer();

        for (int i = 0; i < WARMUP_RUNS; i++) {
            analyzer.analyze(docs, SHINGLE_SIZE);
        }

        long totalNanos = 0;
        int pairsFound = 0;
        for (int i = 0; i < MEASURE_RUNS; i++) {
            long start = System.nanoTime();
            List<ResemblanceScore> results = analyzer.analyze(docs, SHINGLE_SIZE);
            totalNanos += System.nanoTime() - start;
            pairsFound = results.size();
        }

        double avgMs = (totalNanos / (double) MEASURE_RUNS) / 1_000_000.0;
        System.out.printf("%-15d %-20.2f %-12d%n", docCount, avgMs, pairsFound);
    }
}