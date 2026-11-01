package com.marketplace.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.marketplace.dto.CreateListingRequest;
import com.marketplace.dto.ListingResponse;
import com.marketplace.dto.ListingType;
import com.marketplace.exception.InvalidListingException;
import com.marketplace.exception.ResourceNotFoundException;
import com.marketplace.repository.ListingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ListingService {
    private final ListingRepository repository;
    private final CategoryService categoryService;
    private final long testSellerId;

    public ListingService(ListingRepository repository, CategoryService categoryService,
                          @Value("${marketplace.test-seller-id:1}") long testSellerId) {
        this.repository = repository;
        this.categoryService = categoryService;
        this.testSellerId = testSellerId;
    }

    public List<ListingResponse> findAll(int limit, int offset) {
        return repository.findAll(limit, offset);
    }

    public ListingResponse findById(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Listing " + id + " was not found"));
    }

    @Transactional
    public ListingResponse create(CreateListingRequest request) {
        if (request.listingType() == ListingType.FIXED_PRICE && request.price() == null) {
            throw new InvalidListingException("price", "A fixed-price listing requires a price");
        }
        try {
            Currency.getInstance(request.currency());
        } catch (IllegalArgumentException exception) {
            throw new InvalidListingException("currency", "Currency must be a supported ISO 4217 code");
        }
        var category = categoryService.findById(request.categoryId());
        Map<String, JsonNode> attributes = request.attributes() == null ? Map.of() : request.attributes();
        Set<String> allowedCodes = category.attributes().stream().map(attribute -> attribute.code()).collect(Collectors.toSet());
        for (String code : attributes.keySet()) {
            if (!allowedCodes.contains(code)) {
                throw new InvalidListingException("attributes." + code, "Unknown category attribute: " + code);
            }
        }
        for (var definition : category.attributes()) {
            var value = attributes.get(definition.code());
            if (value == null || value.isNull()) {
                if (definition.required()) {
                    throw new InvalidListingException("attributes." + definition.code(), "Required attribute is missing: " + definition.code());
                }
                continue;
            }
            boolean valid = switch (definition.dataType()) {
                case "TEXT" -> value.isTextual() && !value.textValue().isBlank() && value.textValue().length() <= 1000;
                case "NUMBER" -> value.isNumber() && value.asText().length() <= 100;
                case "BOOLEAN" -> value.isBoolean();
                default -> false;
            };
            if (!valid) {
                throw new InvalidListingException("attributes." + definition.code(), "Invalid value for " + definition.code() + "; expected " + definition.dataType());
            }
        }
        if (!repository.sellerExists(testSellerId)) {
            throw new IllegalStateException("The M1 test seller is not configured");
        }
        return repository.create(request, testSellerId, attributes);
    }
}
