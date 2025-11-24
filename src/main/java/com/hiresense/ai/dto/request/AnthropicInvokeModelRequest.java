package com.hiresense.ai.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AnthropicInvokeModelRequest {

    @JsonProperty("anthropic_version")
    private String anthropicVersion;

    @JsonProperty("max_tokens")
    private int maxTokens;

    private List<Message> messages;
}
