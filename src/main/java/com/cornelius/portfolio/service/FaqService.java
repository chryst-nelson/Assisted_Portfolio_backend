package com.cornelius.portfolio.service;

import com.cornelius.portfolio.ai.Lexicon;
import com.cornelius.portfolio.ai.TextNormalizer;
import com.cornelius.portfolio.dto.request.FaqRequest;
import com.cornelius.portfolio.dto.response.FaqResponse;
import com.cornelius.portfolio.entity.FaqEntry;
import com.cornelius.portfolio.exception.NotFoundException;
import com.cornelius.portfolio.repository.FaqRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FaqService {

    // Share of meaningful words two questions must have in common to count as the same question.
    private static final double MATCH_THRESHOLD = 0.7;

    private final FaqRepository faqRepository;

    @Transactional(readOnly = true)
    public List<FaqResponse> getAllForAdmin() {
        return faqRepository.findAllByOrderByIdAsc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public FaqResponse create(FaqRequest request) {
        FaqEntry entry = FaqEntry.builder()
            .question(request.question().trim())
            .answer(request.answer().trim())
            .keywords(blankToNull(request.keywords()))
            .enabled(request.enabled())
            .build();
        return toResponse(faqRepository.save(entry));
    }

    @Transactional
    public FaqResponse update(Long id, FaqRequest request) {
        FaqEntry entry = faqRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("No FAQ entry found with id " + id));
        entry.setQuestion(request.question().trim());
        entry.setAnswer(request.answer().trim());
        entry.setKeywords(blankToNull(request.keywords()));
        entry.setEnabled(request.enabled());
        return toResponse(faqRepository.save(entry));
    }

    @Transactional
    public void delete(Long id) {
        if (!faqRepository.existsById(id)) {
            throw new NotFoundException("No FAQ entry found with id " + id);
        }
        faqRepository.deleteById(id);
    }

    /**
     * Best enabled FAQ answer for a visitor's message, if one is close enough.
     * A keyword phrase found in the message wins outright; otherwise the entry
     * whose question shares enough meaningful words with the message.
     */
    @Transactional(readOnly = true)
    public Optional<String> findAnswer(String message) {
        String normalized = TextNormalizer.normalize(message);
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        String padded = " " + normalized + " ";
        Set<String> messageTokens = stems(message);

        FaqEntry best = null;
        double bestScore = 0;
        for (FaqEntry entry : faqRepository.findByEnabledTrue()) {
            double score = score(entry, padded, messageTokens);
            if (score > bestScore) {
                best = entry;
                bestScore = score;
            }
        }
        if (best == null || bestScore < MATCH_THRESHOLD) {
            return Optional.empty();
        }
        return Optional.of(best.getAnswer());
    }

    private double score(FaqEntry entry, String paddedMessage, Set<String> messageTokens) {
        if (entry.getKeywords() != null) {
            for (String keyword : entry.getKeywords().split(",")) {
                String normalizedKeyword = TextNormalizer.normalize(keyword);
                if (!normalizedKeyword.isEmpty() && paddedMessage.contains(" " + normalizedKeyword + " ")) {
                    return 1.0;
                }
            }
        }
        Set<String> questionTokens = stems(entry.getQuestion());
        if (questionTokens.isEmpty() || messageTokens.isEmpty()) {
            return 0;
        }
        Set<String> common = new HashSet<>(questionTokens);
        common.retainAll(messageTokens);
        Set<String> union = new HashSet<>(questionTokens);
        union.addAll(messageTokens);
        double jaccard = (double) common.size() / union.size();
        // A longer message that still contains (nearly) all of the FAQ question's words is the same question.
        double coverage = (double) common.size() / questionTokens.size();
        double contained = common.size() >= 2 && coverage >= 0.8 ? 0.85 * coverage : 0;
        return Math.max(jaccard, contained);
    }

    private static Set<String> stems(String text) {
        return new java.util.LinkedHashSet<>(Lexicon.contentStems(Lexicon.tokens(text), Set.of()));
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private FaqResponse toResponse(FaqEntry e) {
        return new FaqResponse(e.getId(), e.getQuestion(), e.getAnswer(), e.getKeywords(), e.isEnabled());
    }
}
