package com.aether.ai.internal;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The API key is supplied by the environment and never committed. When it is
 * absent the application still starts; only the chat endpoint refuses.
 */
@ConfigurationProperties(prefix = "aether.ai")
public record AiProperties(
        String apiKey,
        String model,
        String baseUrl,
        int maxTokens,
        int maxToolIterations) {

    boolean configured() {
        return apiKey != null && !apiKey.isBlank();
    }
}
