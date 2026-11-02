package com.edo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ResetPasswordRequest {

    @NotBlank(message = "Token is required")
    private String token;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters")
    private String password;

    @NotBlank(message = "Password confirmation is required")
    private String passwordConfirm;

    public ResetPasswordRequest() {
    }

    public ResetPasswordRequest(String token, String password, String passwordConfirm) {
        this.token = token;
        this.password = password;
        this.passwordConfirm = passwordConfirm;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPasswordConfirm() {
        return passwordConfirm;
    }

    public void setPasswordConfirm(String passwordConfirm) {
        this.passwordConfirm = passwordConfirm;
    }
}
