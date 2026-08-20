package com.cornelius.portfolio.mapper;

import com.cornelius.portfolio.dto.request.SkillRequest;
import com.cornelius.portfolio.dto.response.SkillResponse;
import com.cornelius.portfolio.entity.Skill;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SkillMapper {

    SkillResponse toResponse(Skill skill);

    List<SkillResponse> toResponseList(List<Skill> skills);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Skill toEntity(SkillRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntityFromRequest(SkillRequest request, @MappingTarget Skill skill);
}
