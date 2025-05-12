package com.fibiyo.ecommerce.application.service.impl; // veya com.fibiyo.ecommerce.application.service;
import com.fibiyo.ecommerce.application.service.NotificationService; // eğer ekli değilse
import com.fibiyo.ecommerce.domain.enums.NotificationType; // bu eklenecek
import com.fibiyo.ecommerce.application.dto.JwtAuthenticationResponse;
import com.fibiyo.ecommerce.application.dto.LoginRequest;
import com.fibiyo.ecommerce.application.dto.RegisterRequest;
import com.fibiyo.ecommerce.application.dto.ResetPasswordRequest;
import com.fibiyo.ecommerce.application.exception.BadRequestException; // Custom exception
import com.fibiyo.ecommerce.application.service.AuthService;
import com.fibiyo.ecommerce.application.service.EmailService;
import com.fibiyo.ecommerce.domain.entity.User;
import com.fibiyo.ecommerce.domain.enums.Role;
import com.fibiyo.ecommerce.infrastructure.persistence.repository.UserRepository;
import com.fibiyo.ecommerce.infrastructure.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority; // Roller için
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails; // Principal'ı cast etmek için
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration; // Token geçerlilik süresi için
import java.time.LocalDateTime;
import java.util.UUID;     // Token üretmek için


