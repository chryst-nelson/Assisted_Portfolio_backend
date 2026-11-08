package com.cornelius.portfolio.ai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Small language toolkit for the rule-based answer engine: tokenising, a light
 * stemmer, stop-words, synonym expansion and typo tolerance. Pure Java, no
 * model and no network: everything here is deterministic.
 */
public final class Lexicon {

    private Lexicon() {}

    /** Words that carry no search value on their own. */
    public static final Set<String> STOPWORDS = setOf(
        "a", "an", "the", "is", "are", "was", "were", "be", "been", "am", "do", "does", "did", "can", "could",
        "would", "will", "should", "shall", "may", "might", "i", "me", "my", "mine", "we", "us", "our", "you",
        "your", "yours", "he", "his", "him", "she", "her", "hers", "they", "them", "their", "it", "its", "of",
        "to", "for", "in", "on", "at", "by", "from", "as", "into", "about", "with", "without", "and", "or", "but",
        "not", "no", "so", "if", "than", "then", "this", "that", "these", "those", "there", "here", "what",
        "whats", "which", "who", "whom", "whose", "where", "when", "why", "how", "please", "tell", "give", "show",
        "let", "lets", "know", "like", "also", "just", "very", "really", "some", "any", "all", "more", "most",
        "have", "has", "had", "get", "got", "want", "need", "see", "find", "list", "hi", "hello", "hey", "thanks",
        "thank", "ok", "okay", "yes", "yeah", "yep", "sure", "much", "many", "every", "each", "other", "such"
    );

    private static final Map<String, String> IRREGULAR = new HashMap<>();
    private static final Map<String, List<String>> EXPANSIONS = new HashMap<>();

    static {
        String[][] irregular = {
            {"built", "build"}, {"made", "make"}, {"knew", "know"}, {"known", "know"}, {"used", "use"},
            {"using", "use"}, {"uses", "use"}, {"worked", "work"}, {"working", "work"}, {"works", "work"},
            {"studied", "study"}, {"studying", "study"}, {"studies", "study"}, {"learnt", "learn"},
            {"learned", "learn"}, {"learning", "learn"}, {"learns", "learn"}, {"took", "take"},
            {"developed", "develop"}, {"developing", "develop"}, {"development", "develop"},
            {"created", "create"}, {"creating", "create"}, {"graduated", "graduate"}, {"won", "win"},
            {"awarded", "award"}, {"awards", "award"}, {"certified", "certificate"},
            {"certifications", "certificate"}, {"certification", "certificate"}, {"certificates", "certificate"},
            {"based", "base"}, {"lives", "live"}, {"lived", "live"}, {"living", "live"}, {"located", "locate"},
            {"employed", "employ"}, {"employer", "employ"}, {"employers", "employ"}, {"employment", "employ"},
            {"jobs", "job"}, {"roles", "role"}, {"companies", "company"}, {"technologies", "technology"},
            {"languages", "language"}, {"frameworks", "framework"}, {"libraries", "library"},
            {"apps", "app"}, {"applications", "application"}, {"websites", "website"}, {"sites", "site"}
        };
        for (String[] pair : irregular) {
            IRREGULAR.put(pair[0], pair[1]);
        }

        // Query-side synonyms: a stem on the left also searches for the stems on the right.
        expand("job", "experience", "role", "company");
        expand("employ", "company", "role", "experience");
        expand("company", "experience", "role");
        expand("career", "experience", "role", "company");
        expand("school", "education", "institution");
        expand("university", "education", "institution");
        expand("college", "education", "institution");
        expand("degree", "education");
        expand("study", "education");
        expand("stack", "skill", "technology");
        expand("tech", "technology", "skill");
        expand("language", "skill", "technology");
        expand("tool", "skill", "technology");
        expand("framework", "skill", "technology");
        expand("strength", "skill");
        expand("app", "project");
        expand("application", "project");
        expand("website", "project");
        expand("site", "project");
        expand("build", "project");
        expand("make", "project");
        expand("create", "project");
        expand("deploy", "devops", "docker");
        expand("cloud", "devops");
        expand("database", "db", "sql");
        expand("db", "database");
        expand("api", "rest");
        expand("frontend", "react", "ui");
        expand("backend", "server");
        expand("ui", "interface", "frontend");
        expand("mobile", "native", "app");
        expand("award", "achievement", "recognition");
        expand("win", "achievement", "award");
        expand("teamwork", "collaborate", "team");
        expand("collaborate", "team");
        expand("test", "testing", "quality");
        expand("bug", "testing", "quality");
        expand("realtime", "real", "time", "socket");
        expand("chat", "message", "socket");
    }

    private static void expand(String from, String... to) {
        EXPANSIONS.put(from, Arrays.asList(to));
    }

    @SafeVarargs
    public static <T> Set<T> setOf(T... items) {
        return new HashSet<>(Arrays.asList(items));
    }

