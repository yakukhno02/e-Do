package com.edo.dto.response;

import java.util.UUID;

public class RegisterResponse {

    private UUID userId;
    private String token;
    private UserSummaryResponse user;

    public RegisterResponse() {
    }

    public RegisterResponse(UUID userId, String token, UserSummaryResponse user) {
        this.userId = userId;
        this.token = token;
        this.user = user;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public UserSummaryResponse getUser() {
        return user;
    }

    public void setUser(UserSummaryResponse user) {
        this.user = user;
    }
}
