package com.cornelius.portfolio.ai;

import com.cornelius.portfolio.dto.response.AchievementResponse;
import com.cornelius.portfolio.dto.response.CertificationResponse;
import com.cornelius.portfolio.dto.response.ContactResponse;
import com.cornelius.portfolio.dto.response.EducationResponse;
import com.cornelius.portfolio.dto.response.ExperienceResponse;
import com.cornelius.portfolio.dto.response.FaqResponse;
import com.cornelius.portfolio.dto.response.ProfileResponse;
import com.cornelius.portfolio.dto.response.ProjectChallengeResponse;
import com.cornelius.portfolio.dto.response.ProjectDetailResponse;
import com.cornelius.portfolio.dto.response.ResumeResponse;
import com.cornelius.portfolio.dto.response.SkillResponse;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An immutable, in-memory snapshot of everything the owner has published:
 * profile, skills, projects, experience, education, certifications,
 * achievements, contact, r\u00e9sum\u00e9 and curated FAQ. It also carries a BM25 index
 * over that content and lookup tables that recognise names in a visitor's
 * message ("React", "LearnerX", "NdiliaTalent", ...), including typos.
 */
public final class Knowledge {

    /** A skill category with its skills, used as one searchable passage. */
    public record SkillGroup(String category, List<SkillResponse> skills) {}

    /** One searchable piece of content; {@code ref} is the DTO it came from. */
    public record Passage(String kind, String title, String searchText, Object ref) {}

    private record Entry<T>(T value, List<Pattern> patterns, List<String> fuzzyWords, Pattern strict) {}

    private record Match<T>(T value, int start, int end) {}

    // Aliases that are ordinary English words: only matched with the exact capitalisation of the real name.
    private static final Set<String> AMBIGUOUS = Lexicon.setOf("go", "c", "r", "d", "next", "it", "a", "i", "net");
    // Two-letter aliases that are safe to match case-insensitively.
    private static final Set<String> SAFE_SHORT = Lexicon.setOf("js", "ts", "c#");

    private static final Map<String, List<String>> SYNONYMS = new LinkedHashMap<>();

    static {
        SYNONYMS.put("javascript", List.of("js"));
        SYNONYMS.put("typescript", List.of("ts"));
        SYNONYMS.put("postgresql", List.of("postgres", "psql"));
        SYNONYMS.put("kubernetes", List.of("k8s"));
        SYNONYMS.put("mongodb", List.of("mongo"));
        SYNONYMS.put("amazon web services", List.of("aws"));
        SYNONYMS.put("google cloud platform", List.of("gcp"));
        SYNONYMS.put("spring boot", List.of("spring"));
        SYNONYMS.put("tailwind css", List.of("tailwind"));
        SYNONYMS.put("material ui", List.of("mui"));
        SYNONYMS.put("rest apis", List.of("rest", "restful", "rest api"));
        SYNONYMS.put("jwt authentication", List.of("jwt"));
        SYNONYMS.put("llm integration", List.of("llm", "llms", "large language model"));
        SYNONYMS.put("figma to code", List.of("figma"));
        SYNONYMS.put("github actions", List.of("gh actions"));
        SYNONYMS.put("html5", List.of("html"));
        SYNONYMS.put("css3", List.of("css"));
    }

    private final ProfileResponse profile;
    private final List<SkillResponse> skills;
    private final List<ProjectDetailResponse> projects;
    private final List<ExperienceResponse> experience;
    private final List<EducationResponse> education;
    private final List<CertificationResponse> certifications;
    private final List<AchievementResponse> achievements;
    private final ContactResponse contact;
    private final ResumeResponse resume;
    private final List<FaqResponse> faqs;

    private final Set<String> ownerTokens;
    private final List<Passage> passages;
    private final Bm25Index index;
    private final Set<String> vocabulary;
    private final Map<String, List<SkillResponse>> categories;
    private final Instant builtAt = Instant.now();

    private final List<Entry<SkillResponse>> skillEntries = new ArrayList<>();
    private final List<Entry<ProjectDetailResponse>> projectEntries = new ArrayList<>();
    private final List<Entry<ExperienceResponse>> experienceEntries = new ArrayList<>();
    private final List<Entry<EducationResponse>> educationEntries = new ArrayList<>();
    private final List<Entry<CertificationResponse>> certificationEntries = new ArrayList<>();

