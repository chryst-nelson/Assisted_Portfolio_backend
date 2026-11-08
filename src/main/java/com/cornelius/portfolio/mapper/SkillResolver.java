package com.cornelius.portfolio.mapper;

import com.cornelius.portfolio.entity.Skill;
import com.cornelius.portfolio.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Component
@RequiredArgsConstructor
public class SkillResolver {

    private final SkillRepository skillRepository;

    public Set<Skill> resolve(List<Long> skillIds) {
        if (skillIds == null || skillIds.isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(skillRepository.findAllById(skillIds));
    }
}
