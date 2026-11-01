package com.marketplace.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketplace.dto.CreateListingRequest;
import com.marketplace.dto.ListingResponse;
import com.marketplace.dto.ListingType;
import com.marketplace.jooq.tables.records.ListingsRecord;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.marketplace.jooq.Tables.LISTINGS;

@Repository
public class ListingRepository {
    private final DSLContext dsl;
    private final ObjectMapper objectMapper;

    public ListingRepository(DSLContext dsl, ObjectMapper objectMapper) {
        this.dsl = dsl;
        this.objectMapper = objectMapper;
    }

    public List<ListingResponse> findAll(int limit, int offset) {
        return dsl.selectFrom(LISTINGS).orderBy(LISTINGS.CREATED_AT.desc(), LISTINGS.ID.desc())
                .limit(limit).offset(offset).fetch(this::toResponse);
    }

    public Optional<ListingResponse> findById(long id) {
        return dsl.selectFrom(LISTINGS).where(LISTINGS.ID.eq(id)).fetchOptional(this::toResponse);
    }

    public ListingResponse create(CreateListingRequest request, long sellerId, Map<String, JsonNode> attributes) {
        try {
            var row = dsl.insertInto(LISTINGS)
                    .set(LISTINGS.SELLER_ID, sellerId)
                    .set(LISTINGS.CATEGORY_ID, request.categoryId())
                    .set(LISTINGS.TITLE, request.title().strip())
                    .set(LISTINGS.DESCRIPTION, request.description().strip())
                    .set(LISTINGS.LISTING_TYPE, request.listingType().name())
                    .set(LISTINGS.PRICE, request.price())
                    .set(LISTINGS.CURRENCY, request.currency())
                    .set(LISTINGS.ATTRIBUTES, JSONB.valueOf(objectMapper.writeValueAsString(attributes)))
                    .returning().fetchSingle();
            return toResponse(row);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize listing attributes", exception);
        }
    }

    private ListingResponse toResponse(ListingsRecord row) {
        try {
            Map<String, JsonNode> attributes = objectMapper.readValue(row.getAttributes().data(), new TypeReference<>() {});
            return new ListingResponse(row.getId(), row.getSellerId(), row.getCategoryId(), row.getTitle(),
                    row.getDescription(), ListingType.valueOf(row.getListingType()), row.getPrice(), row.getCurrency(),
                    row.getStatus(), attributes, row.getCreatedAt(), row.getUpdatedAt());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not read listing attributes", exception);
        }
    }
}
