package com.marketplace.dto;

public record CategoryResponse(Long id, String name, String slug, Long parentId) {
}
