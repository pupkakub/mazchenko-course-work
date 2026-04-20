package kpi.mazchenko;

import kpi.mazchenko.model.Document;
import kpi.mazchenko.model.ResemblanceScore;
import kpi.mazchenko.parallel.ForkJoinAnalyzer;
import kpi.mazchenko.sequential.SequentialAnalyzer;
import kpi.mazchenko.util.DocumentGenerator;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class Main {
    private static final int SHINGLE_SIZE = 3;
    private static final int FORK_THRESHOLD = 10;
    private static final int WARMUP_RUNS = 20;
    private static final int MEASURE_RUNS = 20;

    private static final int DEMO_DOC_COUNT = 10;
    private static final int DEMO_WORDS_PER_DOC = 80;
    private static final double DEMO_OVERLAP = 0.5;

    public static void main(String[] args) {
        int parallelism = Runtime.getRuntime().availableProcessors();
        System.out.println("Logical processors: " + parallelism);

        ForkJoinAnalyzer par = new ForkJoinAnalyzer(parallelism);

        runDemo(par);
        runBenchmark(par);

        par.shutdown();
    }

    private static void runDemo(ForkJoinAnalyzer par) {
        System.out.println("\n=== DEMO: resemblance scores ===");
        List<Document> docs = DocumentGenerator.generate(DEMO_DOC_COUNT, DEMO_WORDS_PER_DOC, DEMO_OVERLAP, 7L);
        SequentialAnalyzer seq = new SequentialAnalyzer();

        List<ResemblanceScore> results = seq.analyze(docs, SHINGLE_SIZE);
        results.sort(Comparator.comparingDouble(ResemblanceScore::getScore).reversed());

        if (results.isEmpty()) {
            System.out.println("No matching pairs found.");
        } else {
            for (ResemblanceScore r : results) {
                System.out.println(r);
            }
        }

        List<ResemblanceScore> parResults = par.analyze(docs, SHINGLE_SIZE, FORK_THRESHOLD);
        System.out.println("Total matching pairs found: " + results.size());
        System.out.println("Match valid: " + validate(results, parResults));
    }

    private static void runBenchmark(ForkJoinAnalyzer par) {
        System.out.println("\n=== BENCHMARK ===");

        warmupJvm(par);

        int[] sizes = { 50, 100, 150, 200 }; // 100, 500, 1000, 2000, 3500, 5000

        System.out.printf("%-10s %-15s %-15s %-12s %-10s%n",
                "Docs", "T_seq (ms)", "T_par (ms)", "Speedup", "Valid");
        System.out.println("-".repeat(65));

        for (int n : sizes) {
            runComparison(n, par);
        }
    }

    private static void warmupJvm(ForkJoinAnalyzer par) {
        List<Document> warmupDocs = DocumentGenerator.generate(200, 300, 0.3, 0L);
        SequentialAnalyzer seq = new SequentialAnalyzer();
        for (int i = 0; i < WARMUP_RUNS; i++) {
            seq.analyze(warmupDocs, SHINGLE_SIZE);
            par.analyze(warmupDocs, SHINGLE_SIZE, FORK_THRESHOLD);
        }
    }

    private static void runComparison(int docCount, ForkJoinAnalyzer par) {
        List<Document> docs = DocumentGenerator.generate(docCount, 300, 0.4, 42L);
        SequentialAnalyzer seq = new SequentialAnalyzer();

        long[] seqTimes = new long[MEASURE_RUNS];
        List<ResemblanceScore> seqResult = null;
        for (int i = 0; i < MEASURE_RUNS; i++) {
            long t = System.nanoTime();
            seqResult = seq.analyze(docs, SHINGLE_SIZE);
            seqTimes[i] = System.nanoTime() - t;
        }

        long[] parTimes = new long[MEASURE_RUNS];
        List<ResemblanceScore> parResult = null;
        for (int i = 0; i < MEASURE_RUNS; i++) {
            long t = System.nanoTime();
            parResult = par.analyze(docs, SHINGLE_SIZE, FORK_THRESHOLD);
            parTimes[i] = System.nanoTime() - t;
        }

        double avgSeq = Arrays.stream(seqTimes).average().orElse(0) / 1_000_000.0;
        double avgPar = Arrays.stream(parTimes).average().orElse(0) / 1_000_000.0;
        boolean valid = validate(seqResult, parResult);
        double speedup = avgSeq / avgPar;

        System.out.printf("%-10d %-15.2f %-15.2f %-12.2f %-10s%n",
                docCount, avgSeq, avgPar, speedup, valid ? "OK" : "FAIL");
    }

    private static boolean validate(List<ResemblanceScore> seq, List<ResemblanceScore> par) {
        if (seq.size() != par.size())
            return false;

        Comparator<ResemblanceScore> comp = Comparator
                .comparingInt(ResemblanceScore::getDocId1)
                .thenComparingInt(ResemblanceScore::getDocId2);

        List<ResemblanceScore> seqSorted = seq.stream().sorted(comp).toList();
        List<ResemblanceScore> parSorted = par.stream().sorted(comp).toList();

        for (int i = 0; i < seqSorted.size(); i++) {
            ResemblanceScore s = seqSorted.get(i);
            ResemblanceScore p = parSorted.get(i);
            if (s.getDocId1() != p.getDocId1() || s.getDocId2() != p.getDocId2())
                return false;
            if (Math.abs(s.getScore() - p.getScore()) > 1e-9)
                return false;
        }
        return true;
    }
}