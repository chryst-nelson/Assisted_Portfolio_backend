package com.cornelius.portfolio.service;

import com.cornelius.portfolio.ai.Bm25Index;
import com.cornelius.portfolio.ai.ChatMessageResponse;
import com.cornelius.portfolio.ai.ChatResponseType;
import com.cornelius.portfolio.ai.Knowledge;
import com.cornelius.portfolio.ai.KnowledgeBase;
import com.cornelius.portfolio.ai.Lexicon;
import com.cornelius.portfolio.dto.response.AchievementResponse;
import com.cornelius.portfolio.dto.response.CertificationResponse;
import com.cornelius.portfolio.dto.response.ContactResponse;
import com.cornelius.portfolio.dto.response.EducationResponse;
import com.cornelius.portfolio.dto.response.ExperienceResponse;
import com.cornelius.portfolio.dto.response.FaqResponse;
import com.cornelius.portfolio.dto.response.ProfileResponse;
import com.cornelius.portfolio.dto.response.ProjectChallengeResponse;
import com.cornelius.portfolio.dto.response.ProjectDetailResponse;
import com.cornelius.portfolio.dto.response.ProjectSummaryResponse;
import com.cornelius.portfolio.dto.response.SkillResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Answers visitor questions with no language model at all. Everything it says
 * is read from the owner's published data (held in {@link Knowledge}) and
 * assembled from templates, so it can only state facts that are in the
 * portfolio.
 *
 * Pipeline: curated FAQ, small talk, entity recognition (a project, company,
 * skill... named in the question), intent rules, an "unknown technology"
 * check, BM25 search over all portfolio text, and finally an honest
 * "I don't have that" reply with the contact card.
 */
@Service
public class AnswerEngine {

    // ---- intent vocabulary (also feeds typo correction) -------------------------------------

    private static final String[] W_RESUME = {"resume", "cv", "vitae", "curriculum"};
    private static final String[] W_CONTACT = {"contact", "email", "mail", "reach", "linkedin", "phone",
        "whatsapp", "telephone", "touch", "twitter", "socials", "github"};
    private static final String[] W_AVAIL = {"available", "availability", "freelance", "hire", "hiring",
        "opening", "openings", "opportunity", "opportunities"};
    private static final String[] W_MONEY = {"salary", "rate", "rates", "compensation", "pay", "paid", "fee",
        "fees", "charge", "price", "pricing", "quote", "budget", "expectation", "expectations"};
    private static final String[] W_YEAR = {"year", "years", "long"};
    private static final String[] W_EXP_CTX = {"experience", "experienced", "career", "work", "working", "coding",
        "programming", "developer", "industry", "professional", "professionally"};
    private static final String[] W_CURRENT = {"current", "currently", "now", "present", "today", "right"};
    private static final String[] W_ROLE_CTX = {"role", "job", "work", "company", "employer", "position", "doing",
        "employed", "working", "title"};
    private static final String[] W_LEARN = {"learn", "learning", "exploring"};
    private static final String[] W_BEST = {"best", "strongest", "strength", "strengths", "expert", "expertise",
        "main", "primary", "core", "top", "favorite", "favourite", "specialize", "specialise", "specialty",
        "specialization", "proficient", "excel", "strong"};
    private static final String[] W_SKILL = {"skill", "technology", "tech", "stack", "language", "framework",
        "tool", "library", "programming", "competency", "proficiency", "technologies", "languages"};
    private static final String[] W_PROJECT = {"project", "app", "application", "website", "site", "built", "build",
        "portfolio", "showcase", "example", "sample", "demo", "created", "made", "product", "repo"};
    private static final String[] W_LATEST = {"latest", "recent", "recently", "newest", "last", "new"};
    private static final String[] W_EXP = {"experience", "job", "career", "employment", "employer", "company",
        "companies", "role", "position", "intern", "internship", "professional", "history", "employed"};
    private static final String[] W_EDU = {"education", "educational", "degree", "university", "college", "school",
        "study", "studied", "graduate", "graduated", "graduation", "qualification", "academic", "bsc", "msc",
        "diploma", "campus", "course", "courses"};
    private static final String[] W_CERT = {"certification", "certifications", "certificate", "certificates",
        "certified", "credential", "credentials", "badge", "accreditation"};
    private static final String[] W_ACH = {"achievement", "achievements", "accomplishment", "accomplishments",
        "award", "awards", "recognition", "honor", "honour", "prize", "milestone", "accolade", "won", "win"};
    private static final String[] W_LOC = {"location", "located", "live", "lives", "country", "city", "based",
        "timezone", "reside", "relocate", "relocation", "nationality"};
    private static final String[] W_GOAL = {"goal", "goals", "aspiration", "ambition", "aim", "future", "seeking",
        "aspire", "objective", "dream", "vision", "plans"};
    private static final String[] W_INTEREST = {"interest", "interests", "hobby", "hobbies", "passion",
        "passionate", "enjoy"};
    private static final String[] W_PROFILE = {"bio", "biography", "introduce", "introduction", "summary",
        "overview", "describe", "profile", "background"};
    private static final String[] W_CUE = {"know", "knows", "use", "uses", "familiar", "experience", "experienced",
        "proficient", "skilled", "expertise", "expert", "comfortable", "exposure", "handle", "competent",
        "fluent", "versed", "background"};
    private static final String[] W_MISC = {"currently", "current", "previous", "history", "worked", "education",
        "technologies", "experience", "projects", "skills", "certifications", "achievements", "location",
        "developer", "engineer", "frontend", "backend", "fullstack", "devops", "mobile", "database", "cloud",
        "tools", "thanks", "hello", "goodbye", "assistant", "questions", "portfolio", "career"};

    private static final Set<String> INTENT_VOCAB = new HashSet<>();
    // Words too common in a portfolio to say anything about what a visitor wants.
    private static final Set<String> GENERIC_TERMS = Lexicon.setOf("develop", "work", "use", "build", "make", "project",
        "experience", "skill", "technology", "tool", "thing", "stuff", "good", "great", "best", "new", "know", "kind",
        "type", "anything", "something", "everything", "tell", "knowledge", "background", "detail", "info",
        "information", "question", "ask", "daily", "mostly", "usually", "often");
    private static final Set<String> INTENT_STEMS = new HashSet<>();

    static {
        for (String[] group : new String[][] {W_RESUME, W_CONTACT, W_AVAIL, W_MONEY, W_YEAR, W_EXP_CTX, W_CURRENT,
            W_ROLE_CTX, W_LEARN, W_BEST, W_SKILL, W_PROJECT, W_LATEST, W_EXP, W_EDU, W_CERT, W_ACH, W_LOC, W_GOAL,
            W_INTEREST, W_PROFILE, W_CUE, W_MISC}) {
            INTENT_VOCAB.addAll(Arrays.asList(group));
        }
    }

    static {
        for (String word : INTENT_VOCAB) {
            INTENT_STEMS.add(Lexicon.stem(word));
        }
        INTENT_STEMS.addAll(GENERIC_TERMS);
    }

