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

    public List<ResemblanceScore> analyze(List<Document> documents, int w, int threshold) {
        pool.invoke(new ShingleGenerationTask(documents, w, 0, documents.size(), threshold));
        return pool.invoke(new PairwiseComparisonTask(documents, 0, documents.size(), threshold));
    }

    public void shutdown() {
        pool.shutdown();
    }

    private static class ShingleGenerationTask extends RecursiveAction {
        private final List<Document> documents;
        private final int w;
        private final int start;
        private final int end;
        private final int threshold;

        ShingleGenerationTask(List<Document> documents, int w, int start, int end, int threshold) {
            this.documents = documents;
            this.w = w;
            this.start = start;
            this.end = end;
            this.threshold = threshold;
        }

        @Override
        protected void compute() {
            if (end - start <= threshold) {
                for (int i = start; i < end; i++) {
                    Document doc = documents.get(i);
                    doc.setShingles(ShingleEngine.extractShingles(doc.getText(), w));
                }
            } else {
                int mid = start + (end - start) / 2;
                invokeAll(
                    new ShingleGenerationTask(documents, w, start, mid, threshold),
                    new ShingleGenerationTask(documents, w, mid, end, threshold)
                );
            }
        }
    }

    private static class PairwiseComparisonTask extends RecursiveTask<List<ResemblanceScore>> {
        private final List<Document> documents;
        private final int start;
        private final int end;
        private final int threshold;

        PairwiseComparisonTask(List<Document> documents, int start, int end, int threshold) {
            this.documents = documents;
            this.start = start;
            this.end = end;
            this.threshold = threshold;
        }

        @Override
        protected List<ResemblanceScore> compute() {
            if (end - start <= threshold) {
                List<ResemblanceScore> local = new ArrayList<>();
                int n = documents.size();
                for (int i = start; i < end; i++) {
                    for (int j = i + 1; j < n; j++) {
                        Document docA = documents.get(i);
                        Document docB = documents.get(j);
                        double score = ShingleEngine.calculateJaccard(docA.getShingles(), docB.getShingles());
                        if (score > 0) {
                            local.add(new ResemblanceScore(docA.getId(), docB.getId(), score));
                        }
                    }
                }
                return local;
            }

            int mid = start + (end - start) / 2;
            PairwiseComparisonTask left = new PairwiseComparisonTask(documents, start, mid, threshold);
            PairwiseComparisonTask right = new PairwiseComparisonTask(documents, mid, end, threshold);
            left.fork();
            List<ResemblanceScore> rightResult = right.compute();
            List<ResemblanceScore> leftResult = left.join();
            leftResult.addAll(rightResult);
            return leftResult;
        }
    }
}