    public Knowledge(
        ProfileResponse profile,
        List<SkillResponse> skills,
        List<ProjectDetailResponse> projects,
        List<ExperienceResponse> experience,
        List<EducationResponse> education,
        List<CertificationResponse> certifications,
        List<AchievementResponse> achievements,
        ContactResponse contact,
        ResumeResponse resume,
        List<FaqResponse> faqs
    ) {
        this.profile = profile;
        this.skills = orEmpty(skills);
        this.projects = orEmpty(projects);
        this.experience = orEmpty(experience);
        this.education = orEmpty(education);
        this.certifications = orEmpty(certifications);
        this.achievements = orEmpty(achievements);
        this.contact = contact;
        this.resume = resume;
        this.faqs = orEmpty(faqs);

        this.ownerTokens = new HashSet<>();
        if (profile != null && profile.fullName() != null) {
            this.ownerTokens.addAll(Lexicon.tokens(profile.fullName()));
        }

        this.categories = new LinkedHashMap<>();
        for (SkillResponse skill : this.skills) {
            categories.computeIfAbsent(blank(skill.category()) ? "Other" : skill.category(), k -> new ArrayList<>())
                .add(skill);
        }

        this.passages = buildPassages();
        List<List<String>> documents = new ArrayList<>();
        Set<String> vocab = new HashSet<>();
        for (Passage passage : passages) {
            List<String> tokens = Lexicon.tokens(passage.searchText());
            for (String token : tokens) {
                if (token.length() >= 3 && !Lexicon.STOPWORDS.contains(token)) {
                    vocab.add(token);
                }
            }
            documents.add(Lexicon.contentStems(tokens, ownerTokens));
        }
        this.index = new Bm25Index(documents);
        this.vocabulary = Collections.unmodifiableSet(vocab);

        buildEntries();
    }

    // ---- accessors -------------------------------------------------------------------------

    public ProfileResponse profile() { return profile; }
    public List<SkillResponse> skills() { return skills; }
    public List<ProjectDetailResponse> projects() { return projects; }
    public List<ExperienceResponse> experience() { return experience; }
    public List<EducationResponse> education() { return education; }
    public List<CertificationResponse> certifications() { return certifications; }
    public List<AchievementResponse> achievements() { return achievements; }
    public ContactResponse contact() { return contact; }
    public ResumeResponse resume() { return resume; }
    public List<FaqResponse> faqs() { return faqs; }
    public Set<String> ownerTokens() { return ownerTokens; }
    public List<Passage> passages() { return passages; }
    public Bm25Index index() { return index; }
    public Set<String> vocabulary() { return vocabulary; }
    public Map<String, List<SkillResponse>> categories() { return categories; }
    public Instant builtAt() { return builtAt; }

    // ---- entity lookup ---------------------------------------------------------------------

    public List<SkillResponse> findSkills(String message) { return find(skillEntries, message); }
    public List<ProjectDetailResponse> findProjects(String message) { return find(projectEntries, message); }
    public List<ExperienceResponse> findExperience(String message) { return find(experienceEntries, message); }
    public List<EducationResponse> findEducation(String message) { return find(educationEntries, message); }
    public List<CertificationResponse> findCertifications(String message) { return find(certificationEntries, message); }

