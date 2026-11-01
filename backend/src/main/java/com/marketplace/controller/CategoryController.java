package com.marketplace.controller;

import com.marketplace.dto.CategoryDetailsResponse;
import com.marketplace.dto.CategoryResponse;
import com.marketplace.service.CategoryService;
import jakarta.validation.constraints.Positive;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@Tag(name = "Categories", description = "Available categories and their attribute definitions")
public class CategoryController {
    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List categories", description = "Returns seeded categories ordered by ID.")
    public List<CategoryResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get category details", description = "Returns the selected category and its own attribute definitions. Parent attributes are not inherited.")
    public CategoryDetailsResponse findById(@PathVariable @Positive long id) {
        return service.findById(id);
    }
}
