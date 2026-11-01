package com.marketplace.dto;

import java.time.OffsetDateTime;

public record UserResponse(Long id, String email, String displayName, OffsetDateTime createdAt) {
}
