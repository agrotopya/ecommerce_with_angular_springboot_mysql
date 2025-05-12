package com.fibiyo.ecommerce.infrastructure.security;

import com.fibiyo.ecommerce.domain.entity.User; // Kullanıcı bilgilerini almak için gerekebilir (şimdilik Authentication yetiyor)
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct; // init metodu için
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.stream.Collectors;

@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs; // long yapalım

    private SecretKey key;

    // Bean oluştuktan sonra çalışacak metot
    @PostConstruct
    public void init() {
        try {
            byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
            this.key = Keys.hmacShaKeyFor(keyBytes);
        } catch (Exception e) {
            logger.error("Error initializing JWT key: {}", e.getMessage());
            // Burada uygulamanın başlatılmasını durdurmak iyi bir fikir olabilir
            throw new RuntimeException("Failed to initialize JWT secret key", e);
        }
    }

    // JWT oluşturma
    public String generateToken(Authentication authentication) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        // Rolleri al (virgülle ayrılmış string olarak)
        String roles = userPrincipal.getAuthorities().stream()
                                    .map(GrantedAuthority::getAuthority)
                                    .collect(Collectors.joining(","));

        return Jwts.builder()
                .subject(userPrincipal.getUsername()) // Token'ın konusu kullanıcı adı olsun
                .claim("roles", roles) // Rolleri claim olarak ekle
                // İsteğe bağlı olarak başka claim'ler de eklenebilir (userId vb.)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key) // Anahtarla imzala
                .compact(); // String olarak oluştur
    }

    // Token'dan kullanıcı adını alma
    public String getUsernameFromJWT(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(this.key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

     // Token'dan rolleri alma (opsiyonel, filter'da doğrudan authentication nesnesinden alınabilir)
    public String getRolesFromJWT(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(this.key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.get("roles", String.class); // "roles" claim'ini al
    }


    // Token doğrulama
    public boolean validateToken(String authToken) {
        if (authToken == null || authToken.isBlank()) {
            return false;
        }
        try {
            Jwts.parser().verifyWith(this.key).build().parseSignedClaims(authToken);
            return true;
        } catch (MalformedJwtException ex) {
            logger.error("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            logger.warn("Expired JWT token: {}", ex.getMessage()); // Bunu 'warn' olarak loglamak daha iyi olabilir
        } catch (UnsupportedJwtException ex) {
            logger.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            logger.error("JWT claims string is empty: {}", ex.getMessage());
        } catch (JwtException ex) { // Daha genel JWT hataları
            logger.error("JWT validation failed: {}", ex.getMessage());
        }
        return false;
    }
}