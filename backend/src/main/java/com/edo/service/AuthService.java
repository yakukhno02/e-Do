package com.edo.service;

import com.edo.dto.request.ForgotPasswordRequest;
import com.edo.dto.request.LoginRequest;
import com.edo.dto.request.RegisterRequest;
import com.edo.dto.request.ResetPasswordRequest;
import com.edo.dto.response.AuthResponse;
import com.edo.dto.response.MessageResponse;
import com.edo.dto.response.RegisterResponse;

public interface AuthService {
    RegisterResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    MessageResponse forgotPassword(ForgotPasswordRequest request);
    void resetPassword(ResetPasswordRequest request);
}
