package com.marketplace.service;

import com.marketplace.dto.CategoryDetailsResponse;
import com.marketplace.dto.CategoryResponse;
import com.marketplace.exception.ResourceNotFoundException;
import com.marketplace.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CategoryService {
    private final CategoryRepository repository;

    public CategoryService(CategoryRepository repository) {
        this.repository = repository;
    }

    public List<CategoryResponse> findAll() {
        return repository.findAll();
    }

    public CategoryDetailsResponse findById(long id) {
        var category = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category " + id + " was not found"));
        return new CategoryDetailsResponse(category.id(), category.name(), category.slug(), category.parentId(),
                repository.findAttributes(id));
    }
}
