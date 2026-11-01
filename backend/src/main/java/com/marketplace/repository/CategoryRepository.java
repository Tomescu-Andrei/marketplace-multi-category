package com.marketplace.repository;

import com.marketplace.dto.CategoryAttributeResponse;
import com.marketplace.dto.CategoryResponse;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static com.marketplace.jooq.Tables.CATEGORIES;
import static com.marketplace.jooq.Tables.CATEGORY_ATTRIBUTES;

@Repository
public class CategoryRepository {
    private final DSLContext dsl;

    public CategoryRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<CategoryResponse> findAll() {
        return dsl.selectFrom(CATEGORIES).orderBy(CATEGORIES.ID)
                .fetch(row -> new CategoryResponse(row.getId(), row.getName(), row.getSlug(), row.getParentId()));
    }

    public Optional<CategoryResponse> findById(long id) {
        return dsl.selectFrom(CATEGORIES).where(CATEGORIES.ID.eq(id))
                .fetchOptional(row -> new CategoryResponse(row.getId(), row.getName(), row.getSlug(), row.getParentId()));
    }

    public List<CategoryAttributeResponse> findAttributes(long categoryId) {
        return dsl.selectFrom(CATEGORY_ATTRIBUTES).where(CATEGORY_ATTRIBUTES.CATEGORY_ID.eq(categoryId))
                .orderBy(CATEGORY_ATTRIBUTES.ID)
                .fetch(row -> new CategoryAttributeResponse(row.getId(), row.getName(), row.getCode(),
                        row.getDataType(), row.getRequired()));
    }
}
