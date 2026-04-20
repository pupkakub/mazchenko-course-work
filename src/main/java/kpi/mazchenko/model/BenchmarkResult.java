package kpi.mazchenko.model;

public class BenchmarkResult {
    public final int documents;
    public final int parallelism;
    public final double avgSingleMs;
    public final double avgParallelMs;
    public final double realSpeedup;
    public final double realEfficiency;
    public final double serialFraction;

    public BenchmarkResult(int documents, int parallelism, double avgSingleMs, double avgParallelMs) {
        this.documents = documents;
        this.parallelism = parallelism;
        this.avgSingleMs = avgSingleMs;
        this.avgParallelMs = avgParallelMs;
        double p = parallelism;
        this.realSpeedup = avgSingleMs / Math.max(0.001, avgParallelMs);
        this.realEfficiency = this.realSpeedup / p;
        double s = this.realSpeedup;
        double f;
        if (p <= 1.0) {
            f = 1.0;
        } else {
            f = (1.0 / s - 1.0 / p) / (1.0 - 1.0 / p);
            f = Math.max(0.0, Math.min(1.0, f));
        }
        this.serialFraction = f;
    }
}