package com.aether.auth.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aether.shared.ApiException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository users;

    @Mock
    private RefreshTokenRepository refreshTokens;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AuthService service(boolean openRegistration) {
        return new AuthService(users, refreshTokens, passwordEncoder, jwtService,
                new AuthProperties(openRegistration, Duration.ofMinutes(15), Duration.ofDays(30)));
    }

    @Test
    void firstAccountRegistersEvenWithClosedRegistration() {
        when(users.count()).thenReturn(0L);
        when(users.existsByEmail("nuno@aether.app")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("bcrypt-hash");
        when(jwtService.mintAccessToken(any())).thenReturn("access-token");

        AuthResult result = service(false)
                .register(new RegisterRequest("  Nuno@Aether.APP ", "password123", "Nuno"));

        assertThat(result.response().accessToken()).isEqualTo("access-token");
        assertThat(result.response().user().email()).isEqualTo("nuno@aether.app");
        assertThat(result.refreshToken()).isNotBlank();
        verify(users).save(any(User.class));
        verify(refreshTokens).save(any(RefreshToken.class));
    }

    @Test
    void registrationIsClosedAfterFirstAccount() {
        when(users.count()).thenReturn(1L);

        assertThatThrownBy(() -> service(false)
                .register(new RegisterRequest("second@aether.app", "password123", "Second")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("registration_closed"));
        verify(users, never()).save(any());
    }

    @Test
    void openRegistrationAllowsMoreAccounts() {
        when(users.existsByEmail("second@aether.app")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("bcrypt-hash");
        when(jwtService.mintAccessToken(any())).thenReturn("access-token");

        AuthResult result = service(true)
                .register(new RegisterRequest("second@aether.app", "password123", "Second"));

        assertThat(result.response().user().email()).isEqualTo("second@aether.app");
        verify(users).save(any(User.class));
    }

    @Test
    void duplicateEmailIsRejected() {
        when(users.existsByEmail("nuno@aether.app")).thenReturn(true);

        assertThatThrownBy(() -> service(true)
                .register(new RegisterRequest("nuno@aether.app", "password123", "Nuno")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("email_already_used"));
        verify(users, never()).save(any());
    }

    @Test
    void loginRejectsWrongPasswordAndUnknownEmailWithSameError() {
        User user = new User("nuno@aether.app", "bcrypt-hash", "Nuno");
        when(users.findByEmail("nuno@aether.app")).thenReturn(Optional.of(user));
        when(users.findByEmail("ghost@aether.app")).thenReturn(Optional.empty());
        when(passwordEncoder.matches("wrong", "bcrypt-hash")).thenReturn(false);

        assertThatThrownBy(() -> service(false).login(new LoginRequest("nuno@aether.app", "wrong")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("invalid_credentials"));
        assertThatThrownBy(() -> service(false).login(new LoginRequest("ghost@aether.app", "any")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("invalid_credentials"));
    }

    @Test
    void refreshRotatesTheToken() {
        User user = new User("nuno@aether.app", "bcrypt-hash", "Nuno");
        RefreshToken stored = new RefreshToken(user.id(), "stored-hash",
                Instant.now().plus(Duration.ofDays(1)));
        when(refreshTokens.findByTokenHash(anyString())).thenReturn(Optional.of(stored));
        when(users.findById(user.id())).thenReturn(Optional.of(user));
        when(jwtService.mintAccessToken(user.id())).thenReturn("new-access-token");

        AuthResult result = service(false).refresh("raw-refresh-token");

        assertThat(result.response().accessToken()).isEqualTo("new-access-token");
        verify(refreshTokens).delete(stored);
        verify(refreshTokens).save(any(RefreshToken.class));
    }

    @Test
    void expiredRefreshTokenIsRejectedAndConsumed() {
        RefreshToken stored = new RefreshToken(UUID.randomUUID(), "stored-hash",
                Instant.now().minus(Duration.ofMinutes(1)));
        when(refreshTokens.findByTokenHash(anyString())).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> service(false).refresh("raw-refresh-token"))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("invalid_refresh_token"));
        verify(refreshTokens).delete(stored);
        verify(refreshTokens, never()).save(any());
    }

    @Test
    void unknownRefreshTokenIsRejected() {
        when(refreshTokens.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service(false).refresh("raw-refresh-token"))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("invalid_refresh_token"));
    }
}
