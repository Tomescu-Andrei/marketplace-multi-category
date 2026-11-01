package com.marketplace.controller;

import com.marketplace.dto.CategoryDetailsResponse;
import com.marketplace.dto.CategoryResponse;
import com.marketplace.service.CategoryService;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {
    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<CategoryResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public CategoryDetailsResponse findById(@PathVariable @Positive long id) {
        return service.findById(id);
    }
}