    /** Lower-case, accent-free, punctuation-free words. */
    public static List<String> tokens(String text) {
        String normalized = TextNormalizer.normalize(text);
        if (normalized.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(normalized.split(" ")));
    }

    /** Light suffix stemmer: "projects"/"project" and "building"/"build" meet in the middle. */
    public static String stem(String word) {
        String irregular = IRREGULAR.get(word);
        if (irregular != null) {
            return irregular;
        }
        String s = word;
        if (s.length() <= 3) {
            return s;
        }
        if (s.endsWith("ies") && s.length() > 4) {
            s = s.substring(0, s.length() - 3) + "y";
        } else if (s.endsWith("sses")) {
            s = s.substring(0, s.length() - 2);
        } else if (s.endsWith("ss") || s.endsWith("us") || s.endsWith("is")) {
            // keep: "express", "status", "redis"
        } else if (s.endsWith("s")) {
            s = s.substring(0, s.length() - 1);
        }
        if (s.length() > 5 && s.endsWith("ing")) {
            s = undouble(s.substring(0, s.length() - 3));
        } else if (s.length() > 4 && s.endsWith("ed")) {
            s = undouble(s.substring(0, s.length() - 2));
        }
        if (s.length() > 3 && s.endsWith("e")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    private static String undouble(String s) {
        int n = s.length();
        if (n > 2 && s.charAt(n - 1) == s.charAt(n - 2) && "bdgmnprt".indexOf(s.charAt(n - 1)) >= 0) {
            return s.substring(0, n - 1);
        }
        return s;
    }

    /** Stems of the meaningful words in {@code tokens}, minus stop-words and anything in {@code ignore}. */
    public static List<String> contentStems(List<String> tokens, Set<String> ignore) {
        List<String> out = new ArrayList<>();
        for (String token : tokens) {
            if (STOPWORDS.contains(token) || ignore.contains(token) || token.length() < 2) {
                continue;
            }
            out.add(stem(token));
        }
        return out;
    }

    /** The stems plus their synonyms (deduplicated, original stems first). */
    public static List<String> withSynonyms(List<String> stems) {
        List<String> out = new ArrayList<>();
        for (String stem : stems) {
            if (!out.contains(stem)) {
                out.add(stem);
            }
        }
        for (String stem : stems) {
            List<String> extra = EXPANSIONS.get(stem);
            if (extra == null) {
                continue;
            }
            for (String e : extra) {
                if (!out.contains(e)) {
                    out.add(e);
                }
            }
        }
        return out;
    }

    /**
     * Edit distance with adjacent transpositions counted as one edit
     * ("porject" -> "project" is 1). Returns {@code max + 1} when it exceeds {@code max}.
     */
    public static int distance(String a, String b, int max) {
        int n = a.length();
        int m = b.length();
        if (Math.abs(n - m) > max) {
            return max + 1;
        }
        int[][] d = new int[n + 1][m + 1];
        for (int i = 0; i <= n; i++) {
            d[i][0] = i;
        }
        for (int j = 0; j <= m; j++) {
            d[0][j] = j;
        }
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                d[i][j] = Math.min(Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + cost);
                if (i > 1 && j > 1 && a.charAt(i - 1) == b.charAt(j - 2) && a.charAt(i - 2) == b.charAt(j - 1)) {
                    d[i][j] = Math.min(d[i][j], d[i - 2][j - 2] + 1);
                }
            }
        }
        return Math.min(d[n][m], max + 1);
    }

    /**
     * Returns the closest known word to {@code token} (a likely typo fix), or
     * {@code token} itself when it is already known or nothing is close enough.
     * Only words of five or more letters are corrected, the first letter must
     * match, and words from {@code corpus} (the portfolio's own text) must be
     * at least six letters long, so short real words are never rewritten.
     */
    public static String correct(String token, Set<String> intentWords, Set<String> corpus) {
        if (token.length() < 5 || intentWords.contains(token) || corpus.contains(token)
            || STOPWORDS.contains(token) || containsDigit(token)) {
            return token;
        }
        int max = token.length() >= 11 ? 2 : 1;
        String best = token;
        int bestDistance = max + 1;
        for (String candidate : intentWords) {
            int d = candidateDistance(token, candidate, max);
            if (d < bestDistance || (d == bestDistance && d <= max && candidate.compareTo(best) < 0)) {
                best = candidate;
                bestDistance = d;
            }
        }
        for (String candidate : corpus) {
            if (candidate.length() < 6) {
                continue;
            }
            int d = candidateDistance(token, candidate, max);
            if (d < bestDistance || (d == bestDistance && d <= max && candidate.compareTo(best) < 0)) {
                best = candidate;
                bestDistance = d;
            }
        }
        return best;
    }

    private static int candidateDistance(String token, String candidate, int max) {
        if (candidate.isEmpty() || candidate.charAt(0) != token.charAt(0)) {
            return max + 1;
        }
        return distance(token, candidate, max);
    }

    private static boolean containsDigit(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (Character.isDigit(s.charAt(i))) {
                return true;
            }
        }
        return false;
    }
}