    private static final Set<String> CUE_STEMS = new HashSet<>();
    private static final Set<String> CUE_SKIP = Lexicon.setOf("with", "in", "at", "on", "of", "to", "any", "the", "a",
        "an", "some", "using", "knowing", "working", "programming", "coding", "development", "language",
        "languages", "framework", "technology", "tech", "skills", "skill", "stack", "tools", "tool", "library",
        "good", "strong", "hands", "prior", "previous", "professional", "practical", "real", "world", "deep",
        "solid", "extensive", "enough", "much", "he", "his", "you", "your");
    private static final Set<String> NOISE = Lexicon.setOf("lot", "lots", "bit", "thing", "things", "stuff", "time",
        "way", "kind", "sort", "type", "idea", "clue", "answer", "question", "questions", "more", "something");

    static {
        for (String w : W_CUE) {
            CUE_STEMS.add(Lexicon.stem(w));
        }
    }

    private static final Set<String> GREETING = Lexicon.setOf("hi", "hello", "hey", "howdy", "yo", "sup", "greetings",
        "good", "morning", "afternoon", "evening", "there", "hiya", "hola", "welcome", "wassup", "whatsup", "how",
        "are", "you", "doing", "today", "is", "it", "going", "u", "r", "everyone", "all", "again", "back", "nice",
        "to", "meet", "assistant", "bot");
    private static final Set<String> THANKS = Lexicon.setOf("thanks", "thank", "thx", "ty", "cheers", "appreciate",
        "appreciated", "grateful", "you", "so", "much", "a", "lot", "very", "really", "that", "was", "is", "helpful",
        "great", "for", "the", "help", "info", "answer", "it", "i", "nice", "awesome", "perfect", "good", "much");
    private static final Set<String> BYE = Lexicon.setOf("bye", "goodbye", "cya", "later", "farewell", "see", "you",
        "soon", "take", "care", "good", "night", "talk", "ttyl", "gotta", "go", "now", "have", "a", "great", "day");
    private static final Set<String> ACK = Lexicon.setOf("ok", "okay", "cool", "great", "nice", "awesome", "good",
        "wow", "alright", "fine", "perfect", "sure", "yes", "yeah", "yep", "no", "nope", "hmm", "k", "got", "it",
        "i", "see", "sounds", "right", "lol", "haha", "interesting", "amazing", "impressive", "noted", "understood");

