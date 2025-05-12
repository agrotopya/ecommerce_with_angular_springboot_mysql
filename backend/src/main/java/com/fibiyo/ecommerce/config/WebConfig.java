package com.fibiyo.ecommerce.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${file.serve-path}")
    private String servePath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Ensure servePath ends with /** and uploadDir ends with /
        String aServePath = servePath.endsWith("/") ? servePath + "**" : servePath + "/**";
        String anUploadDir = uploadDir.endsWith("/") ? uploadDir : uploadDir + "/";

        registry.addResourceHandler(aServePath)
                .addResourceLocations("file:" + anUploadDir);
    }

    // CORS yapılandırması SecurityConfig.java içinde yönetiliyor.
    // Bu bean'i ve metodu kaldırmak, olası çakışmaları önler.
    /*
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("http://localhost:4200")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
    */
}
