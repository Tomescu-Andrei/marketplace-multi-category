package com.marketplace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 254) @Schema(example = "seller@example.com") String email,
        @NotBlank @Size(min = 12, max = 72) @Schema(format = "password", accessMode = Schema.AccessMode.WRITE_ONLY,
                description = "12 or more characters; at most 72 UTF-8 bytes") String password,
        @NotBlank @Size(max = 100) @Schema(example = "Marketplace Seller") String displayName) {
}
