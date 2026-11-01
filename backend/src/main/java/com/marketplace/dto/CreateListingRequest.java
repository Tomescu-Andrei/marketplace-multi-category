package com.marketplace.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.Map;

public record CreateListingRequest(
        @NotNull @Positive Long categoryId,
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 10000) String description,
        @NotNull ListingType listingType,
        @DecimalMin("0.01") @Digits(integer = 12, fraction = 2) BigDecimal price,
        @NotNull @Pattern(regexp = "[A-Z]{3}") String currency,
        @Size(max = 100) Map<@NotBlank @Size(max = 100) String, JsonNode> attributes) {
}
