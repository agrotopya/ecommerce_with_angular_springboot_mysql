package com.fibiyo.ecommerce.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService; // Interface'i kullanalım
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils; // Metin kontrolü için yardımcı sınıf
import org.springframework.web.filter.OncePerRequestFilter; // Her istekte bir kez çalışır

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtTokenProvider tokenProvider;

    // Custom yerine interface üzerinden inject etmek daha doğru
    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getServletPath();
        return path.startsWith("/api/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        


                                        
       

                                        try {
            String jwt = getJwtFromRequest(request);

            // Token var mı ve geçerli mi kontrolü
            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                // Token'dan kullanıcı adını al
                String username = tokenProvider.getUsernameFromJWT(jwt);

                // Kullanıcı bilgilerini UserDetailsService üzerinden yükle
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // Spring Security için Authentication nesnesi oluştur
                // userDetails: kimlik bilgileri, null: şifre (gerek yok, token doğrulandı), authorities: yetkiler
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());

                // Authentication nesnesine request detaylarını ekle (IP adresi vb.)
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Oluşturulan Authentication nesnesini SecurityContext'e yerleştir
                SecurityContextHolder.getContext().setAuthentication(authentication);
                logger.debug("Authentication set for user '{}' in SecurityContext. Is authenticated: {}", username, SecurityContextHolder.getContext().getAuthentication().isAuthenticated());
            } else {
                if (StringUtils.hasText(jwt)) {
                   logger.warn("Invalid JWT token for request to {}: {}", request.getRequestURI(), jwt);
                } else {
                   logger.debug("No JWT token found in request to {}", request.getRequestURI());
                }
            }
        } catch (Exception ex) {
            // Token parse etme, user bulma vb. hatalar
            logger.error("Could not set user authentication in security context", ex);
        }

        // Filtre zincirinde bir sonraki adıma geç
        filterChain.doFilter(request, response);
    }

    // Request header'dan "Authorization: Bearer <token>" kısmını parse eder
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // "Bearer " kısmını at
        }
        return null; // Token yok veya format hatalı
    }
}
