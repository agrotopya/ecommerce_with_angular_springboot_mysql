package com.fibiyo.ecommerce.config;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty; // bu eklenecek
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class OpenAiConfig {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiConfig.class);

    @Value("${openai.api.key:#{null}}")
    private String openaiApiKey;

    @Value("${openai.timeout.connect:15}")
    private long connectTimeoutSeconds;

    @Value("${openai.timeout.read:180}")
    private long readTimeoutSeconds;

    @Value("${openai.timeout.write:60}")
    private long writeTimeoutSeconds;

    // bu eklenecek
    @Bean
    @ConditionalOnProperty(name = "openai.api.key") // sadece key varsa oluştur
    public OpenAIClient openAIClient() {
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(writeTimeoutSeconds, TimeUnit.SECONDS)
                .build();

        logger.info("Configuring OpenAIClient with custom timeouts...");
        return OpenAIOkHttpClient.builder()
                .apiKey(openaiApiKey)
                .build();
    }
}
