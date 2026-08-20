package com.cornelius.portfolio.service;

import com.cornelius.portfolio.dto.request.SkillRequest;
import com.cornelius.portfolio.dto.response.SkillResponse;
import com.cornelius.portfolio.entity.Skill;
import com.cornelius.portfolio.exception.NotFoundException;
import com.cornelius.portfolio.mapper.SkillMapper;
import com.cornelius.portfolio.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SkillService {

    private final SkillRepository skillRepository;
    private final SkillMapper skillMapper;

    @Transactional(readOnly = true)
    public List<SkillResponse> getSkills(String category) {
        var skills = StringUtils.hasText(category)
            ? skillRepository.findByCategoryIgnoreCase(category)
            : skillRepository.findAll();

        return skillMapper.toResponseList(skills);
    }

    @Transactional
    public SkillResponse createSkill(SkillRequest request) {
        Skill skill = skillMapper.toEntity(request);
        skill.setCreatedAt(LocalDateTime.now());
        return skillMapper.toResponse(skillRepository.save(skill));
    }

    @Transactional
    public SkillResponse updateSkill(Long id, SkillRequest request) {
        Skill skill = skillRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("No skill found with id " + id));
        skillMapper.updateEntityFromRequest(request, skill);
        return skillMapper.toResponse(skillRepository.save(skill));
    }

    @Transactional
    public void deleteSkill(Long id) {
        if (!skillRepository.existsById(id)) {
            throw new NotFoundException("No skill found with id " + id);
        }
        skillRepository.deleteById(id);
    }
}