import java.util.List; // Roller listesi için
import java.util.Optional;
import java.util.stream.Collectors; // Roller için

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    // Gerekli bağımlılıkları @Autowired ile inject edelim
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final NotificationService notificationService; // bu eklenecek
    private final EmailService emailService; // Inject et
    



    @Autowired
    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider tokenProvider,
                           NotificationService notificationService,
                           EmailService emailService) { 
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.notificationService = notificationService; 
        this.emailService = emailService;
    }

    @Override
    @Transactional(readOnly = true) // Sadece okuma ve token üretimi var
    public JwtAuthenticationResponse authenticateUser(LoginRequest loginRequest) {
        logger.info("Authentication attempt for user: {}", loginRequest.getUsernameOrEmail());
        try {
            // Spring Security'nin kimlik doğrulama mekanizmasını kullan
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsernameOrEmail(),
                            loginRequest.getPassword()
                    )
            );

            // Authentication nesnesini Security Context'e yerleştir (opsiyonel ama iyi pratik)
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Başarılı kimlik doğrulama sonrası JWT oluştur
            String jwt = tokenProvider.generateToken(authentication);
            logger.debug("JWT generated successfully for user: {}", loginRequest.getUsernameOrEmail());

            // Yanıt için ek kullanıcı bilgilerini alalım
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            // UserDetails üzerinden rolleri al
             List<String> roles = userDetails.getAuthorities().stream()
                     .map(GrantedAuthority::getAuthority)
                     .collect(Collectors.toList());

            // Veritabanından tam User nesnesini almak da mümkün ama username ve ID yeterli olabilir.
             User user = userRepository.findByUsername(userDetails.getUsername())
                     .orElseThrow(() -> new BadRequestException("Authenticated user not found in database - unexpected.")); // Bu olmamalı

            logger.info("User '{}' authenticated successfully. Roles: {}", user.getUsername(), roles);

            return new JwtAuthenticationResponse(jwt, user.getId(), user.getUsername(), user.getEmail(), roles);

        } catch (Exception e) {
            logger.error("Authentication failed for user: {}", loginRequest.getUsernameOrEmail(), e);
            // Direkt AuthenticationException yerine daha genel bir exception fırlatmak veya özel hata DTO dönmek daha iyi olabilir.
            // Şimdilik BadRequestException kullanalım.
            throw new BadRequestException("Giriş başarısız: Kullanıcı adı veya şifre hatalı.", e);
        }
    }

    @Override
    @Transactional // Veritabanına yazma işlemi var
    public User registerUser(RegisterRequest registerRequest) {
        logger.info("Registration attempt for username: {}", registerRequest.getUsername());

        // Kullanıcı adı mevcut mu?
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            logger.warn("Username '{}' is already taken.", registerRequest.getUsername());
            throw new BadRequestException("Kullanıcı adı zaten alınmış!");
        }

        // E-posta mevcut mu?
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            logger.warn("Email '{}' is already in use.", registerRequest.getEmail());
            throw new BadRequestException("E-posta adresi zaten kullanılıyor!");
        }

         // İstediğimiz roller dışında (örn: ADMIN) kayıt olunmasını engelle
        if (registerRequest.getRole() == Role.ADMIN) {
            logger.warn("Registration attempt with ADMIN role denied for username '{}'", registerRequest.getUsername());
            throw new BadRequestException("Bu rolle kayıt olamazsınız.");
        }


        // Yeni Kullanıcı Oluşturma
        User user = new User();
        user.setFirstName(registerRequest.getFirstName());
        user.setLastName(registerRequest.getLastName());
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPasswordHash(passwordEncoder.encode(registerRequest.getPassword())); // Şifreyi hash'le
        user.setRole(registerRequest.getRole());
        user.setActive(true); // Yeni kullanıcılar varsayılan olarak aktif
        user.setAuthProvider(null); // Local kayıt olduğu için provider null
        user.setProviderId(null);
        // Varsayılan abonelik, puan, kota vs. User entity'sinde default olarak ayarlı

        User savedUser = userRepository.save(user);
        logger.info("User '{}' registered successfully with role {}.", savedUser.getUsername(), savedUser.getRole());

        // Burada kayıt sonrası email gönderme gibi işlemler de eklenebilir (Async olarak)



        try {
            String subject = "Fibiyo E-Ticaret'e Hoş Geldiniz!";
            String textBody = "Merhaba " + savedUser.getFirstName() + ",\n\nFibiyo platformuna başarıyla kaydoldunuz.\n\nİyi alışverişler!";
             // String htmlBody = "<html><body><h1>Merhaba "+savedUser.getFirstName()+"</h1><p>Fibiyo platformuna başarıyla kaydoldunuz.</p></body></html>"; // HTML istersen
            emailService.sendSimpleMessage(savedUser.getEmail(), subject, textBody);
       } catch (Exception e){
            // E-posta gönderimi hatası ana işlemi etkilememeli ama loglanmalı
            logger.error("Failed to send registration confirmation email to {}: {}", savedUser.getEmail(), e.getMessage());
       }


        notificationService.createNotification(
            savedUser,
            "Kayıt işleminiz başarıyla tamamlandı. Hoş geldiniz!",
            "/profile",
            NotificationType.SYSTEM
        );
        return savedUser; // Kaydedilmiş User nesnesini dön (ID vb. içerir)
    }


    
    @Override
    @Transactional // Token kaydetme işlemi var
    public void processForgotPassword(String email) {
         logger.info("Forgot password request received for email: {}", email);
         Optional<User> userOpt = userRepository.findByEmail(email);

         if (userOpt.isEmpty()) {
             // ÖNEMLİ GÜVENLİK NOTU: Kullanıcının var olup olmadığını belli etmemek için
             // hata fırlatmak yerine başarılı gibi loglayıp e-posta GÖNDERMEMEK daha güvenli olabilir.
             logger.warn("Password reset requested for non-existent email: {}", email);
             // throw new ResourceNotFoundException("Bu e-posta adresi ile kayıtlı kullanıcı bulunamadı."); // -> Bu bilgi sızdırır!
             return; // Sessizce işlemi bitir
         }

        User user = userOpt.get();

        // Eğer kullanıcı sosyal medya ile kayıt olduysa ve şifresi yoksa?
        if(user.getPasswordHash() == null && user.getAuthProvider() != null){
            logger.warn("Password reset requested for social media user ({}): {}", user.getAuthProvider(), email);
             // Bu durumda ne yapılmalı? E-posta ile bilgi mi verilmeli, işlem mi engellenmeli?
             // Şimdilik engellemeyelim, belki sonradan lokal şifre belirlemek ister.
             // throw new BadRequestException("Sosyal medya hesabınızla giriş yapmanız gerekmektedir.");
        }

         // Güvenli bir token oluştur (UUID iyi bir başlangıç)
         String resetToken = UUID.randomUUID().toString();
         // Token'ın geçerlilik süresini belirle (örn: 1 saat)
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(1); // Veya plusMinutes(30)

         // Token ve expiry date'i kullanıcıya kaydet
         user.setPasswordResetToken(resetToken);
         user.setPasswordResetTokenExpiry(expiryDate);
         userRepository.save(user);

         // Sıfırlama linkini oluştur (Frontend URL'i + token)
          // Frontend URL'ini application.properties'dan almak daha iyi olur.
          String frontendBaseUrl = "http://localhost:4200"; // Geçici
          String resetUrl = frontendBaseUrl + "/auth/reset-password?token=" + resetToken;


         // E-posta gönder
         try {
             String subject = "Fibiyo Şifre Sıfırlama İsteği";
              String emailBody = "Merhaba " + user.getFirstName() + ",\n\n"
                       + "Şifrenizi sıfırlamak için aşağıdaki linke tıklayın. Bu link 1 saat geçerlidir:\n\n"
                       + resetUrl + "\n\n"
                      + "Eğer bu isteği siz yapmadıysanız, bu e-postayı görmezden gelebilirsiniz.";
             emailService.sendSimpleMessage(user.getEmail(), subject, emailBody);
             logger.info("Password reset email sent to {} (Token expires at {})", email, expiryDate);
          } catch (Exception e) {
               logger.error("Failed to send password reset email to {}: {}", email, e.getMessage(), e);
               // E-posta gitmese bile token kaydedildi. Kullanıcı tekrar deneyebilir.
               // Hata fırlatmak yerine loglamak yeterli.
           }
     }

     @Override
     @Transactional // Şifre güncelleme ve token silme işlemi var
     public void resetPassword(ResetPasswordRequest request) {
          logger.info("Reset password attempt received for token: {}", request.getToken().substring(0, Math.min(request.getToken().length(), 8)) + "..."); // Token'ın tamamını loglama
 
          if(request.getToken() == null || request.getToken().isBlank()){
              throw new BadRequestException("Şifre sıfırlama token'ı geçersiz veya eksik.");
          }
 
         // Token ile kullanıcıyı bul
          User user = userRepository.findByPasswordResetToken(request.getToken()) // Bu metodu Repo'ya eklemeliyiz!
                  .orElseThrow(() -> {
                       logger.warn("Invalid or non-existent password reset token used: {}", request.getToken());
                      return new BadRequestException("Şifre sıfırlama linki geçersiz veya süresi dolmuş.");
                  });
 
         // Token süresi dolmuş mu kontrol et
          if (user.getPasswordResetTokenExpiry() == null || user.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {
              logger.warn("Expired password reset token used for user '{}' (Token: {})", user.getUsername(), request.getToken());
              // Token'ı temizleyebiliriz
               user.setPasswordResetToken(null);
               user.setPasswordResetTokenExpiry(null);
               userRepository.save(user);
              throw new BadRequestException("Şifre sıfırlama linkinin süresi dolmuş. Lütfen tekrar istek gönderin.");
          }
 
         // Yeni şifreyi hash'le ve kullanıcıyı güncelle
         user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
 
         // Token'ı temizle (tek kullanımlık)
          user.setPasswordResetToken(null);
          user.setPasswordResetTokenExpiry(null);
 
         userRepository.save(user);
         logger.info("Password successfully reset for user '{}'", user.getUsername());
 
          // Opsiyonel: Kullanıcıya şifresinin değiştirildiğine dair bilgi e-postası gönderilebilir.
          try {
                String subject = "Fibiyo Şifreniz Değiştirildi";
                String body = "Merhaba " + user.getFirstName() + ",\n\nHesabınızın şifresi başarıyla değiştirilmiştir.";
                emailService.sendSimpleMessage(user.getEmail(), subject, body);
            } catch (Exception e) {
               logger.error("Failed to send password change confirmation email to {}: {}", user.getEmail(), e.getMessage());
            }
     }



}