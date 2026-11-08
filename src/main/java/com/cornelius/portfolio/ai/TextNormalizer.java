package com.cornelius.portfolio.ai;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/** Shared text clean-up for FAQ matching, the intent router and the response cache. */
public final class TextNormalizer {

    // Words that carry no meaning when comparing two questions.
    private static final Set<String> STOPWORDS = Set.of(
        "a", "an", "the", "is", "are", "was", "were", "do", "does", "did", "can", "could", "would",
        "i", "me", "my", "you", "your", "he", "his", "him", "she", "her", "it", "its", "of", "to",
        "for", "in", "on", "at", "and", "or", "with", "about", "please", "what", "whats", "which",
        "who", "where", "how", "tell", "give", "show"
    );

    private TextNormalizer() {}

    /** Lower-case, accent-free, punctuation-free, single-spaced. "Résumé?" -> "resume". */
    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String s = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        // Possessives and contractions first ("cornelius's" -> "cornelius", "what's" -> "what").
        s = s.toLowerCase(Locale.ROOT).replaceAll("['’`]s\\b", "").replaceAll("['’`]", "");
        return s.replaceAll("[^a-z0-9]+", " ").trim();
    }

    public static Set<String> tokens(String text) {
        String normalized = normalize(text);
        if (normalized.isEmpty()) {
            return Set.of();
        }
        return new LinkedHashSet<>(Arrays.asList(normalized.split(" ")));
    }

    /** Tokens with filler words removed. */
    public static Set<String> contentTokens(String text) {
        Set<String> tokens = new LinkedHashSet<>(tokens(text));
        tokens.removeAll(STOPWORDS);
        return tokens;
    }
}
