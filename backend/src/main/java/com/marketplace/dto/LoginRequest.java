package com.marketplace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Size(max = 254) @JsonAlias("email")
        @Schema(description = "Username or email address", example = "ana.popescu") String identifier,
        @NotBlank @Size(max = 256) @Schema(format = "password", accessMode = Schema.AccessMode.WRITE_ONLY) String password) {
}