    private static final DateTimeFormatter MONTH_YEAR = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);
    private static final int MAX_TOKENS = 80;
    private static final double MIN_SEARCH_SCORE = 0.8;

    private final KnowledgeBase knowledgeBase;
    private final FaqService faqService;

    public AnswerEngine(KnowledgeBase knowledgeBase, FaqService faqService) {
        this.knowledgeBase = knowledgeBase;
        this.faqService = faqService;
    }

    // ---- entry point ------------------------------------------------------------------------

    public ChatMessageResponse answer(String message, String mode) {
        Knowledge k = knowledgeBase.current();

        Optional<String> faq = faqService.findAnswer(message);
        if (faq.isPresent()) {
            return text(faq.get());
        }

        Query q = new Query(message, k);
        if (q.tokens.isEmpty()) {
            return text("Ask me anything about " + owner(k) + ": skills, projects, work experience, education, "
                + "certifications, or how to get in touch.");
        }

        ChatMessageResponse reply = smallTalk(q, k);
        if (reply == null) {
            reply = route(q, k, mode);
        }
        if (reply == null) {
            reply = search(q, k, mode);
        }
        return reply != null ? reply : notFound(q, k);
    }

    // ---- query ------------------------------------------------------------------------------

    private static final class Query {
        final String raw;
        final List<String> tokens;
        final Set<String> stems = new HashSet<>();
        final String padded;
        final List<String> terms;

        Query(String raw, Knowledge k) {
            this.raw = raw == null ? "" : raw;
            List<String> corrected = new ArrayList<>();
            Set<String> categoryWords = new HashSet<>();
            for (String category : k.categories().keySet()) {
                categoryWords.addAll(Lexicon.tokens(category));
            }
            Set<String> known = new HashSet<>(INTENT_VOCAB);
            known.addAll(categoryWords);
            for (String token : Lexicon.tokens(this.raw)) {
                if (corrected.size() >= MAX_TOKENS) {
                    break;
                }
                corrected.add(Lexicon.correct(token, known, k.vocabulary()));
            }
            this.tokens = corrected;
            for (String token : corrected) {
                stems.add(Lexicon.stem(token));
            }
            this.padded = " " + String.join(" ", corrected) + " ";
            this.terms = Lexicon.contentStems(corrected, k.ownerTokens());
        }

        boolean any(String... words) {
            for (String word : words) {
                if (stems.contains(Lexicon.stem(word))) {
                    return true;
                }
            }
            return false;
        }

        boolean phrase(String... phrases) {
            for (String phrase : phrases) {
                if (padded.contains(" " + phrase + " ")) {
                    return true;
                }
            }
            return false;
        }

        boolean onlyOf(Set<String> vocabulary, Set<String> owner) {
            for (String token : tokens) {
                if (!vocabulary.contains(token) && !owner.contains(token)) {
                    return false;
                }
            }
            return true;
        }
    }

    // ---- small talk -------------------------------------------------------------------------

    private ChatMessageResponse smallTalk(Query q, Knowledge k) {
        Set<String> owner = k.ownerTokens();

        if (q.phrase("who are you", "what are you", "are you a bot", "are you an ai", "are you ai", "are you human",
            "are you real", "are you a robot", "are you a person", "are you chatgpt", "are you gemini",
            "which model", "what model", "who made you", "who built you", "who created you", "are you an assistant",
            "what is this", "what is this site", "what is this website")) {
            return text("I'm the automated assistant built into " + possessive(k) + " portfolio. I don't improvise: "
                + "every answer comes straight from the information he has published here, such as his skills, "
                + "projects, experience, education and contact details. What would you like to know?");
        }
        if (q.phrase("what can you do", "what can i ask", "how do you work", "how does this work", "what questions",
            "what do you know", "what should i ask", "help me", "need help") || (q.tokens.size() <= 2 && q.any("help"))) {
            return text(helpText(k));
        }
        if (q.any("thanks", "thank", "thx", "cheers", "appreciate", "grateful") && q.onlyOf(THANKS, owner)) {
            return text("You're welcome! Anything else you'd like to know about " + owner(k) + "?");
        }
        if (q.any("bye", "goodbye", "cya", "farewell", "later", "ttyl") && q.onlyOf(BYE, owner)) {
            return text("Goodbye! Thanks for stopping by. Feel free to come back any time.");
        }
        if (q.phrase("how are you", "how are you doing", "how is it going", "hows it going", "how r u", "how are u")
            && q.onlyOf(GREETING, owner)) {
            return text("Doing well, thanks for asking! " + shortHelp(k));
        }
        if (q.any("hi", "hello", "hey", "howdy", "yo", "sup", "greetings", "hiya", "hola", "wassup", "whatsup",
            "morning", "afternoon", "evening") && q.onlyOf(GREETING, owner)) {
            return text("Hi there! " + shortHelp(k));
        }
        if (q.onlyOf(ACK, owner) && q.tokens.stream().anyMatch(ACK::contains)) {
            return text("Glad that helps! Is there anything else you'd like to know about " + owner(k) + "?");
        }
        return null;
    }

    private String helpText(Knowledge k) {
        return "You can ask me about " + possessive(k) + " skills and technologies, projects (try a project's name for "
            + "details), work experience, education, certifications and achievements. I can also share his "
            + "r\u00e9sum\u00e9 and contact details. For example: \"What projects use React?\", \"How many years of "
            + "experience does he have?\" or \"Does he know Docker?\"";
    }

    private String shortHelp(Knowledge k) {
        return "I can tell you about " + possessive(k) + " skills, projects, experience, education and "
            + "certifications, or share his r\u00e9sum\u00e9 and contact details. What would you like to know?";
    }

    // ---- routing ----------------------------------------------------------------------------

    private ChatMessageResponse route(Query q, Knowledge k, String mode) {
        boolean brief = "RECRUITER".equalsIgnoreCase(mode);
        boolean detailed = "DEVELOPER".equalsIgnoreCase(mode);

        // 1. Explicit, unambiguous requests.
        List<ProjectDetailResponse> projects = k.findProjects(q.raw);
        if (q.any(W_RESUME) && projects.isEmpty()) {
            return resume(k);
        }
        if (q.any(W_MONEY) && !q.any("project", "app")) {
            return money(k);
        }
        if (q.any(W_AVAIL) || q.phrase("open to work", "looking for work", "work with him", "work with you",
            "available for")) {
            return availability(k);
        }
        if (q.any(W_CONTACT) && projects.isEmpty() && !(q.any("github") && q.any("project", "repo"))) {
            return contact(k);
        }

        // 2. A specific thing was named.
        List<SkillResponse> skills = k.findSkills(q.raw);
        if (!projects.isEmpty()) {
            return projectAnswer(projects, skills, q, k, brief, detailed);
        }
        List<ExperienceResponse> companies = k.findExperience(q.raw);
        if (!companies.isEmpty()) {
            return companyAnswer(companies, q, k, brief);
        }
        List<CertificationResponse> certs = k.findCertifications(q.raw);
        if (!certs.isEmpty()) {
            return certificationAnswer(certs, k);
        }
        List<EducationResponse> schools = k.findEducation(q.raw);
        if (!schools.isEmpty()) {
            return educationAnswer(schools, k);
        }
        if (!skills.isEmpty()) {
            boolean wantsProjects = q.any(W_PROJECT) || q.phrase("worked on", "built with", "built using");
            return wantsProjects ? projectsUsing(skills, k) : skillCheck(skills, k);
        }

        // 3. "Does he know <something not in the portfolio>?"
        String unknown = unknownSkillTerm(q, k);
        if (unknown != null) {
            return unknownSkill(unknown, k);
        }

        // 4. Intents without a named thing.
        if (q.any(W_GOAL)) {
            return goals(k);
        }
        if (q.any(W_INTEREST) || q.phrase("free time")) {
            return interests(k);
        }
        if (q.any(W_YEAR) && (q.any(W_EXP_CTX) || q.phrase("how long"))) {
            return years(k);
        }
        List<String> residual = residualTerms(q, k);
        if (!residual.isEmpty()) {
            ChatMessageResponse found = searchTerms(q, k, residual, true, mode);
            if (found != null) {
                return found;
            }
        }
        if ((q.any(W_CURRENT) && q.any(W_ROLE_CTX))
            || q.phrase("where does he work", "where do you work", "where is he working", "who does he work for",
                "what is his job", "what is he doing")) {
            return currentRole(k);
        }
        if (q.phrase("what does he do", "what do you do", "what does he do for a living")) {
            return profileAnswer(k, brief);
        }
        if (q.any(W_LEARN)) {
            return learning(k);
        }
        List<String> categoryHits = matchingCategories(q, k);
        if (q.any(W_BEST) && (q.any(W_SKILL) || q.phrase("good at", "best at", "strong in", "specialize in",
            "specialise in", "strongest at")) || (q.any("expertise", "specialty", "specialization", "strengths",
            "strength", "specialize", "specialise") && categoryHits.isEmpty())) {
            return topSkills(k, brief);
        }
        if (q.any(W_SKILL) || !categoryHits.isEmpty()) {
            return skillsList(k, categoryHits, q);
        }
        if (q.any(W_LATEST) && q.any(W_PROJECT)) {
            return latestProject(k);
        }
        if (q.any(W_PROJECT)) {
            return hint(projectsList(k, brief), q, "projects");
        }
        if (q.any(W_CERT)) {
            return hint(certificationAnswer(k.certifications(), k), q, "certifications");
        }
        if (q.any(W_EDU)) {
            return hint(educationAnswer(k.education(), k), q, "education");
        }
        if (q.any(W_EXP) || q.phrase("work history", "work experience", "worked at", "worked for",
            "where has he worked", "where did he work", "places he has worked")) {
            return hint(experienceList(k, brief), q, "experience");
        }
        if (q.any(W_ACH)) {
            return achievementAnswer(k);
        }
        if (q.any(W_LOC) || q.phrase("where is he", "where are you", "where is he from", "where are you from")) {
            return location(k);
        }
        boolean nameOnly = q.tokens.stream().anyMatch(k.ownerTokens()::contains) && q.tokens.stream().allMatch(
            t -> k.ownerTokens().contains(t) || Lexicon.STOPWORDS.contains(t));
        if (q.any(W_PROFILE) || nameOnly
            || q.phrase("tell me about", "about him", "about you", "about yourself", "about himself", "who is he",
                "who is this", "who is that", "who he is", "what kind of developer", "what type of developer")) {
            return profileAnswer(k, brief);
        }
        return null;
    }

    /** Content words of the question once intent words, category names and the owner's name are removed. */
    private List<String> residualTerms(Query q, Knowledge k) {
        Set<String> categoryStems = new HashSet<>();
        for (String category : k.categories().keySet()) {
            for (String word : Lexicon.tokens(category)) {
                categoryStems.add(Lexicon.stem(word));
            }
        }
        List<String> out = new ArrayList<>();
        for (String term : q.terms) {
            if (!INTENT_STEMS.contains(term) && !categoryStems.contains(term) && !out.contains(term)) {
                out.add(term);
            }
        }
        return out;
    }

    private List<String> matchingCategories(Query q, Knowledge k) {
        List<String> out = new ArrayList<>();
        for (String category : k.categories().keySet()) {
            for (String word : Lexicon.tokens(category)) {
                if (word.length() >= 4 && !Lexicon.STOPWORDS.contains(word) && q.stems.contains(Lexicon.stem(word))
                    && !out.contains(category)) {
                    out.add(category);
                }
            }
        }
        return out;
    }

    private String unknownSkillTerm(Query q, Knowledge k) {
        List<String> t = q.tokens;
        for (int i = 0; i < t.size(); i++) {
            String stem = Lexicon.stem(t.get(i));
            boolean workWith = stem.equals("work") && i + 1 < t.size()
                && (t.get(i + 1).equals("with") || t.get(i + 1).equals("using"));
            if (!CUE_STEMS.contains(stem) && !workWith) {
                continue;
            }
            int j = i + 1;
            while (j < t.size() && CUE_SKIP.contains(t.get(j))) {
                j++;
            }
            if (j >= t.size()) {
                continue;
            }
            String candidate = t.get(j);
            if (candidate.length() < 3 || Lexicon.STOPWORDS.contains(candidate) || NOISE.contains(candidate)
                || k.ownerTokens().contains(candidate) || INTENT_VOCAB.contains(candidate)
                || k.vocabulary().contains(candidate)) {
                continue;
            }
            return candidate;
        }
        return null;
    }

    // ---- simple fact answers ----------------------------------------------------------------

    private ChatMessageResponse resume(Knowledge k) {
        if (k.resume() == null) {
            return withContact(k, "A r\u00e9sum\u00e9 hasn't been uploaded yet. You can ask him for it directly.");
        }
        String version = blank(k.resume().version()) ? "" : " (version " + k.resume().version() + ")";
        return new ChatMessageResponse(ChatResponseType.RESUME, "Here is his r\u00e9sum\u00e9" + version + ".", k.resume());
    }

    private ChatMessageResponse contact(Knowledge k) {
        ContactResponse c = k.contact();
        if (c == null) {
            return text("No contact details have been published yet.");
        }
        List<String> channels = new ArrayList<>();
        if (!blank(c.email())) {
            channels.add("email (" + c.email() + ")");
        }
        if (!blank(c.linkedinUrl())) {
            channels.add("LinkedIn");
        }
        if (!blank(c.githubUrl())) {
            channels.add("GitHub");
        }
        String message = channels.isEmpty()
            ? "Here is how to reach him."
            : "You can reach him by " + list(channels) + ". Details below.";
        return new ChatMessageResponse(ChatResponseType.CONTACT, message, c);
    }

    private ChatMessageResponse availability(Knowledge k) {
        StringBuilder sb = new StringBuilder("I don't have his current availability on record.");
        ProfileResponse p = k.profile();
        if (p != null && !blank(p.careerGoals())) {
            sb.append(" What he has published: ").append(p.careerGoals().trim());
        }
        sb.append(" The quickest way to find out is to contact him directly.");
        return withContact(k, sb.toString());
    }

    private ChatMessageResponse money(Knowledge k) {
        return withContact(k, "He hasn't published salary or rate information, so I can't speak to that. "
            + "For anything commercial, please contact him directly.");
    }

    private ChatMessageResponse location(Knowledge k) {
        ProfileResponse p = k.profile();
        if (p == null || blank(p.location())) {
            return text("His location isn't listed in the portfolio.");
        }
        return text("He is based in " + p.location().trim() + ".");
    }

    private ChatMessageResponse goals(Knowledge k) {
        ProfileResponse p = k.profile();
        if (p == null || blank(p.careerGoals())) {
            return text("He hasn't published career goals yet.");
        }
        return text("His career goals: " + p.careerGoals().trim());
    }

    private ChatMessageResponse interests(Knowledge k) {
        ProfileResponse p = k.profile();
        if (p == null || blank(p.interests())) {
            return text("He hasn't listed his interests yet.");
        }
        return text("His interests: " + p.interests().trim());
    }

    private ChatMessageResponse learning(Knowledge k) {
        List<SkillResponse> learning = new ArrayList<>();
        for (SkillResponse s : k.skills()) {
            if (s.currentlyLearning()) {
                learning.add(s);
            }
        }
        if (learning.isEmpty()) {
            return text("Nothing is marked as \"currently learning\" in his profile right now.");
        }
        return new ChatMessageResponse(ChatResponseType.SKILL_LIST,
            "He is currently learning " + list(names(learning)) + ".", learning);
    }

    private ChatMessageResponse years(Knowledge k) {
        if (k.experience().isEmpty()) {
            return text("No work experience is listed in the portfolio yet.");
        }
        long months = totalMonths(k.experience());
        boolean allInterns = true;
        for (ExperienceResponse e : k.experience()) {
            if (e.role() == null || !e.role().toLowerCase(Locale.ROOT).contains("intern")) {
                allInterns = false;
            }
        }
        boolean anyIntern = false;
        for (ExperienceResponse e : k.experience()) {
            if (e.role() != null && e.role().toLowerCase(Locale.ROOT).contains("intern")) {
                anyIntern = true;
            }
        }
        String span = describeMonths(months);
        String note = allInterns ? " (all of it as internships)" : anyIntern ? " (including internships)" : "";
        return text("Adding up the roles listed in his portfolio, he has about " + span
            + " of professional experience" + note + ". Ask for his work experience to see each role.");
    }

    private ChatMessageResponse currentRole(Knowledge k) {
        if (k.experience().isEmpty()) {
            return text("No work experience is listed in the portfolio yet.");
        }
        LocalDate today = LocalDate.now();
        List<ExperienceResponse> current = new ArrayList<>();
        for (ExperienceResponse e : k.experience()) {
            if (e.endDate() == null || e.endDate().isAfter(today)) {
                current.add(e);
            }
        }
        if (!current.isEmpty()) {
            List<String> lines = new ArrayList<>();
            for (ExperienceResponse e : current) {
                lines.add(e.role() + " at " + e.company());
            }
            return new ChatMessageResponse(ChatResponseType.EXPERIENCE,
                "He currently works as " + list(lines) + ".", current);
        }
        List<ExperienceResponse> sorted = sortedByStart(k.experience());
        ExperienceResponse latest = sorted.get(0);
        return new ChatMessageResponse(ChatResponseType.EXPERIENCE,
            "I don't have a current position on record. His most recent role was " + latest.role() + " at "
                + latest.company() + " (" + range(latest.startDate(), latest.endDate()) + ").",
            List.of(latest));
    }

    // ---- skills -----------------------------------------------------------------------------

    private ChatMessageResponse topSkills(Knowledge k, boolean brief) {
        if (k.skills().isEmpty()) {
            return text("No skills are listed in the portfolio yet.");
        }
        Map<String, Integer> usage = new HashMap<>();
        for (ProjectDetailResponse p : k.projects()) {
            for (SkillResponse s : orEmpty(p.technologies())) {
                usage.merge(lower(s.name()), 1, Integer::sum);
            }
        }
        for (ExperienceResponse e : k.experience()) {
            for (SkillResponse s : orEmpty(e.technologies())) {
                usage.merge(lower(s.name()), 1, Integer::sum);
            }
        }
        List<SkillResponse> ranked = new ArrayList<>(k.skills());
        ranked.sort(Comparator
            .comparingInt((SkillResponse s) -> -usage.getOrDefault(lower(s.name()), 0))
            .thenComparingInt(s -> -proficiencyRank(s.proficiency()))
            .thenComparing(s -> lower(s.name())));
        int limit = brief ? 4 : 6;
        List<SkillResponse> top = new ArrayList<>();
        for (SkillResponse s : ranked) {
            if (top.size() >= limit) {
                break;
            }
            if (usage.getOrDefault(lower(s.name()), 0) > 0 || proficiencyRank(s.proficiency()) >= 4) {
                top.add(s);
            }
        }
        if (top.isEmpty()) {
            top.addAll(ranked.subList(0, Math.min(limit, ranked.size())));
        }
        return new ChatMessageResponse(ChatResponseType.SKILL_LIST,
            "The technologies that show up most in his projects and roles are " + list(names(top)) + ".", top);
    }

    private ChatMessageResponse skillsList(Knowledge k, List<String> categoryHits, Query q) {
        List<SkillResponse> shown = new ArrayList<>();
        Set<String> groups = new LinkedHashSet<>();
        for (SkillResponse s : k.skills()) {
            String category = blank(s.category()) ? "Other" : s.category();
            if (categoryHits.isEmpty() || categoryHits.contains(category)) {
                shown.add(s);
                groups.add(category);
            }
        }
        if (shown.isEmpty()) {
            return text("No skills are listed in the portfolio yet.");
        }
        StringBuilder sb = new StringBuilder("He works with " + shown.size() + " technologies and skills");
        sb.append(categoryHits.isEmpty()
            ? " across " + groups.size() + " areas (" + list(new ArrayList<>(groups)) + ")."
            : " in " + list(new ArrayList<>(groups)) + ".");
        List<SkillResponse> learning = new ArrayList<>();
        for (SkillResponse s : shown) {
            if (s.currentlyLearning()) {
                learning.add(s);
            }
        }
        if (!learning.isEmpty()) {
            sb.append(" Currently learning: ").append(list(names(learning))).append('.');
        }
        return hint(new ChatMessageResponse(ChatResponseType.SKILL_LIST, sb.toString(), shown), q, "skills");
    }

    private ChatMessageResponse skillCheck(List<SkillResponse> skills, Knowledge k) {
        StringBuilder sb = new StringBuilder();
        Map<Long, ProjectDetailResponse> usedIn = new java.util.LinkedHashMap<>();
        for (SkillResponse s : skills) {
            List<ProjectDetailResponse> projects = projectsUsingSkill(k, s);
            List<String> companies = new ArrayList<>();
            for (ExperienceResponse e : k.experience()) {
                if (usesSkill(e.technologies(), s)) {
                    companies.add(e.company());
                }
            }
            // Concepts such as "Authentication" are rarely tagged; also look for them in the written descriptions.
            Set<String> skillStems = new LinkedHashSet<>(Lexicon.contentStems(
                Lexicon.tokens(s.name().replaceAll("\\(.*?\\)", " ")), Set.of()));
            for (ProjectDetailResponse p : k.projects()) {
                if (!projects.contains(p) && mentions(skillStems, p.description(), p.problemSolved())) {
                    projects.add(p);
                }
            }
            for (ExperienceResponse e : k.experience()) {
                if (!companies.contains(e.company())
                    && mentions(skillStems, e.responsibilities(), String.join(" ", orEmpty(e.achievements())))) {
                    companies.add(e.company());
                }
            }
            sb.append("Yes \u2014 ").append(s.name()).append(" is part of his skill set");
            if (!blank(s.category())) {
                sb.append(" (").append(s.category());
                if (!blank(s.proficiency())) {
                    sb.append(", ").append(s.proficiency());
                }
                sb.append(')');
            } else if (!blank(s.proficiency())) {
                sb.append(" (").append(s.proficiency()).append(')');
            }
            sb.append('.');
            if (s.currentlyLearning()) {
                sb.append(" He is currently learning it.");
            }
            if (!projects.isEmpty()) {
                sb.append(" He has used it in ").append(list(projectNames(projects))).append('.');
            }
            if (!companies.isEmpty()) {
                sb.append(" It also appears in his work at ").append(list(companies)).append('.');
            }
            if (projects.isEmpty() && companies.isEmpty() && !s.currentlyLearning()) {
                sb.append(" It isn't tied to a specific project or role yet.");
            }
            sb.append('\n');
            for (ProjectDetailResponse p : projects) {
                usedIn.put(p.id(), p);
            }
        }
        String message = sb.toString().trim();
        if (usedIn.isEmpty()) {
            return text(message);
        }
        return new ChatMessageResponse(ChatResponseType.PROJECT_LIST, message, summaries(new ArrayList<>(usedIn.values())));
    }

    private ChatMessageResponse projectsUsing(List<SkillResponse> skills, Knowledge k) {
        Map<Long, ProjectDetailResponse> found = new java.util.LinkedHashMap<>();
        for (SkillResponse s : skills) {
            for (ProjectDetailResponse p : projectsUsingSkill(k, s)) {
                found.put(p.id(), p);
            }
        }
        String what = list(names(skills));
        boolean asksGithub = false;
        for (SkillResponse s : skills) {
            asksGithub |= s.name() != null && s.name().trim().equalsIgnoreCase("github");
        }
        if (found.isEmpty() && asksGithub) {
            for (ProjectDetailResponse p : k.projects()) {
                if (!blank(p.githubUrl())) {
                    found.put(p.id(), p);
                }
            }
            if (!found.isEmpty()) {
                List<ProjectDetailResponse> hosted = new ArrayList<>(found.values());
                return new ChatMessageResponse(ChatResponseType.PROJECT_LIST,
                    hosted.size() + (hosted.size() == 1 ? " project has" : " projects have")
                        + " a public GitHub repository: " + list(projectNames(hosted)) + ".",
                    summaries(hosted));
            }
        }
        if (found.isEmpty()) {
            return text("I don't see any of his projects tagged with " + what
                + ", although it is part of his skill set.");
        }
        List<ProjectDetailResponse> list = new ArrayList<>(found.values());
        return new ChatMessageResponse(ChatResponseType.PROJECT_LIST,
            list.size() + (list.size() == 1 ? " project uses " : " projects use ") + what + ": "
                + list(projectNames(list)) + ".",
            summaries(list));
    }

    private ChatMessageResponse unknownSkill(String term, Knowledge k) {
        StringBuilder sb = new StringBuilder("I don't see \"" + term + "\" among his skills, projects or roles, "
            + "so I can't confirm it.");
        if (k.skills().isEmpty()) {
            return withContact(k, sb.append(" You can ask him directly.").toString());
        }
        sb.append(" Here is what he does list.");
        return new ChatMessageResponse(ChatResponseType.SKILL_LIST, sb.toString(), k.skills());
    }

    // ---- projects ---------------------------------------------------------------------------

    private ChatMessageResponse projectsList(Knowledge k, boolean brief) {
        List<ProjectDetailResponse> projects = k.projects();
        if (projects.isEmpty()) {
            return text("No projects are listed in the portfolio yet.");
        }
        List<String> labels = new ArrayList<>();
        for (ProjectDetailResponse p : projects) {
            labels.add(blank(p.category()) ? p.name() : p.name() + " (" + p.category() + ")");
        }
        List<String> shown = brief && labels.size() > 4 ? labels.subList(0, 4) : labels;
        String message = "He has " + projects.size() + (projects.size() == 1 ? " project" : " projects") + ": "
            + list(shown) + (shown.size() < labels.size() ? " and more" : "")
            + ". Ask about any of them by name for the details.";
        return new ChatMessageResponse(ChatResponseType.PROJECT_LIST, message, summaries(projects));
    }

    private ChatMessageResponse latestProject(Knowledge k) {
        if (k.projects().isEmpty()) {
            return text("No projects are listed in the portfolio yet.");
        }
        ProjectDetailResponse latest = k.projects().get(0);
        for (ProjectDetailResponse p : k.projects()) {
            if (isNewer(p, latest)) {
                latest = p;
            }
        }
        return new ChatMessageResponse(ChatResponseType.PROJECT,
            "His most recently added project is " + latest.name() + ". " + nz(latest.description()), latest);
    }

    private boolean isNewer(ProjectDetailResponse a, ProjectDetailResponse b) {
        if (a.createdAt() != null && b.createdAt() != null && !a.createdAt().equals(b.createdAt())) {
            return a.createdAt().isAfter(b.createdAt());
        }
        return a.id() != null && b.id() != null && a.id() > b.id();
    }

    private ChatMessageResponse projectAnswer(List<ProjectDetailResponse> projects, List<SkillResponse> skills,
                                              Query q, Knowledge k, boolean brief, boolean detailed) {
        if (projects.size() > 1) {
            List<String> lines = new ArrayList<>();
            for (ProjectDetailResponse p : projects) {
                lines.add("- " + p.name() + ": " + firstSentence(p.description()));
            }
            return new ChatMessageResponse(ChatResponseType.PROJECT_LIST,
                "Here are the projects you mentioned:\n" + String.join("\n", lines)
                    + "\nAsk about one of them for the full details.",
                summaries(projects));
        }
        ProjectDetailResponse p = projects.get(0);
        List<String> tech = names(orEmpty(p.technologies()));
        boolean asksTech = q.any("technology", "tech", "stack", "framework", "language", "tool", "library", "use")
            || q.phrase("built with", "built using", "made with", "written in", "developed with");
        boolean asksChallenges = q.any("challenge", "difficult", "hard", "lesson", "obstacle", "struggle")
            || (q.any("problem") && !q.any("solve"));
        boolean asksProblem = q.any("solve", "purpose", "why") || (q.any("problem") && q.any("solve"));
        boolean asksLink = q.any("link", "url", "live", "demo", "repo", "repository", "source", "visit", "deploy")
            || (q.any("github", "code") && !q.any("use"));

        StringBuilder sb = new StringBuilder();
        if (!skills.isEmpty() && !asksLink) {
            for (SkillResponse s : skills) {
                boolean uses = usesSkill(p.technologies(), s);
                sb.append(uses
                    ? "Yes \u2014 " + p.name() + " uses " + s.name() + ".\n"
                    : "I don't see " + s.name() + " listed for " + p.name() + ".\n");
            }
            if (!tech.isEmpty()) {
                sb.append("It is built with ").append(list(tech)).append('.');
            }
        } else if (asksLink) {
            appendLinks(sb, p);
        } else if (asksChallenges) {
            appendChallenges(sb, p);
        } else if (asksProblem && !blank(p.problemSolved())) {
            sb.append(p.name()).append(" solves this: ").append(p.problemSolved().trim());
        } else if (asksTech) {
            sb.append(tech.isEmpty()
                ? "No technologies are listed for " + p.name() + " yet."
                : p.name() + " is built with " + list(tech) + ".");
        } else {
            sb.append(p.name());
            if (!blank(p.category())) {
                sb.append(" \u2014 ").append(p.category());
            }
            sb.append(".\n").append(nz(p.description()).trim());
            if (!blank(p.problemSolved())) {
                sb.append("\nProblem it solves: ").append(p.problemSolved().trim());
            }
            if (!tech.isEmpty()) {
                sb.append("\nBuilt with: ").append(list(tech)).append('.');
            }
            if (detailed) {
                appendChallenges(sb, p);
            }
            if (!brief && (!blank(p.liveUrl()) || !blank(p.githubUrl()))) {
                appendLinks(sb, p);
            }
        }
        return new ChatMessageResponse(ChatResponseType.PROJECT, sb.toString().trim(), p);
    }

    private void appendLinks(StringBuilder sb, ProjectDetailResponse p) {
        boolean live = !blank(p.liveUrl());
        boolean repo = !blank(p.githubUrl());
        if (!live && !repo) {
            sb.append("\nNo live link or repository is published for ").append(p.name()).append(" yet.");
            return;
        }
        if (live) {
            sb.append("\nLive: ").append(p.liveUrl().trim());
        }
        if (repo) {
            sb.append("\nSource: ").append(p.githubUrl().trim());
        }
    }

    private void appendChallenges(StringBuilder sb, ProjectDetailResponse p) {
        List<ProjectChallengeResponse> challenges = orEmpty(p.challenges());
        if (challenges.isEmpty()) {
            sb.append("\nNo challenges have been recorded for ").append(p.name()).append(" yet.");
            return;
        }
        sb.append("\nChallenges on ").append(p.name()).append(':');
        for (ProjectChallengeResponse c : challenges) {
            sb.append("\n- ").append(nz(c.challenge()).trim());
            if (!blank(c.lessonLearned())) {
                sb.append(" Lesson: ").append(c.lessonLearned().trim());
            }
        }
    }

    // ---- experience, education, certifications, achievements --------------------------------

    private ChatMessageResponse experienceList(Knowledge k, boolean brief) {
        if (k.experience().isEmpty()) {
            return text("No work experience is listed in the portfolio yet.");
        }
        List<ExperienceResponse> sorted = sortedByStart(k.experience());
        List<String> lines = new ArrayList<>();
        for (ExperienceResponse e : brief && sorted.size() > 3 ? sorted.subList(0, 3) : sorted) {
            lines.add("- " + e.role() + " at " + e.company() + " (" + range(e.startDate(), e.endDate()) + ")");
        }
        String message = "His work experience, most recent first:\n" + String.join("\n", lines)
            + "\nAsk about a company by name for the details.";
        return new ChatMessageResponse(ChatResponseType.EXPERIENCE, message, sorted);
    }

    private ChatMessageResponse companyAnswer(List<ExperienceResponse> jobs, Query q, Knowledge k, boolean brief) {
        if (jobs.size() > 1) {
            List<String> lines = new ArrayList<>();
            for (ExperienceResponse e : jobs) {
                lines.add("- " + e.role() + " at " + e.company() + " (" + range(e.startDate(), e.endDate()) + ")");
            }
            return new ChatMessageResponse(ChatResponseType.EXPERIENCE,
                "Here are the roles you mentioned:\n" + String.join("\n", lines), jobs);
        }
        ExperienceResponse e = jobs.get(0);
        StringBuilder sb = new StringBuilder();
        List<String> tech = names(orEmpty(e.technologies()));
        if (q.any("technology", "tech", "stack", "tool", "use", "language", "framework")) {
            sb.append(tech.isEmpty()
                ? "No technologies are listed for his role at " + e.company() + "."
                : "At " + e.company() + " he used " + list(tech) + ".");
        } else if (q.any("achievement", "accomplishment", "accomplish", "achieve", "impact", "result")) {
            List<String> wins = orEmpty(e.achievements());
            sb.append(wins.isEmpty()
                ? "No specific achievements are recorded for his role at " + e.company() + "."
                : "At " + e.company() + ": " + String.join("; ", wins));
        } else {
            sb.append(e.role()).append(" at ").append(e.company()).append(" (")
                .append(range(e.startDate(), e.endDate())).append(").");
            List<String> duties = lines(e.responsibilities());
            for (String duty : brief && duties.size() > 2 ? duties.subList(0, 2) : duties) {
                sb.append("\n- ").append(duty);
            }
            if (!tech.isEmpty()) {
                sb.append("\nTechnologies: ").append(list(tech)).append('.');
            }
            if (!orEmpty(e.achievements()).isEmpty()) {
                sb.append("\nAchievements: ").append(String.join("; ", e.achievements())).append('.');
            }
        }
        return new ChatMessageResponse(ChatResponseType.EXPERIENCE, sb.toString().trim(), List.of(e));
    }

    private ChatMessageResponse educationAnswer(List<EducationResponse> list, Knowledge k) {
        if (list.isEmpty()) {
            return text("No education details are listed in the portfolio yet.");
        }
        List<String> lines = new ArrayList<>();
        for (EducationResponse e : list) {
            StringBuilder sb = new StringBuilder("- ");
            if (!blank(e.degree())) {
                sb.append(e.degree().trim());
            }
            if (!blank(e.course())) {
                sb.append(blank(e.degree()) ? "" : " in ").append(e.course().trim());
            }
            if (!blank(e.institution())) {
                sb.append(sb.length() > 2 ? ", " : "").append(e.institution().trim());
            }
            if (e.startDate() != null) {
                sb.append(" (").append(range(e.startDate(), e.endDate())).append(')');
            }
            if (!blank(e.details())) {
                sb.append(" \u2014 ").append(e.details().trim());
            }
            lines.add(sb.toString());
        }
        return text("His education:\n" + String.join("\n", lines));
    }

    private ChatMessageResponse certificationAnswer(List<CertificationResponse> list, Knowledge k) {
        if (list.isEmpty()) {
            return text("No certifications are listed in the portfolio yet.");
        }
        List<String> lines = new ArrayList<>();
        for (CertificationResponse c : list) {
            StringBuilder sb = new StringBuilder("- ").append(nz(c.name()).trim());
            if (!blank(c.provider())) {
                sb.append(" \u2014 ").append(c.provider().trim());
            }
            if (c.issuedDate() != null) {
                sb.append(" (").append(MONTH_YEAR.format(c.issuedDate())).append(')');
            }
            if (!blank(c.credentialUrl())) {
                sb.append(" ").append(c.credentialUrl().trim());
            }
            lines.add(sb.toString());
        }
        return text((list.size() == 1 ? "His certification:\n" : "His certifications:\n") + String.join("\n", lines));
    }

    private ChatMessageResponse achievementAnswer(Knowledge k) {
        if (k.achievements().isEmpty()) {
            return text("No achievements are listed in the portfolio yet.");
        }
        List<String> lines = new ArrayList<>();
        for (AchievementResponse a : k.achievements()) {
            StringBuilder sb = new StringBuilder("- ").append(nz(a.title()).trim());
            if (!blank(a.description())) {
                sb.append(": ").append(a.description().trim());
            }
            lines.add(sb.toString());
        }
        return text("His achievements:\n" + String.join("\n", lines));
    }

    private ChatMessageResponse profileAnswer(Knowledge k, boolean brief) {
        ProfileResponse p = k.profile();
        if (p == null) {
            return text("The profile hasn't been filled in yet. " + helpText(k));
        }
        StringBuilder sb = new StringBuilder(nz(p.fullName()).trim());
        if (!blank(p.title())) {
            sb.append(" is ").append(article(p.title())).append(' ').append(p.title().trim());
            if (!blank(p.location())) {
                sb.append(" based in ").append(p.location().trim());
            }
            sb.append('.');
        } else if (!blank(p.location())) {
            sb.append(" is based in ").append(p.location().trim()).append('.');
        }
        if (!blank(p.bio())) {
            sb.append("\n").append(p.bio().trim());
        }
        if (!brief && !blank(p.interests())) {
            sb.append("\nInterests: ").append(p.interests().trim());
        }
        if (!blank(p.careerGoals())) {
            sb.append("\nCareer goals: ").append(p.careerGoals().trim());
        }
        return text(sb.toString().trim());
    }

    // ---- search fallback --------------------------------------------------------------------

    private ChatMessageResponse search(Query q, Knowledge k, String mode) {
        List<String> base = new ArrayList<>();
        for (String term : q.terms) {
            if (!GENERIC_TERMS.contains(term) && !base.contains(term)) {
                base.add(term);
            }
        }
        return searchTerms(q, k, base, false, mode);
    }

    /**
     * BM25 over the portfolio text. First with the exact words, then with their
     * synonyms. {@code strict} demands that the best passage contains every word.
     */
    private ChatMessageResponse searchTerms(Query q, Knowledge k, List<String> base, boolean strict, String mode) {
        if (base.isEmpty()) {
            return null;
        }
        Bm25Index index = k.index();
        for (boolean expand : new boolean[] {false, true}) {
            if (expand && strict) {
                break;
            }
            List<Bm25Index.Hit> hits = index.search(expand ? Lexicon.withSynonyms(base) : base);
            if (hits.isEmpty()) {
                continue;
            }
            Bm25Index.Hit top = hits.get(0);
            int covered = 0;
            for (String term : base) {
                if (index.has(top.doc(), term)) {
                    covered++;
                }
            }
            int distinct = base.size();
            boolean enough = strict
                ? covered == distinct
                : covered >= 1 && (double) covered / distinct >= 0.5 && (distinct < 3 || covered >= 2);
            if (enough && top.score() >= MIN_SEARCH_SCORE) {
                return render(q, k, hits, mode);
            }
        }
        return null;
    }

    private ChatMessageResponse render(Query q, Knowledge k, List<Bm25Index.Hit> hits, String mode) {
        Bm25Index.Hit top = hits.get(0);
        boolean brief = "RECRUITER".equalsIgnoreCase(mode);
        boolean detailed = "DEVELOPER".equalsIgnoreCase(mode);
        Knowledge.Passage passage = k.passages().get(top.doc());
        Object ref = passage.ref();
        switch (passage.kind()) {
            case "faq":
                return text(((FaqResponse) ref).answer());
            case "profile":
                return profileAnswer(k, brief);
            case "skills": {
                Knowledge.SkillGroup group = (Knowledge.SkillGroup) ref;
                return new ChatMessageResponse(ChatResponseType.SKILL_LIST,
                    "In " + group.category() + ", he works with " + list(names(group.skills())) + ".",
                    group.skills());
            }
            case "project": {
                List<ProjectDetailResponse> related = new ArrayList<>();
                related.add((ProjectDetailResponse) ref);
                for (int i = 1; i < hits.size() && i < 3; i++) {
                    Knowledge.Passage other = k.passages().get(hits.get(i).doc());
                    if ("project".equals(other.kind()) && hits.get(i).score() >= 0.85 * top.score()) {
                        related.add((ProjectDetailResponse) other.ref());
                    }
                }
                if (related.size() > 1) {
                    return new ChatMessageResponse(ChatResponseType.PROJECT_LIST,
                        "These projects match: " + list(projectNames(related)) + ".", summaries(related));
                }
                return projectAnswer(related, List.of(), q, k, brief, detailed);
            }
            case "experience":
                return companyAnswer(List.of((ExperienceResponse) ref), q, k, brief);
            case "education":
                return educationAnswer(List.of((EducationResponse) ref), k);
            case "certification":
                return certificationAnswer(List.of((CertificationResponse) ref), k);
            case "achievement": {
                AchievementResponse a = (AchievementResponse) ref;
                return text(nz(a.title()).trim() + (blank(a.description()) ? "" : ": " + a.description().trim()));
            }
            default:
                return null;
        }
    }

    private ChatMessageResponse notFound(Query q, Knowledge k) {
        String message = "I couldn't find that in " + possessive(k) + " portfolio. I can tell you about his skills, "
            + "projects, work experience, education, certifications and achievements, or share his r\u00e9sum\u00e9. "
            + "If you'd like to ask him directly, here is how to reach him.";
        return withContact(k, message);
    }

    // ---- helpers ----------------------------------------------------------------------------

    /** Mentions other topics the visitor asked about in the same message. */
    private ChatMessageResponse hint(ChatMessageResponse reply, Query q, String handled) {
        List<String> others = new ArrayList<>();
        if (!handled.equals("skills") && q.any("skill", "technology", "tech", "stack")) {
            others.add("his skills");
        }
        if (!handled.equals("projects") && q.any("project")) {
            others.add("his projects");
        }
        if (!handled.equals("experience") && q.any("experience", "job", "career", "company")) {
            others.add("his work experience");
        }
        if (!handled.equals("education") && q.any("education", "degree", "university", "school")) {
            others.add("his education");
        }
        if (!handled.equals("certifications") && q.any("certificate")) {
            others.add("his certifications");
        }
        if (others.isEmpty()) {
            return reply;
        }
        return new ChatMessageResponse(reply.type(),
            reply.message() + "\n\nYou also mentioned " + list(others) + " \u2014 just ask and I'll cover that next.",
            reply.payload());
    }

    private ChatMessageResponse withContact(Knowledge k, String message) {
        if (k.contact() != null) {
            return new ChatMessageResponse(ChatResponseType.CONTACT, message, k.contact());
        }
        return text(message);
    }

    private static ChatMessageResponse text(String message) {
        return new ChatMessageResponse(ChatResponseType.TEXT, message, null);
    }

    private static String owner(Knowledge k) {
        ProfileResponse p = k.profile();
        return p == null || blank(p.fullName()) ? "him" : p.fullName().trim();
    }

    private static String possessive(Knowledge k) {
        ProfileResponse p = k.profile();
        return p == null || blank(p.fullName()) ? "his" : p.fullName().trim() + "'s";
    }

    private static List<ProjectDetailResponse> projectsUsingSkill(Knowledge k, SkillResponse s) {
        List<ProjectDetailResponse> out = new ArrayList<>();
        for (ProjectDetailResponse p : k.projects()) {
            if (usesSkill(p.technologies(), s)) {
                out.add(p);
            }
        }
        return out;
    }

    private static boolean mentions(Set<String> skillStems, String... texts) {
        if (skillStems.isEmpty()) {
            return false;
        }
        Set<String> present = new HashSet<>();
        for (String text : texts) {
            present.addAll(Lexicon.contentStems(Lexicon.tokens(text), Set.of()));
        }
        return present.containsAll(skillStems);
    }

    private static boolean usesSkill(List<SkillResponse> technologies, SkillResponse skill) {
        for (SkillResponse t : orEmpty(technologies)) {
            if ((t.id() != null && t.id().equals(skill.id())) || lower(t.name()).equals(lower(skill.name()))) {
                return true;
            }
        }
        return false;
    }

    private static List<ProjectSummaryResponse> summaries(List<ProjectDetailResponse> projects) {
        List<ProjectSummaryResponse> out = new ArrayList<>();
        for (ProjectDetailResponse p : projects) {
            out.add(new ProjectSummaryResponse(p.id(), p.name(), p.slug(), p.description(), p.category(),
                p.githubUrl(), p.liveUrl()));
        }
        return out;
    }

    private static List<String> names(List<SkillResponse> skills) {
        List<String> out = new ArrayList<>();
        for (SkillResponse s : skills) {
            out.add(s.name());
        }
        return out;
    }

    private static List<String> projectNames(List<ProjectDetailResponse> projects) {
        List<String> out = new ArrayList<>();
        for (ProjectDetailResponse p : projects) {
            out.add(p.name());
        }
        return out;
    }

    private static List<ExperienceResponse> sortedByStart(List<ExperienceResponse> jobs) {
        List<ExperienceResponse> sorted = new ArrayList<>(jobs);
        sorted.sort(Comparator.comparing(ExperienceResponse::startDate,
            Comparator.nullsFirst(Comparator.<LocalDate>naturalOrder())).reversed());
        return sorted;
    }

    private static List<String> lines(String text) {
        List<String> out = new ArrayList<>();
        if (text == null) {
            return out;
        }
        for (String line : text.split("\\r?\\n")) {
            String trimmed = line.trim().replaceFirst("^[-\u2022*]\\s*", "");
            if (!trimmed.isEmpty()) {
                out.add(trimmed);
            }
        }
        return out;
    }

    private static String firstSentence(String text) {
        if (blank(text)) {
            return "";
        }
        String t = text.trim();
        int dot = t.indexOf(". ");
        return dot > 0 ? t.substring(0, dot + 1) : t;
    }

    private static String range(LocalDate start, LocalDate end) {
        String from = start == null ? "" : MONTH_YEAR.format(start);
        String to = end == null || end.isAfter(LocalDate.now()) ? "Present" : MONTH_YEAR.format(end);
        return from.isEmpty() ? to : from + " \u2013 " + to;
    }

    private static long totalMonths(List<ExperienceResponse> jobs) {
        LocalDate today = LocalDate.now();
        List<LocalDate[]> intervals = new ArrayList<>();
        for (ExperienceResponse e : jobs) {
            if (e.startDate() == null) {
                continue;
            }
            LocalDate end = e.endDate() == null || e.endDate().isAfter(today) ? today : e.endDate();
            if (!end.isBefore(e.startDate())) {
                intervals.add(new LocalDate[] {e.startDate(), end});
            }
        }
        intervals.sort(Comparator.comparing((LocalDate[] a) -> a[0]));
        long total = 0;
        LocalDate curStart = null;
        LocalDate curEnd = null;
        for (LocalDate[] a : intervals) {
            if (curStart == null) {
                curStart = a[0];
                curEnd = a[1];
            } else if (!a[0].isAfter(curEnd)) {
                if (a[1].isAfter(curEnd)) {
                    curEnd = a[1];
                }
            } else {
                total += ChronoUnit.MONTHS.between(curStart, curEnd);
                curStart = a[0];
                curEnd = a[1];
            }
        }
        if (curStart != null) {
            total += ChronoUnit.MONTHS.between(curStart, curEnd);
        }
        return total;
    }

    private static String describeMonths(long months) {
        if (months < 1) {
            return "less than a month";
        }
        long years = months / 12;
        long rest = months % 12;
        String y = years + (years == 1 ? " year" : " years");
        String m = rest + (rest == 1 ? " month" : " months");
        if (years == 0) {
            return m;
        }
        return rest == 0 ? y : y + " and " + m;
    }

    private static int proficiencyRank(String proficiency) {
        if (blank(proficiency)) {
            return 0;
        }
        String p = proficiency.toLowerCase(Locale.ROOT);
        if (p.contains("expert") || p.contains("master")) {
            return 5;
        }
        if (p.contains("advanced") || p.contains("senior") || p.contains("strong")) {
            return 4;
        }
        if (p.contains("proficient") || p.contains("intermediate") || p.contains("competent")) {
            return 3;
        }
        if (p.contains("beginner") || p.contains("basic") || p.contains("novice")) {
            return 1;
        }
        return 2;
    }

    private static String article(String word) {
        String w = word.trim().toLowerCase(Locale.ROOT);
        return !w.isEmpty() && "aeiou".indexOf(w.charAt(0)) >= 0 ? "an" : "a";
    }

    private static String list(List<String> items) {
        if (items.isEmpty()) {
            return "";
        }
        if (items.size() == 1) {
            return items.get(0);
        }
        return String.join(", ", items.subList(0, items.size() - 1)) + " and " + items.get(items.size() - 1);
    }

    private static String lower(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT).trim();
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    private static <T> List<T> orEmpty(List<T> list) {
        return list == null ? List.of() : list;
    }
}
