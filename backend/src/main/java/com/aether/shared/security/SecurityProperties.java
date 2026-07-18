package com.aether.shared.security;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aether.security")
public record SecurityProperties(String jwtSecret, List<String> allowedOrigins) {}
