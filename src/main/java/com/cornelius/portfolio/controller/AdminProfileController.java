package com.cornelius.portfolio.controller;

import com.cornelius.portfolio.dto.request.ProfileUpdateRequest;
import com.cornelius.portfolio.dto.response.ProfileResponse;
import com.cornelius.portfolio.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/profile")
@RequiredArgsConstructor
public class AdminProfileController {

    private final ProfileService profileService;

    @PutMapping
    public ProfileResponse update(@Valid @RequestBody ProfileUpdateRequest request) {
        return profileService.updateProfile(request);
    }
}
