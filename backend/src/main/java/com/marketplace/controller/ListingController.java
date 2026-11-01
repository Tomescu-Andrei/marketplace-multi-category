package com.marketplace.controller;

import com.marketplace.dto.CreateListingRequest;
import com.marketplace.dto.ListingResponse;
import com.marketplace.service.ListingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/listings")
public class ListingController {
    private final ListingService service;

    public ListingController(ListingService service) {
        this.service = service;
    }

    @GetMapping
    public List<ListingResponse> findAll(@RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
                                         @RequestParam(defaultValue = "0") @Min(0) int offset) {
        return service.findAll(limit, offset);
    }

    @GetMapping("/{id}")
    public ListingResponse findById(@PathVariable @Positive long id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<ListingResponse> create(@RequestBody @Valid CreateListingRequest request) {
        var listing = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/listings/" + listing.id())).body(listing);
    }
}
