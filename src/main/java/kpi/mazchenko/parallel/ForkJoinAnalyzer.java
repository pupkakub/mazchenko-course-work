package kpi.mazchenko.parallel;

import kpi.mazchenko.core.ShingleEngine;
import kpi.mazchenko.model.Document;
import kpi.mazchenko.model.ResemblanceScore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
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

        int n = documents.size();
        int workers = pool.getParallelism();
        int chunkSize = Math.max(1, (int) Math.ceil((double) n / workers));

        List<ShingleGenerationTask> shingleTasks = new ArrayList<>();
        for (int i = 0; i < n; i += chunkSize) {
            shingleTasks.add(new ShingleGenerationTask(documents, w, i, Math.min(i + chunkSize, n)));
        }

        pool.invoke(new RecursiveAction() {
            protected void compute() {
                ForkJoinTask.invokeAll(shingleTasks);
            }
        });

        long[] rowOffsets = new long[n];
        long currentOffset = 0;
        for (int i = 0; i < n; i++) {
            rowOffsets[i] = currentOffset;
            currentOffset += (n - 1 - i);
        }
        long totalPairs = currentOffset;

        return pool.invoke(new PairwiseComparisonTask(documents, rowOffsets, 0, totalPairs, forkThreshold));
    }

    public void shutdown() {
        pool.shutdown();
    }

    public int getParallelism() {
        return pool.getParallelism();
    }

    private static class ShingleGenerationTask extends RecursiveAction {
        private final List<Document> documents;
        private final int w;
        private final int start;
        private final int end;

        ShingleGenerationTask(List<Document> documents, int w, int start, int end) {
            this.documents = documents;
            this.w = w;
            this.start = start;
            this.end = end;
        }

        @Override
        protected void compute() {
            for (int i = start; i < end; i++) {
                Document doc = documents.get(i);
                doc.setShingles(ShingleEngine.extractShingles(doc.getText(), w));
            }
        }
    }

    private static class PairwiseComparisonTask extends RecursiveTask<List<ResemblanceScore>> {
        private final List<Document> documents;
        private final long[] rowOffsets;
        private final long start;
        private final long end;
        private final int forkThreshold;

        PairwiseComparisonTask(List<Document> documents, long[] rowOffsets, long start, long end, int forkThreshold) {
            this.documents = documents;
            this.rowOffsets = rowOffsets;
            this.start = start;
            this.end = end;
            this.forkThreshold = forkThreshold;
        }

        @Override
        protected List<ResemblanceScore> compute() {
            if (end - start <= forkThreshold) {
                List<ResemblanceScore> local = new ArrayList<>();
                for (long k = start; k < end; k++) {
                    int i = getRow(k);
                    int j = i + 1 + (int) (k - rowOffsets[i]);
                    Document docA = documents.get(i);
                    Document docB = documents.get(j);
                    double score = ShingleEngine.calculateJaccard(docA.getShingles(), docB.getShingles());
                    if (score > 0) {
                        local.add(new ResemblanceScore(docA.getId(), docB.getId(), score));
                    }
                }
                return local;
            }

            long mid = start + (end - start) / 2;
            PairwiseComparisonTask left = new PairwiseComparisonTask(documents, rowOffsets, start, mid, forkThreshold);
            PairwiseComparisonTask right = new PairwiseComparisonTask(documents, rowOffsets, mid, end, forkThreshold);
            left.fork();
            List<ResemblanceScore> rightResult = right.compute();
            List<ResemblanceScore> leftResult = left.join();
            leftResult.addAll(rightResult);
            return leftResult;
        }

        private int getRow(long k) {
            int left = 0, right = rowOffsets.length - 1, ans = 0;
            while (left <= right) {
                int mid = left + (right - left) / 2;
                if (rowOffsets[mid] <= k) {
                    ans = mid;
                    left = mid + 1;
                } else {
                    right = mid - 1;
                }
            }
            return ans;
        }
    }
}