package kpi.mazchenko.model;

import java.util.Set;

public class Document {
    private final int id;
    private final String text;
    private volatile Set<String> shingles;

    public Document(int id, String text) {
        this.id = id;
        this.text = text;
    }

    public int getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public Set<String> getShingles() {
        return shingles;
    }

    public void setShingles(Set<String> shingles) {
        this.shingles = shingles;
    }
}