package com.aether.auth.internal;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aether.auth")
public record AuthProperties(
        boolean openRegistration,
        Duration accessTokenTtl,
        Duration refreshTokenTtl) {}