    private static <T> List<T> find(List<Entry<T>> entries, String message) {
        if (message == null || entries.isEmpty()) {
            return List.of();
        }
        String lower = message.toLowerCase(Locale.ROOT).replaceAll("[-_/]+", " ").replaceAll("\\s+", " ");
        String[] words = lower.replaceAll("[^a-z0-9 ]", " ").trim().split(" +");

        List<Match<T>> matches = new ArrayList<>();
        for (Entry<T> entry : entries) {
            Match<T> best = null;
            for (Pattern pattern : entry.patterns()) {
                Matcher m = pattern.matcher(lower);
                if (m.find() && (best == null || m.end() - m.start() > best.end() - best.start())) {
                    best = new Match<>(entry.value(), m.start(), m.end());
                }
            }
            if (best == null && entry.strict() != null) {
                Matcher m = entry.strict().matcher(message);
                if (m.find()) {
                    best = new Match<>(entry.value(), m.start(), m.end());
                }
            }
            if (best == null && !entry.fuzzyWords().isEmpty()) {
                outer:
                for (String word : words) {
                    if (word.length() < 6) {
                        continue;
                    }
                    for (String candidate : entry.fuzzyWords()) {
                        if (candidate.charAt(0) == word.charAt(0)
                            && Lexicon.distance(word, candidate, 1) <= 1) {
                            int start = lower.indexOf(word);
                            best = new Match<>(entry.value(), start, start + word.length());
                            break outer;
                        }
                    }
                }
            }
            if (best != null) {
                matches.add(best);
            }
        }

        // A short name inside a longer one ("React" inside "React Native") is not a separate mention.
        List<Match<T>> kept = new ArrayList<>();
        for (Match<T> a : matches) {
            boolean swallowed = false;
            for (Match<T> b : matches) {
                if (a != b && b.start() <= a.start() && b.end() >= a.end()
                    && b.end() - b.start() > a.end() - a.start()) {
                    swallowed = true;
                    break;
                }
            }
            if (!swallowed) {
                kept.add(a);
            }
        }
        kept.sort(Comparator.comparingInt(Match::start));
        List<T> out = new ArrayList<>();
        for (Match<T> m : kept) {
            out.add(m.value());
        }
        return out;
    }

    private void buildEntries() {
        for (SkillResponse skill : skills) {
            skillEntries.add(entry(skill, skill.name(), null));
        }
        for (ProjectDetailResponse project : projects) {
            projectEntries.add(entry(project, project.name(), project.slug()));
        }
        for (ExperienceResponse job : experience) {
            experienceEntries.add(entry(job, job.company(), null));
        }
        for (EducationResponse edu : education) {
            Entry<EducationResponse> e = entry(edu, edu.institution(), edu.course());
            educationEntries.add(e);
        }
        for (CertificationResponse cert : certifications) {
            certificationEntries.add(entry(cert, cert.name(), cert.provider()));
        }
    }

    private static <T> Entry<T> entry(T value, String name, String alternate) {
        Set<String> aliases = new LinkedHashSet<>();
        Pattern strict = null;
        for (String source : new String[] {name, alternate}) {
            if (blank(source)) {
                continue;
            }
            for (String alias : aliasesFor(source)) {
                if (AMBIGUOUS.contains(alias)) {
                    if (source == name) {
                        strict = Pattern.compile("(?<![A-Za-z0-9])" + Pattern.quote(name.trim()) + "(?![A-Za-z0-9])");
                    }
                    continue;
                }
                if (alias.length() < 2 || (alias.length() == 2 && !SAFE_SHORT.contains(alias))) {
                    continue;
                }
                aliases.add(alias);
            }
        }
        List<Pattern> patterns = new ArrayList<>();
        List<String> fuzzy = new ArrayList<>();
        for (String alias : aliases) {
            patterns.add(Pattern.compile("(?<![a-z0-9])" + Pattern.quote(alias) + "(?![a-z0-9])"));
            if (alias.length() >= 6 && alias.matches("[a-z0-9]+")) {
                fuzzy.add(alias);
            }
        }
        return new Entry<>(value, patterns, fuzzy, strict);
    }

