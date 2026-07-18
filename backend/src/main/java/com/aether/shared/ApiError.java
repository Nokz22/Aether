package com.aether.shared;

import java.util.Map;

/** Error payload returned by every failing API call. */
public record ApiError(String code, String message, Map<String, String> fields) {

    public static ApiError of(String code, String message) {
        return new ApiError(code, message, Map.of());
    }
}
