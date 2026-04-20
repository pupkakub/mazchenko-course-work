package kpi.mazchenko.parallel;

import kpi.mazchenko.core.ShingleEngine;
import kpi.mazchenko.model.Document;
import kpi.mazchenko.model.ResemblanceScore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;

public class ForkJoinAnalyzer {

    private final ForkJoinPool pool;

    public ForkJoinAnalyzer(int parallelism) {
        this.pool = new ForkJoinPool(parallelism);
    }

    public List<ResemblanceScore> analyze(List<Document> documents, int w, int forkThreshold) {
        for (Document doc : documents) {
            doc.resetShingles();
        }
        pool.invoke(new ShingleGenerationTask(documents, w, 0, documents.size(), forkThreshold));

        int n = documents.size();
        List<int[]> pairs = new ArrayList<>(n * (n - 1) / 2);
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                pairs.add(new int[] { i, j });
            }
        }

        return pool.invoke(new PairwiseComparisonTask(documents, pairs, 0, pairs.size(), forkThreshold));
    }

    public void shutdown() {
        pool.shutdown();
    }

    private static class ShingleGenerationTask extends RecursiveAction {
        private final List<Document> documents;
        private final int w;
        private final int start;
        private final int end;
        private final int forkThreshold;

        ShingleGenerationTask(List<Document> documents, int w, int start, int end, int forkThreshold) {
            this.documents = documents;
            this.w = w;
            this.start = start;
            this.end = end;
            this.forkThreshold = forkThreshold;
        }

        @Override
        protected void compute() {
            if (end - start <= forkThreshold) {
                for (int i = start; i < end; i++) {
                    Document doc = documents.get(i);
                    doc.setShingles(ShingleEngine.extractShingles(doc.getText(), w));
                }
            } else {
                int mid = start + (end - start) / 2;
                invokeAll(
                        new ShingleGenerationTask(documents, w, start, mid, forkThreshold),
                        new ShingleGenerationTask(documents, w, mid, end, forkThreshold));
            }
        }
    }

    private static class PairwiseComparisonTask extends RecursiveTask<List<ResemblanceScore>> {
        private final List<Document> documents;
        private final List<int[]> pairs;
        private final int start;
        private final int end;
        private final int forkThreshold;

        PairwiseComparisonTask(List<Document> documents, List<int[]> pairs, int start, int end, int forkThreshold) {
            this.documents = documents;
            this.pairs = pairs;
            this.start = start;
            this.end = end;
            this.forkThreshold = forkThreshold;
        }

        @Override
        protected List<ResemblanceScore> compute() {
            if (end - start <= forkThreshold) {
                List<ResemblanceScore> local = new ArrayList<>();
                for (int k = start; k < end; k++) {
                    int[] pair = pairs.get(k);
                    Document docA = documents.get(pair[0]);
                    Document docB = documents.get(pair[1]);
                    double score = ShingleEngine.calculateJaccard(docA.getShingles(), docB.getShingles());
                    if (score > 0) {
                        local.add(new ResemblanceScore(docA.getId(), docB.getId(), score));
                    }
                }
                return local;
            }

            int mid = start + (end - start) / 2;
            PairwiseComparisonTask left = new PairwiseComparisonTask(documents, pairs, start, mid, forkThreshold);
            PairwiseComparisonTask right = new PairwiseComparisonTask(documents, pairs, mid, end, forkThreshold);
            left.fork();
            List<ResemblanceScore> rightResult = right.compute();
            List<ResemblanceScore> leftResult = left.join();
            leftResult.addAll(rightResult);
            return leftResult;
        }
    }
}