    /** Every way a visitor might write {@code name}: spaced, joined, without ".js", plural, abbreviated. */
    static List<String> aliasesFor(String name) {
        Set<String> out = new LinkedHashSet<>();
        String noParens = name.replaceAll("\\(.*?\\)", " ");
        String base = noParens.toLowerCase(Locale.ROOT).replaceAll("[-_/]+", " ").replaceAll("\\s+", " ").trim();
        String camel = noParens.replaceAll("([a-z])([A-Z])", "$1 $2").toLowerCase(Locale.ROOT)
            .replaceAll("[-_/]+", " ").replaceAll("\\s+", " ").trim();
        String normalized = TextNormalizer.normalize(base);

        List<String> seeds = new ArrayList<>();
        seeds.add(base);
        seeds.add(camel);
        seeds.add(normalized);
        for (String seed : new ArrayList<>(seeds)) {
            String noJs = seed.replaceAll("(\\.|\\s)?js$", "");
            if (noJs.length() >= 4) {
                seeds.add(noJs);
            }
            String noDigit = seed.replaceAll("[0-9]+$", "");
            if (noDigit.length() >= 3) {
                seeds.add(noDigit);
            }
        }
        for (String seed : seeds) {
            if (seed.isEmpty()) {
                continue;
            }
            out.add(seed);
            String compact = seed.replaceAll("[^a-z0-9+#]", "");
            if (compact.length() >= 3) {
                out.add(compact);
            }
            List<String> extra = SYNONYMS.get(seed);
            if (extra != null) {
                out.addAll(extra);
            }
        }
        // Singular/plural of the last word.
        for (String alias : new ArrayList<>(out)) {
            if (alias.length() > 3 && alias.endsWith("s")) {
                out.add(alias.substring(0, alias.length() - 1));
            } else if (alias.length() > 3 && Character.isLetter(alias.charAt(alias.length() - 1))) {
                out.add(alias + "s");
            }
        }
        return new ArrayList<>(out);
    }

    // ---- passages --------------------------------------------------------------------------

    private List<Passage> buildPassages() {
        List<Passage> out = new ArrayList<>();
        if (profile != null) {
            out.add(new Passage("profile", "About " + s(profile.fullName()),
                join("profile about who", profile.fullName(), profile.fullName(), profile.title(), profile.bio(),
                    profile.location(), profile.interests(), profile.careerGoals()),
                profile));
        }
        for (Map.Entry<String, List<SkillResponse>> group : categories.entrySet()) {
            List<String> names = new ArrayList<>();
            for (SkillResponse skill : group.getValue()) {
                names.add(skill.name());
            }
            out.add(new Passage("skills", group.getKey() + " skills",
                join("skills technologies", group.getKey(), group.getKey(), String.join(" ", names)),
                new SkillGroup(group.getKey(), group.getValue())));
        }
        for (ProjectDetailResponse p : projects) {
            List<String> parts = new ArrayList<>();
            parts.add("project");
            parts.add(p.name());
            parts.add(p.name());
            parts.add(p.category());
            parts.add(p.description());
            parts.add(p.problemSolved());
            for (SkillResponse tech : orEmpty(p.technologies())) {
                parts.add(tech.name());
            }
            for (ProjectChallengeResponse c : orEmpty(p.challenges())) {
                parts.add(c.challenge());
                parts.add(c.lessonLearned());
            }
            out.add(new Passage("project", p.name(), join(parts.toArray(new String[0])), p));
        }
        for (ExperienceResponse e : experience) {
            List<String> parts = new ArrayList<>();
            parts.add("work experience job");
            parts.add(e.company());
            parts.add(e.company());
            parts.add(e.role());
            parts.add(e.responsibilities());
            for (SkillResponse tech : orEmpty(e.technologies())) {
                parts.add(tech.name());
            }
            parts.addAll(orEmpty(e.achievements()));
            out.add(new Passage("experience", e.role() + " at " + e.company(), join(parts.toArray(new String[0])), e));
        }
        for (EducationResponse e : education) {
            out.add(new Passage("education", s(e.course()) + " at " + s(e.institution()),
                join("education study school", e.institution(), e.degree(), e.course(), e.details()), e));
        }
        for (CertificationResponse c : certifications) {
            out.add(new Passage("certification", c.name(),
                join("certification certificate credential", c.name(), c.name(), c.provider()), c));
        }
        for (AchievementResponse a : achievements) {
            out.add(new Passage("achievement", a.title(),
                join("achievement award", a.title(), a.title(), a.description()), a));
        }
        for (FaqResponse f : faqs) {
            if (f.enabled()) {
                out.add(new Passage("faq", f.question(),
                    join(f.question(), f.question(), f.keywords(), f.answer()), f));
            }
        }
        return out;
    }

    // ---- helpers ---------------------------------------------------------------------------

    private static String join(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!blank(part)) {
                sb.append(part).append(' ');
            }
        }
        return sb.toString().trim();
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    private static String s(String s) {
        return s == null ? "" : s;
    }

    private static <T> List<T> orEmpty(List<T> list) {
        return list == null ? List.of() : list;
    }
}
