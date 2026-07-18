package com.aether.auth.internal;

import java.util.UUID;

record UserResponse(UUID id, String email, String displayName) {

    static UserResponse from(User user) {
        return new UserResponse(user.id(), user.email(), user.displayName());
    }
}
