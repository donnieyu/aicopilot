package com.example.aicopilot.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class AiConfig {

    @Value("${openai.api-key}")
    private String apiKey;

    @Bean
    ChatLanguageModel chatLanguageModel() {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4o-mini") // 빠르고 저렴한 모델 (Agent용)
                .timeout(Duration.ofSeconds(120)) // 복잡한 생성은 시간이 걸릴 수 있음
                .build();
    }
}