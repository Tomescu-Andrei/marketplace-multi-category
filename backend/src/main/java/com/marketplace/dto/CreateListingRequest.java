package com.marketplace.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.Map;

public record CreateListingRequest(
        @Schema(description = "Existing category ID", example = "1")
        @NotNull @Positive Long categoryId,
        @Schema(example = "Used laptop")
        @NotBlank @Size(max = 200) String title,
        @Schema(example = "Working laptop")
        @NotBlank @Size(max = 10000) String description,
        @NotNull ListingType listingType,
        @Schema(description = "Required for FIXED_PRICE; optional for AUCTION. Positive, at most 12 integer digits and 2 decimal places.", example = "1250.50", nullable = true)
        @DecimalMin("0.01") @Digits(integer = 12, fraction = 2) BigDecimal price,
        @Schema(description = "Supported uppercase ISO 4217 currency", example = "RON")
        @NotNull @Pattern(regexp = "[A-Z]{3}") String currency,
        @Schema(description = "Category-specific values keyed by attribute code. TEXT, NUMBER and BOOLEAN require matching JSON scalar types. Optional values may be null.",
                type = "object", example = "{\"brand\":\"Lenovo\",\"year\":2022,\"original_packaging\":false}")
        @Size(max = 100) Map<@NotBlank @Size(max = 100) String, JsonNode> attributes) {
}
