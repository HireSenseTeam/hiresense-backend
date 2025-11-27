package com.hiresense.ai.dto.request;

import java.util.List;

public record Message(
        String role,
        List<ContentBlock> content
) {
}
