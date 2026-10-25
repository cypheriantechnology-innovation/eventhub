package com.eventhub.infrastructure.adapters.in.web.dto;

public record LoginResponse(
        String token,
        String tokenType,
        String username,
        String message
) {
    public static LoginResponse success(String token, String username) {
        return new LoginResponse(token, "Bearer", username, "Authentication successful");
    }
}
