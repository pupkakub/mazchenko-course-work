package kpi.mazchenko;

import kpi.mazchenko.model.BenchmarkResult;
import kpi.mazchenko.model.Document;
import kpi.mazchenko.model.ResemblanceScore;
import kpi.mazchenko.parallel.ForkJoinAnalyzer;
import kpi.mazchenko.sequential.SequentialAnalyzer;
import kpi.mazchenko.util.DocumentGenerator;
import kpi.mazchenko.util.ResultVisualizer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Main {
    private static final int SHINGLE_SIZE = 3;
    private static final int WARMUP_RUNS = 10;
    private static final int MEASURE_RUNS = 20;

    private static final int DEMO_DOC_COUNT = 30;
    private static final int DEMO_WORDS_PER_DOC = 150;
    private static final double DEMO_OVERLAP = 0.5;

    private static final int THRESHOLD_SEARCH_N = 200;
    private static final int SCALABILITY_N = 500;
    private static final int WORDS_PER_DOC = 1000;
    private static final double GEN_OVERLAP = 0.2;

    private static final int[] THRESHOLD_CANDIDATES = { 5, 10, 25, 50, 100, 200, 500 };
    private static final int[] DATA_SCALE_SIZES = { 100, 200, 500, 1000, 2000 };

    public static void main(String[] args) throws Exception {
        int maxThreads = Runtime.getRuntime().availableProcessors();
        System.out.println("Logical processors: " + maxThreads);

        ForkJoinAnalyzer par = new ForkJoinAnalyzer(maxThreads);
        runDemo(par);
        par.shutdown();

        int bestThreshold = runThresholdSearch(maxThreads);
        List<BenchmarkResult> scalabilityResults = runThreadScalability(maxThreads, bestThreshold);
        List<BenchmarkResult> dataSizeResults = runDataScalability(maxThreads, bestThreshold);

        ResultVisualizer.saveCharts(scalabilityResults, dataSizeResults, maxThreads);
        System.out.println("\nCharts saved to results/");
    }

    private static void runDemo(ForkJoinAnalyzer par) {
        System.out.println("\n=== DEMO ===");
        List<Document> docs = DocumentGenerator.generate(DEMO_DOC_COUNT, DEMO_WORDS_PER_DOC, DEMO_OVERLAP, 7L);
        SequentialAnalyzer seq = new SequentialAnalyzer();
        List<ResemblanceScore> seqResults = seq.analyze(docs, SHINGLE_SIZE);
        seqResults.sort(Comparator.comparingDouble(ResemblanceScore::getScore).reversed());
        if (seqResults.isEmpty()) {
            System.out.println("No matching pairs found.");
        } else {
            for (ResemblanceScore r : seqResults) {
                System.out.println(r);
            }
        }
        List<ResemblanceScore> parResults = par.analyze(docs, SHINGLE_SIZE, 10);
        System.out.println("Total pairs: " + seqResults.size());
        System.out.println("Match valid: " + validate(seqResults, parResults));
    }

    private static int runThresholdSearch(int maxThreads) throws Exception {
        System.out.println(
                "\n=== STAGE 1: THRESHOLD SEARCH (N=" + THRESHOLD_SEARCH_N + ", threads=" + maxThreads + ") ===");
        List<Document> docs = DocumentGenerator.generate(THRESHOLD_SEARCH_N, WORDS_PER_DOC, GEN_OVERLAP, 42L);

        int bestThreshold = THRESHOLD_CANDIDATES[0];
        double minTime = Double.MAX_VALUE;

        System.out.printf("%-12s | %-12s%n", "Threshold", "Time (ms)");
        System.out.println("-".repeat(28));

        for (int th : THRESHOLD_CANDIDATES) {
            ForkJoinAnalyzer fjp = new ForkJoinAnalyzer(maxThreads);
            double t = measurePar(fjp, docs, th);
            fjp.shutdown();
            System.out.printf("%-12d | %-12.2f%n", th, t);
            if (t < minTime) {
                minTime = t;
                bestThreshold = th;
            }
        }

        System.out.println("Best threshold: " + bestThreshold);
        return bestThreshold;
    }

    private static List<BenchmarkResult> runThreadScalability(int maxThreads, int forkThreshold) throws Exception {
        System.out.println("\n=== STAGE 2: THREAD SCALABILITY (N=" + SCALABILITY_N + ") ===");
        List<Document> docs = DocumentGenerator.generate(SCALABILITY_N, WORDS_PER_DOC, GEN_OVERLAP, 42L);
        SequentialAnalyzer seq = new SequentialAnalyzer();
        double tSeq = measureSeq(seq, docs);

        List<BenchmarkResult> results = new ArrayList<>();
        System.out.printf("%-8s | %-10s | %-10s | %-8s | %-12s | %-15s%n",
                "Threads", "Seq(ms)", "Par(ms)", "Speedup", "Efficiency", "SerialFraction");
        System.out.println("-".repeat(72));

        for (int p = 1; p <= maxThreads; p++) {
            ForkJoinAnalyzer fjp = new ForkJoinAnalyzer(p);
            double tPar = measurePar(fjp, docs, forkThreshold);
            fjp.shutdown();
            BenchmarkResult r = new BenchmarkResult(SCALABILITY_N, p, tSeq, tPar);
            results.add(r);
            System.out.printf("%-8d | %-10.2f | %-10.2f | %-8.2f | %-12.4f | %-15.5f%n",
                    r.parallelism, r.avgSingleMs, r.avgParallelMs,
                    r.realSpeedup, r.realEfficiency, r.serialFraction);
        }
        return results;
    }

    private static List<BenchmarkResult> runDataScalability(int maxThreads, int forkThreshold) throws Exception {
        System.out.println("\n=== STAGE 3: DATA SCALABILITY (threads=" + maxThreads + ") ===");
        ForkJoinAnalyzer fjp = new ForkJoinAnalyzer(maxThreads);
        SequentialAnalyzer seq = new SequentialAnalyzer();

        List<BenchmarkResult> results = new ArrayList<>();
        System.out.printf("%-8s | %-10s | %-10s | %-8s | %-12s%n",
                "Docs", "Seq(ms)", "Par(ms)", "Speedup", "Efficiency");
        System.out.println("-".repeat(55));

        for (int n : DATA_SCALE_SIZES) {
            List<Document> docs = DocumentGenerator.generate(n, WORDS_PER_DOC, GEN_OVERLAP, 42L);
            double tSeq = measureSeq(seq, docs);
            double tPar = measurePar(fjp, docs, forkThreshold);
            BenchmarkResult r = new BenchmarkResult(n, maxThreads, tSeq, tPar);
            results.add(r);
            System.out.printf("%-8d | %-10.2f | %-10.2f | %-8.2f | %-12.4f%n",
                    n, r.avgSingleMs, r.avgParallelMs, r.realSpeedup, r.realEfficiency);
        }

        fjp.shutdown();
        return results;
    }

    private static double measureSeq(SequentialAnalyzer sa, List<Document> docs) {
        for (int i = 0; i < WARMUP_RUNS; i++)
            sa.analyze(docs, SHINGLE_SIZE);
        long start = System.nanoTime();
        for (int i = 0; i < MEASURE_RUNS; i++)
            sa.analyze(docs, SHINGLE_SIZE);
        return (System.nanoTime() - start) / (MEASURE_RUNS * 1_000_000.0);
    }

    private static double measurePar(ForkJoinAnalyzer fa, List<Document> docs, int th) {
        for (int i = 0; i < WARMUP_RUNS; i++)
            fa.analyze(docs, SHINGLE_SIZE, th);
        long start = System.nanoTime();
        for (int i = 0; i < MEASURE_RUNS; i++)
            fa.analyze(docs, SHINGLE_SIZE, th);
        return (System.nanoTime() - start) / (MEASURE_RUNS * 1_000_000.0);
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