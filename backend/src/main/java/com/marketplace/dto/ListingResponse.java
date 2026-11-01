package com.marketplace.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

public record ListingResponse(Long id, Long sellerId, Long categoryId, String title, String description,
                              ListingType listingType, BigDecimal price, String currency, String status,
                              Map<String, JsonNode> attributes, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
}
