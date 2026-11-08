package com.cornelius.portfolio.mapper;

import com.cornelius.portfolio.dto.request.ExperienceRequest;
import com.cornelius.portfolio.dto.response.ExperienceResponse;
import com.cornelius.portfolio.entity.Experience;
import com.cornelius.portfolio.entity.ExperienceAchievement;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring", uses = { SkillMapper.class, SkillResolver.class })
public interface ExperienceMapper {

    @Mapping(target = "achievements", source = "achievements", qualifiedByName = "achievementsToStrings")
    ExperienceResponse toResponse(Experience experience);

    List<ExperienceResponse> toResponseList(List<Experience> experiences);


    @Mapping(target = "id", ignore = true)
    @Mapping(target = "technologies", source = "technologyIds")
    @Mapping(target = "achievements", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Experience toEntity(ExperienceRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "technologies", source = "technologyIds")
    @Mapping(target = "achievements", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntityFromRequest(ExperienceRequest request, @MappingTarget Experience experience);

    @Named("achievementsToStrings")
    default List<String> achievementsToStrings(List<ExperienceAchievement> achievements) {
        return achievements.stream().map(ExperienceAchievement::getAchievement).toList();
    }
}
