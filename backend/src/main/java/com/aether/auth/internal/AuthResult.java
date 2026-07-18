package com.aether.auth.internal;

/** What the service returns: the HTTP body plus the raw refresh token for the cookie. */
record AuthResult(TokenResponse response, String refreshToken) {}
