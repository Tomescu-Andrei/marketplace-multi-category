package com.marketplace.dto;

import java.util.List;

public record CategoryDetailsResponse(Long id, String name, String slug, Long parentId,
                                      List<CategoryAttributeResponse> attributes) {
}
