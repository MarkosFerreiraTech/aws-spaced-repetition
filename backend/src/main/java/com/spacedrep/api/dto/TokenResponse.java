package com.spacedrep.api.dto;

import java.time.Instant;

public record TokenResponse(String token, String tokenType, Instant expiresAt) {
}
