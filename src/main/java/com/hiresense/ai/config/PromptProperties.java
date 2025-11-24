package com.hiresense.ai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "prompts")
@Getter
@Setter
public class PromptProperties {
    private String tailBiting;
    private String jobPostingQuestion;
    private String resumeQuestion;
    private String scoring;
}
