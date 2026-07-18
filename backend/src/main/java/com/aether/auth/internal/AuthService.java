package com.aether.auth.internal;

import com.aether.shared.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthProperties properties;

    AuthService(UserRepository users,
                RefreshTokenRepository refreshTokens,
                PasswordEncoder passwordEncoder,
                JwtService jwtService,
                AuthProperties properties) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.properties = properties;
    }

    @Transactional
    AuthResult register(RegisterRequest request) {
        if (!properties.openRegistration() && users.count() > 0) {
            throw new ApiException(HttpStatus.FORBIDDEN, "registration_closed",
                    "Registration is closed.");
        }
        String email = normalize(request.email());
        if (users.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "email_already_used",
                    "An account with this email already exists.");
        }
        User user = new User(email, passwordEncoder.encode(request.password()),
                request.displayName().trim());
        users.save(user);
        return issueTokens(user);
    }

    @Transactional
    AuthResult login(LoginRequest request) {
        return users.findByEmail(normalize(request.email()))
                .filter(user -> passwordEncoder.matches(request.password(), user.passwordHash()))
                .map(this::issueTokens)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED,
                        "invalid_credentials", "Invalid email or password."));
    }

    // A presented refresh token is consumed no matter what: rotation on success,
    // and the delete must survive the 401 on failure — hence no rollback for it.
    @Transactional(noRollbackFor = ApiException.class)
    AuthResult refresh(String rawRefreshToken) {
        RefreshToken stored = refreshTokens.findByTokenHash(hash(rawRefreshToken))
                .orElseThrow(AuthService::invalidRefreshToken);
        refreshTokens.delete(stored);
        if (stored.isExpired(Instant.now())) {
            throw invalidRefreshToken();
        }
        User user = users.findById(stored.userId())
                .orElseThrow(AuthService::invalidRefreshToken);
        return issueTokens(user);
    }

    @Transactional
    void logout(String rawRefreshToken) {
        refreshTokens.deleteByTokenHash(hash(rawRefreshToken));
    }

    @Transactional(readOnly = true)
    UserResponse currentUser(UUID userId) {
        return users.findById(userId)
                .map(UserResponse::from)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED,
                        "invalid_token", "Unknown user."));
    }

    Duration refreshTokenTtl() {
        return properties.refreshTokenTtl();
    }

    private AuthResult issueTokens(User user) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        refreshTokens.save(new RefreshToken(user.id(), hash(rawToken),
                Instant.now().plus(properties.refreshTokenTtl())));
        String accessToken = jwtService.mintAccessToken(user.id());
        return new AuthResult(new TokenResponse(accessToken, UserResponse.from(user)), rawToken);
    }

    private static ApiException invalidRefreshToken() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "invalid_refresh_token",
                "Refresh token is invalid or expired.");
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of()
                    .formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required but unavailable", e);
        }
    }
}
