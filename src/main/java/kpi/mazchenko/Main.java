package kpi.mazchenko;

import kpi.mazchenko.model.Document;
import kpi.mazchenko.model.ResemblanceScore;
import kpi.mazchenko.parallel.ForkJoinAnalyzer;
import kpi.mazchenko.sequential.SequentialAnalyzer;
import kpi.mazchenko.util.DocumentGenerator;

import java.util.Comparator;
import java.util.List;

public class Main {
    // додати вивід resemblance для текстових фрагментів, збільшити прогони і масиви, зробити графіки
    private static final int SHINGLE_SIZE = 3;
    private static final int THRESHOLD = 10;
    private static final int WARMUP_RUNS = 3;
    private static final int MEASURE_RUNS = 5;

    public static void main(String[] args) {
        int parallelism = Runtime.getRuntime().availableProcessors();
        System.out.println("Logical processors: " + parallelism);

        int[] sizes = {20, 50, 100, 150, 200, 300};

        System.out.printf("%-10s %-15s %-15s %-12s %-10s%n",
                "Docs", "T_seq (ms)", "T_par (ms)", "Speedup", "Valid");
        System.out.println("-".repeat(65));

        ForkJoinAnalyzer par = new ForkJoinAnalyzer(parallelism);
        for (int n : sizes) {
            runComparison(n, par);
        }
        par.shutdown();
    }

    private static void runComparison(int docCount, ForkJoinAnalyzer par) {
        List<Document> docs = DocumentGenerator.generate(docCount, 300, 0.4, 42L);

        SequentialAnalyzer seq = new SequentialAnalyzer();

        for (int i = 0; i < WARMUP_RUNS; i++) {
            seq.analyze(docs, SHINGLE_SIZE);
            par.analyze(docs, SHINGLE_SIZE, THRESHOLD);
        }

        List<ResemblanceScore> seqResult = null;
        long seqStart = System.nanoTime();
        for (int i = 0; i < MEASURE_RUNS; i++) {
            seqResult = seq.analyze(docs, SHINGLE_SIZE);
        }
        double avgSeq = (System.nanoTime() - seqStart) / (double) MEASURE_RUNS / 1_000_000.0;

        List<ResemblanceScore> parResult = null;
        long parStart = System.nanoTime();
        for (int i = 0; i < MEASURE_RUNS; i++) {
            parResult = par.analyze(docs, SHINGLE_SIZE, THRESHOLD);
        }
        double avgPar = (System.nanoTime() - parStart) / (double) MEASURE_RUNS / 1_000_000.0;

        boolean valid = validate(seqResult, parResult);
        double speedup = avgSeq / avgPar;

        System.out.printf("%-10d %-15.2f %-15.2f %-12.2f %-10s%n",
                docCount, avgSeq, avgPar, speedup, valid ? "OK" : "FAIL");
    }

    private static boolean validate(List<ResemblanceScore> seq, List<ResemblanceScore> par) {
        if (seq.size() != par.size()) return false;

        Comparator<ResemblanceScore> comp = Comparator
                .comparingInt(ResemblanceScore::getDocId1)
                .thenComparingInt(ResemblanceScore::getDocId2);

        seq.sort(comp);
        par.sort(comp);

        for (int i = 0; i < seq.size(); i++) {
            ResemblanceScore s = seq.get(i);
            ResemblanceScore p = par.get(i);
            if (s.getDocId1() != p.getDocId1() || s.getDocId2() != p.getDocId2()) return false;
            if (Math.abs(s.getScore() - p.getScore()) > 1e-9) return false;
        }
        return true;
    }
}