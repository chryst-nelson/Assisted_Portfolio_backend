package com.cornelius.portfolio.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Okapi BM25 ranking over a handful of short documents, held in memory. */
public final class Bm25Index {

    private static final double K1 = 1.4;
    private static final double B = 0.75;

    public record Hit(int doc, double score, int matchedTerms) {
    }

    private final List<Map<String, Integer>> termFrequencies = new ArrayList<>();
    private final List<Integer> lengths = new ArrayList<>();
    private final Map<String, Integer> documentFrequency = new HashMap<>();
    private final double averageLength;

    public Bm25Index(List<List<String>> documents) {
        long total = 0;
        for (List<String> doc : documents) {
            Map<String, Integer> tf = new HashMap<>();
            for (String term : doc) {
                tf.merge(term, 1, Integer::sum);
            }
            for (String term : tf.keySet()) {
                documentFrequency.merge(term, 1, Integer::sum);
            }
            termFrequencies.add(tf);
            lengths.add(doc.size());
            total += doc.size();
        }
        this.averageLength = documents.isEmpty() ? 1.0 : Math.max(1.0, (double) total / documents.size());
    }

    public boolean knows(String term) {
        return documentFrequency.containsKey(term);
    }

    /** Whether document {@code doc} contains {@code term}. */
    public boolean has(int doc, String term) {
        return termFrequencies.get(doc).containsKey(term);
    }

    /** Documents that match at least one query term, best first. */
    public List<Hit> search(List<String> query) {
        Set<String> terms = new HashSet<>(query);
        int n = termFrequencies.size();
        List<Hit> hits = new ArrayList<>();
        for (int doc = 0; doc < n; doc++) {
            Map<String, Integer> tf = termFrequencies.get(doc);
            double score = 0;
            int matched = 0;
            for (String term : terms) {
                Integer f = tf.get(term);
                if (f == null) {
                    continue;
                }
                matched++;
                int df = documentFrequency.getOrDefault(term, 0);
                double idf = Math.log(1.0 + (n - df + 0.5) / (df + 0.5));
                double norm = f + K1 * (1 - B + B * lengths.get(doc) / averageLength);
                score += idf * (f * (K1 + 1)) / norm;
            }
            if (matched > 0) {
                hits.add(new Hit(doc, score, matched));
            }
        }
        hits.sort((x, y) -> Double.compare(y.score(), x.score()));
        return hits;
    }
}
