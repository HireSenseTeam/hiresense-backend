package com.hiresense.ai.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AnthropicInvokeModelRequest(
        @JsonProperty("anthropic_version")
        String anthropicVersion,

        @JsonProperty("max_tokens")
        int maxTokens,

        List<Message> messages
) {
}
