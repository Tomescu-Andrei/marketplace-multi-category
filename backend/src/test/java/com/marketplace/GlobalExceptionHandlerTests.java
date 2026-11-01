package com.marketplace;

import com.marketplace.controller.ListingController;
import com.marketplace.service.ListingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Focused failure-path checks complement the real PostgreSQL integration tests.
@WebMvcTest(ListingController.class)
class GlobalExceptionHandlerTests {
    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ListingService service;

    @Test
    void unexpectedFailuresDoNotExposeInternalMessages() throws Exception {
        when(service.findById(1)).thenThrow(new IllegalStateException("Internal configuration detail"));
        mvc.perform(get("/api/v1/listings/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.instance").value("/api/v1/listings/1"))
                .andExpect(jsonPath("$.errors").isEmpty())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
    }

    @Test
    void constraintFailuresDoNotExposeSqlOrConstraintNames() throws Exception {
        when(service.findById(1)).thenThrow(new DataIntegrityViolationException("SQL INSERT internal_constraint_name"));
        mvc.perform(get("/api/v1/listings/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DATA_CONFLICT"))
                .andExpect(jsonPath("$.detail").value("The operation conflicts with existing data"))
                .andExpect(jsonPath("$.errors").isEmpty());
    }
}
