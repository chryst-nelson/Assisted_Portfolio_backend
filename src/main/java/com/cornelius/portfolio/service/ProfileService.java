package com.cornelius.portfolio.service;

import com.cornelius.portfolio.dto.request.ProfileUpdateRequest;
import com.cornelius.portfolio.dto.response.ProfileResponse;
import com.cornelius.portfolio.entity.Profile;
import com.cornelius.portfolio.exception.NotFoundException;
import com.cornelius.portfolio.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;

    @Transactional(readOnly = true)
    public ProfileResponse getProfile() {
        Profile profile = profileRepository.findTopByOrderByIdAsc()
            .orElseThrow(() -> new NotFoundException("Profile has not been configured yet."));
        return toResponse(profile);
    }

    // Upsert: creates the single profile row on first save, updates it thereafter.
    @Transactional
    public ProfileResponse updateProfile(ProfileUpdateRequest request) {
        Profile profile = profileRepository.findTopByOrderByIdAsc().orElseGet(Profile::new);

        profile.setFullName(request.fullName());
        profile.setTitle(request.title());
        profile.setBio(request.bio());
        profile.setLocation(request.location());
        profile.setInterests(request.interests());
        profile.setCareerGoals(request.careerGoals());
        profile.setUpdatedAt(LocalDateTime.now());

        return toResponse(profileRepository.save(profile));
    }

    private ProfileResponse toResponse(Profile profile) {
        return new ProfileResponse(
            profile.getId(),
            profile.getFullName(),
            profile.getTitle(),
            profile.getBio(),
            profile.getLocation(),
            profile.getInterests(),
            profile.getCareerGoals(),
            profile.getUpdatedAt()
        );
    }
}
