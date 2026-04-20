package kpi.mazchenko.sequential;

import kpi.mazchenko.core.ShingleEngine;
import kpi.mazchenko.model.Document;
import kpi.mazchenko.model.ResemblanceScore;

import java.util.ArrayList;
import java.util.List;

public class SequentialAnalyzer {
    public List<ResemblanceScore> analyze(List<Document> documents, int w) {
        for (Document doc : documents) {
            doc.resetShingles();
            doc.setShingles(ShingleEngine.extractShingles(doc.getText(), w));
        }

        List<ResemblanceScore> results = new ArrayList<>();
        int n = documents.size();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                Document docA = documents.get(i);
                Document docB = documents.get(j);
                double score = ShingleEngine.calculateJaccard(docA.getShingles(), docB.getShingles());
                if (score > 0) {
                    results.add(new ResemblanceScore(docA.getId(), docB.getId(), score));
                }
            }
        }
        return results;
    }
}