package com.fibiyo.ecommerce.application.service;

import com.fibiyo.ecommerce.application.dto.JwtAuthenticationResponse;
import com.fibiyo.ecommerce.application.dto.LoginRequest;
import com.fibiyo.ecommerce.application.dto.RegisterRequest;
import com.fibiyo.ecommerce.application.dto.ResetPasswordRequest;
import com.fibiyo.ecommerce.domain.entity.User; // Opsiyonel, belki User yerine UserResponse dönmek daha iyi

public interface AuthService {

    JwtAuthenticationResponse authenticateUser(LoginRequest loginRequest);

    User registerUser(RegisterRequest registerRequest); // Veya UserResponse dönülebilir

    
    void processForgotPassword(String email); // E-postayı parametre olarak alalım
    void resetPassword(ResetPasswordRequest request);
    // TODO: Sosyal medya login metodu eklenecek
    // JwtAuthenticationResponse socialLogin(SocialLoginRequest socialLoginRequest);

    // TODO: Logout işlemi genellikle client-side token silme ile olur ama gerekirse server-side invalidation metodu eklenebilir
}