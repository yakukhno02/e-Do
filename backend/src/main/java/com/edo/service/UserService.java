package com.edo.service;

import com.edo.dto.request.UpdateProfileRequest;
import com.edo.dto.response.UserProfileResponse;

import java.util.UUID;

public interface UserService {
    UserProfileResponse getProfile(UUID userId);
    UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request);
}
