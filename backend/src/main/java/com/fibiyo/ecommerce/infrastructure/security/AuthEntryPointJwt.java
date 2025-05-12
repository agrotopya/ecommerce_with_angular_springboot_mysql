package com.fibiyo.ecommerce.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fibiyo.ecommerce.application.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component // bu eklenecek
public class AuthEntryPointJwt implements AuthenticationEntryPoint {

    private static final Logger logger = LoggerFactory.getLogger(AuthEntryPointJwt.class);

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        logger.error("Unauthorized error: {}", authException.getMessage());

        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        ApiResponse apiResponse = new ApiResponse(false, "Giriş yapmanız gerekiyor.");
        ObjectMapper mapper = new ObjectMapper();
        String jsonResponse = mapper.writeValueAsString(apiResponse);

        response.getWriter().write(jsonResponse);
    }
}
