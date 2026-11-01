package com.marketplace.controller;

import com.marketplace.dto.CreateListingRequest;
import com.marketplace.dto.ListingResponse;
import com.marketplace.service.ListingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/listings")
@Tag(name = "Listings", description = "Create and retrieve listings using the M1 test seller")
public class ListingController {
    private final ListingService service;

    public ListingController(ListingService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List listings", description = "Newest first, ordered by creation time and ID. Limit: 1–100; offset: 0 or greater.")
    public List<ListingResponse> findAll(@RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
                                         @RequestParam(defaultValue = "0") @Min(0) int offset) {
        return service.findAll(limit, offset);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get listing details")
    public ListingResponse findById(@PathVariable @Positive long id) {
        return service.findById(id);
    }

    @PostMapping
    @Operation(summary = "Create a listing", description = "The server supplies the predefined seller. FIXED_PRICE requires a positive price. "
            + "AUCTION accepts a null price and stores metadata only. Attribute codes and JSON scalar types must match the category definitions.")
    @ApiResponse(responseCode = "201", description = "Listing created",
            headers = @Header(name = "Location", description = "Relative URL of the created listing", schema = @Schema(type = "string")),
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ListingResponse.class)))
    public ResponseEntity<ListingResponse> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples = @ExampleObject(
                    name = "Electronics fixed-price listing", value = """
                    {"categoryId":1,"title":"Used laptop","description":"Working laptop",
                     "listingType":"FIXED_PRICE","price":1250.50,"currency":"RON",
                     "attributes":{"brand":"Lenovo","year":2022,"original_packaging":false}}
                    """)))
            @RequestBody @Valid CreateListingRequest request) {
        var listing = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/listings/" + listing.id())).body(listing);
    }
}
