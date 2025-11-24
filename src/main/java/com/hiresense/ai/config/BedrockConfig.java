package com.hiresense.ai.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

import jakarta.annotation.PostConstruct;

@Slf4j
@Configuration
public class BedrockConfig {

    @Value("${bedrock.model-id}")
    private String modelId;

    @Value("${bedrock.region:us-east-1}")
    private String region;

    @Value("${bedrock.enabled:true}")
    private boolean bedrockEnabled;

    @PostConstruct
    public void validateConfig() {
        if (bedrockEnabled) {
            if (modelId == null || modelId.isBlank()) {
                log.warn("[BedrockConfig] bedrock.model-id가 설정되지 않았습니다. Bedrock 기능이 제대로 동작하지 않을 수 있습니다.");
            }
            if (region == null || region.isBlank()) {
                log.warn("[BedrockConfig] bedrock.region이 설정되지 않았습니다. 기본값(us-east-1)을 사용합니다.");
            }
            log.info("[BedrockConfig] Bedrock 설정 완료 - modelId: {}, region: {}, enabled: {}", modelId, region, bedrockEnabled);
        } else {
            log.info("[BedrockConfig] Bedrock이 비활성화되어 있습니다.");
        }
    }

    @Bean
    public BedrockRuntimeClient bedrockRuntimeClient() {
        if (!bedrockEnabled) {
            log.info("[BedrockConfig] Bedrock이 비활성화되어 있어 BedrockClient Bean을 생성하지 않습니다.");
            return BedrockRuntimeClient.builder()
                    .region(Region.of(region))
                    .credentialsProvider(DefaultCredentialsProvider.create())
                    .build();
        }

        try {
            BedrockRuntimeClient client = BedrockRuntimeClient.builder()
                    .region(Region.of(region))
                    .credentialsProvider(DefaultCredentialsProvider.create())
                    .build();
            log.info("[BedrockConfig] BedrockRuntimeClient Bean 생성 완료");
            return client;
        } catch (Exception e) {
            log.error("[BedrockConfig] BedrockRuntimeClient Bean 생성 실패: {}", e.getMessage(), e);
            return BedrockRuntimeClient.builder()
                    .region(Region.of(region))
                    .credentialsProvider(DefaultCredentialsProvider.create())
                    .build();
        }
    }

    public String getModelId() {
        return modelId;
    }

    public String getRegion() {
        return region;
    }

    public boolean isBedrockEnabled() {
        return bedrockEnabled;
    }
}
