package kpi.mazchenko.model;

public class BenchmarkResult {
    public final int documents;
    public final int parallelism;
    public final int maxThreads;
    public final double avgSingleMs;
    public final double avgParallelMs;
    public final double realSpeedup;
    public final double realEfficiency;

    public BenchmarkResult(int documents, int parallelism, int maxThreads, double avgSingleMs, double avgParallelMs) {
        this.documents = documents;
        this.parallelism = parallelism;
        this.maxThreads = maxThreads;
        this.avgSingleMs = avgSingleMs;
        this.avgParallelMs = avgParallelMs;
        this.realSpeedup = avgSingleMs / Math.max(0.001, avgParallelMs);
        this.realEfficiency = this.realSpeedup / maxThreads;
    }
}