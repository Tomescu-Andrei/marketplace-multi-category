package com.marketplace;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static com.marketplace.jooq.Tables.LISTINGS;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureMockMvc
class MarketplaceApplicationTests {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("marketplace_test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private DataSource dataSource;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DSLContext dsl;

    @Autowired
    private Flyway flyway;

    @BeforeEach
    void clearListings() {
        dsl.deleteFrom(LISTINGS).execute();
    }

    private static final String VALID_LISTING = """
            {"categoryId":1,"title":"Used laptop","description":"Working laptop",
             "listingType":"FIXED_PRICE","price":1250.50,"currency":"RON",
             "attributes":{"brand":"Lenovo","year":2022,"original_packaging":false}}
            """;

    @Test
    void applicationConnectsToPostgres() throws SQLException {
        try (var connection = dataSource.getConnection();
             var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT current_database()")) {
            assertThat(connection.isValid(5)).isTrue();
            assertThat(result.next()).isTrue();
            assertThat(result.getString(1)).isEqualTo("marketplace_test");
        }
    }

    @Test
    void migrationsRunOnCleanPostgresAndCanBeValidatedAgain() {
        flyway.validate();
        assertThat(flyway.info().applied()).hasSize(3);
        assertThat(flyway.migrate().migrationsExecuted).isZero();
    }

    @Test
    void categoriesContainTheirAttributeDefinitions() throws Exception {
        mvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].slug").value("electronics"));
        mvc.perform(get("/api/v1/categories/1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.attributes.length()").value(3))
                .andExpect(jsonPath("$.attributes[0].code").value("brand"))
                .andExpect(jsonPath("$.attributes[0].required").value(true));
    }

    @Test
    void createsPersistsAndRetrievesListing() throws Exception {
        var response = mvc.perform(post("/api/v1/listings").contentType(MediaType.APPLICATION_JSON).content(VALID_LISTING))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sellerId").value(1))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.price").value(1250.50))
                .andExpect(jsonPath("$.attributes.brand").value("Lenovo"))
                .andExpect(jsonPath("$.attributes.original_packaging").value(false))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andReturn().getResponse();
        long id = objectMapper.readTree(response.getContentAsString()).get("id").longValue();
        assertThat(response.getHeader("Location")).isEqualTo("/api/v1/listings/" + id);
        assertThat(dsl.fetchCount(LISTINGS)).isEqualTo(1);
        mvc.perform(get(response.getHeader("Location")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.attributes.year").value(2022));
        mvc.perform(get("/api/v1/listings"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id));
        mvc.perform(get("/api/v1/listings?limit=1&offset=1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void auctionMetadataAllowsNoPriceWithoutImplementingBidding() throws Exception {
        var body = VALID_LISTING.replace("FIXED_PRICE", "AUCTION").replace("1250.50", "null");
        mvc.perform(post("/api/v1/listings").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.listingType").value("AUCTION"))
                .andExpect(jsonPath("$.price").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/categories/999999", "/api/v1/listings/999999"})
    void missingResourcesReturn404(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/listings?limit=101", "/api/v1/listings?limit=0",
            "/api/v1/listings?offset=-1", "/api/v1/listings/0", "/api/v1/categories/-1"})
    void invalidParametersReturn400(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{", "{\"listingType\":\"INVALID\"}"})
    void malformedOrIncompleteRequestsReturn400(String body) throws Exception {
        mvc.perform(post("/api/v1/listings").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        assertThat(dsl.fetchCount(LISTINGS)).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"blankTitle", "negativePrice", "fractionalPrice", "missingPrice",
            "missingBrand", "wrongAttributeType", "unknownAttribute", "unknownCurrency", "blankBrand"})
    void invalidListingCannotBePersisted(String scenario) throws Exception {
        var body = objectMapper.readTree(VALID_LISTING).deepCopy();
        var listing = (com.fasterxml.jackson.databind.node.ObjectNode) body;
        var attributes = (com.fasterxml.jackson.databind.node.ObjectNode) listing.get("attributes");
        switch (scenario) {
            case "blankTitle" -> listing.put("title", "  ");
            case "negativePrice" -> listing.put("price", -1);
            case "fractionalPrice" -> listing.put("price", new java.math.BigDecimal("1.001"));
            case "missingPrice" -> listing.remove("price");
            case "missingBrand" -> attributes.remove("brand");
            case "wrongAttributeType" -> attributes.put("year", "2022");
            case "unknownAttribute" -> attributes.put("unknown", true);
            case "unknownCurrency" -> listing.put("currency", "ZZZ");
            case "blankBrand" -> attributes.put("brand", " ");
            default -> throw new IllegalArgumentException(scenario);
        }
        mvc.perform(post("/api/v1/listings").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        assertThat(dsl.fetchCount(LISTINGS)).isZero();
    }

    @Test
    void unknownCategoryDoesNotCreateListing() throws Exception {
        mvc.perform(post("/api/v1/listings").contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_LISTING.replace("\"categoryId\":1", "\"categoryId\":999999")))
                .andExpect(status().isNotFound());
        assertThat(dsl.fetchCount(LISTINGS)).isZero();
    }
}
