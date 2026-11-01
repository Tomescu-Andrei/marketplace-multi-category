package com.marketplace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public record RegisterRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9_.-]{2,31}")
        @Schema(description = "Unique username, 3–32 ASCII letters/digits, dot, underscore or hyphen; starts with a letter/digit", example = "ana.popescu") String username,
        @NotBlank @Email @Size(max = 254) @Schema(example = "seller@example.com") String email,
        @NotBlank @Size(min = 12, max = 72) @Schema(format = "password", accessMode = Schema.AccessMode.WRITE_ONLY,
                description = "12 or more characters; at most 72 UTF-8 bytes") String password,
        @NotBlank @Size(max = 100) @Schema(example = "Marketplace Seller") String displayName) {
}
