package kpi.mazchenko.model;

public class ResemblanceScore {
    private final int docId1;
    private final int docId2;
    private final double score;

    public ResemblanceScore(int docId1, int docId2, double score) {
        this.docId1 = docId1;
        this.docId2 = docId2;
        this.score = score;
    }

    public int getDocId1() {
        return docId1;
    }

    public int getDocId2() {
        return docId2;
    }

    public double getScore() {
        return score;
    }

    @Override
    public String toString() {
        return String.format("Document %d and Document %d | Score: %.4f", docId1, docId2, score);
    }
}