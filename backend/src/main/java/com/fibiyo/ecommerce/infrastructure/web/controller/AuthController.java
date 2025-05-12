package com.fibiyo.ecommerce.infrastructure.web.controller;

import com.fibiyo.ecommerce.application.dto.ApiResponse;
import com.fibiyo.ecommerce.application.dto.ForgotPasswordRequest;
import com.fibiyo.ecommerce.application.dto.JwtAuthenticationResponse;
import com.fibiyo.ecommerce.application.dto.LoginRequest;
import com.fibiyo.ecommerce.application.dto.RegisterRequest;
import com.fibiyo.ecommerce.application.dto.ResetPasswordRequest;
import com.fibiyo.ecommerce.application.service.AuthService;
import com.fibiyo.ecommerce.domain.entity.User;
import jakarta.validation.Valid; // Bean Validation için
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus; // HTTP durum kodları için
import org.springframework.http.ResponseEntity; // HTTP yanıtları için
import org.springframework.web.bind.annotation.*;

@RestController // REST Controller olduğunu belirtir
@RequestMapping("/api/auth") // Bu controller'daki tüm endpoint'ler /api/auth ile başlar
// @CrossOrigin(origins = "http://localhost:4200") // SecurityConfig'deki global ayar yeterli, gerekirse spesifik endpoint'lere eklenebilir
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    @Autowired // Constructor injection tercih edilir
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // --- LOGIN Endpoint ---
    @PostMapping("/login") // veya "/signin"
    public ResponseEntity<JwtAuthenticationResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        logger.info("POST /api/auth/login requested for user: {}", loginRequest.getUsernameOrEmail());
        JwtAuthenticationResponse jwtResponse = authService.authenticateUser(loginRequest);
        // Başarılı yanıtta JWT token'ını ve 200 OK durumunu döndür
        return ResponseEntity.ok(jwtResponse);
    }

    // --- REGISTER Endpoint ---
    @PostMapping("/register") // veya "/signup"
    public ResponseEntity<ApiResponse> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        logger.info("POST /api/auth/register requested for username: {}", registerRequest.getUsername());

        User registeredUser = authService.registerUser(registerRequest); // Servis metodunu çağır

        // Başarılı yanıtta basit bir mesaj ve 201 Created durumunu döndür
        ApiResponse response = new ApiResponse(true, "Kullanıcı başarıyla kaydedildi!", registeredUser.getId());
        // İsteğe bağlı olarak UserResponse DTO'su da döndürülebilir

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // TODO: Sosyal medya login/callback endpoint'leri eklenecek
    // @GetMapping("/oauth2/callback/{provider}")
    // public ResponseEntity<?> handleOAuth2Callback(...)

    // TODO: Şifremi unuttum, şifre sıfırlama endpoint'leri eklenecek

    // --- Şifremi Unuttum Endpoint'i ---
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
    logger.info("POST /api/auth/forgot-password requested for email: {}", request.getEmail());
     // Servis hata fırlatmak yerine sessizce işlemi bitireceği için her zaman OK dönebiliriz.
     authService.processForgotPassword(request.getEmail());
     String message = "Eğer girdiğiniz e-posta adresi sistemimizde kayıtlıysa, şifre sıfırlama linki gönderilmiştir.";
     return ResponseEntity.ok(new ApiResponse(true, message));
}
    // --- Şifre Sıfırlama Endpoint'i ---
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
    logger.info("POST /api/auth/reset-password requested with token (partial): {}", request.getToken().substring(0, Math.min(request.getToken().length(), 8))+"...");
    authService.resetPassword(request);
    return ResponseEntity.ok(new ApiResponse(true, "Şifreniz başarıyla sıfırlandı. Şimdi giriş yapabilirsiniz."));
}


    // TODO: E-posta doğrulama endpoint'leri eklenecek
}