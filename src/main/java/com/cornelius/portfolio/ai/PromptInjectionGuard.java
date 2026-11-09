package com.cornelius.portfolio.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;


@Component
@Slf4j
public class PromptInjectionGuard {

    private static final List<Pattern> SUSPICIOUS_PATTERNS = List.of(
        Pattern.compile("ignore (all )?(previous|prior|above) instructions", Pattern.CASE_INSENSITIVE),
        Pattern.compile("disregard (your|the) (guidelines|instructions|rules)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("reveal (your|the) (system prompt|instructions)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("what (is|are) your (system prompt|instructions)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("you are now", Pattern.CASE_INSENSITIVE),
        Pattern.compile("pretend (you are|to be)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("act as if you (have no|are not)", Pattern.CASE_INSENSITIVE)
    );

    public void check(String message) {
        isSuspicious(message);
    }

    /** True (and logged) when the message looks like an attempt to override the assistant's rules. */
    public boolean isSuspicious(String message) {
        for (Pattern pattern : SUSPICIOUS_PATTERNS) {
            if (pattern.matcher(message).find()) {
                log.warn("Message matched a prompt-injection pattern ({}): {}", pattern.pattern(), message);
                return true; // one match is enough to log; no need to check the rest
            }
        }
        return false;
    }
}
