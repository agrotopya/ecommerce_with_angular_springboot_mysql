package com.fibiyo.ecommerce.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload-dir:./uploads/}") 
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String resolvedUploadDir = uploadDir.startsWith("file:") ? uploadDir : "file:" + uploadDir;
        if (!resolvedUploadDir.endsWith("/")) {
            resolvedUploadDir += "/";
        }

        // /feels/videos/** URL'lerini diskteki uploads/feels/videos/ altına yönlendir
        // Bu, http://localhost:8080/feels/videos/xyz.mp4 isteğinin
        // ./uploads/feels/videos/xyz.mp4 dosyasını sunmasını sağlar.
        registry.addResourceHandler("/feels/videos/**")
                .addResourceLocations(resolvedUploadDir + "feels/videos/");

        // Diğer yüklenen dosyalar için genel bir kural (örneğin ürün resimleri, kategori resimleri)
        // Eğer bu dosyalar da /uploads/ altında farklı alt klasörlerdeyse
        // ve URL'ler http://localhost:8080/uploads/products/images/abc.png gibi ise
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(resolvedUploadDir);
        
        // Örnek: Eğer thumbnail'lar için URL yapısı http://localhost:8080/thumbnails/feel1.jpg ise
        // ve dosyalar ./uploads/thumbnails/ altında ise:
        // registry.addResourceHandler("/thumbnails/**")
        //         .addResourceLocations(resolvedUploadDir + "thumbnails/");
        
        // Örnek: Eğer satıcı profil resimleri için URL yapısı http://localhost:8080/avatars/seller1.png ise
        // ve dosyalar ./uploads/avatars/ altında ise:
        // registry.addResourceHandler("/avatars/**")
        //         .addResourceLocations(resolvedUploadDir + "avatars/");
    }
}
