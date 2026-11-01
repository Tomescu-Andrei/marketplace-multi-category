package com.marketplace;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static com.marketplace.jooq.Tables.LISTINGS;
import static com.marketplace.jooq.Tables.USERS;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureMockMvc
@ActiveProfiles("local")
@WithMockUser(username = "seller@marketplace.test")
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

    @Autowired
    private PasswordEncoder passwordEncoder;

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
    void healthChecksDatabaseWithoutExposingDetails() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
    }

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
        assertThat(flyway.info().applied()).hasSize(5);
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
        var response = mvc.perform(post("/api/v1/listings").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(VALID_LISTING))
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
        mvc.perform(post("/api/v1/listings").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
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
        mvc.perform(get(path)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
                .andExpect(jsonPath("$.errors[0].field").isNotEmpty())
                .andExpect(jsonPath("$.instance").value(path.split("\\?")[0]));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{", "{\"listingType\":\"INVALID\"}"})
    void malformedOrIncompleteRequestsReturn400(String body) throws Exception {
        mvc.perform(post("/api/v1/listings").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
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
        mvc.perform(post("/api/v1/listings").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        assertThat(dsl.fetchCount(LISTINGS)).isZero();
    }

    @Test
    void unknownCategoryDoesNotCreateListing() throws Exception {
        mvc.perform(post("/api/v1/listings").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_LISTING.replace("\"categoryId\":1", "\"categoryId\":999999")))
                .andExpect(status().isNotFound());
        assertThat(dsl.fetchCount(LISTINGS)).isZero();
    }

    @Test
    void validationErrorsIdentifyFieldsWithoutExposingRejectedValues() throws Exception {
        mvc.perform(post("/api/v1/listings").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.instance").value("/api/v1/listings"))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("categoryId"))
                .andExpect(jsonPath("$.errors[0].message").isNotEmpty())
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void businessErrorsIdentifyTheAttribute() throws Exception {
        mvc.perform(post("/api/v1/listings").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_LISTING.replace("\"year\":2022", "\"year\":\"2022\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_LISTING"))
                .andExpect(jsonPath("$.errors[0].field").value("attributes.year"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"fractionalId", "numericEnum", "unknownField", "trailingJson", "duplicateKey"})
    void ambiguousJsonIsRejected(String scenario) throws Exception {
        String body = switch (scenario) {
            case "fractionalId" -> VALID_LISTING.replace("\"categoryId\":1", "\"categoryId\":1.5");
            case "numericEnum" -> VALID_LISTING.replace("\"FIXED_PRICE\"", "0");
            case "unknownField" -> VALID_LISTING.replace("\"categoryId\":1", "\"sellerId\":123,\"categoryId\":1");
            case "trailingJson" -> VALID_LISTING + "{}";
            case "duplicateKey" -> VALID_LISTING.replace("\"categoryId\":1", "\"categoryId\":2,\"categoryId\":1");
            default -> throw new IllegalArgumentException(scenario);
        };
        mvc.perform(post("/api/v1/listings").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_JSON"))
                .andExpect(jsonPath("$.errors").isArray());
        assertThat(dsl.fetchCount(LISTINGS)).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/categories/abc", "/api/v1/listings?limit=abc"})
    void invalidParameterTypesHaveConsistentErrors(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
                .andExpect(jsonPath("$.errors[0].message").isNotEmpty());
    }

    @Test
    void unsupportedMethodsAndContentTypesKeepHttpSemantics() throws Exception {
        mvc.perform(put("/api/v1/listings/1").with(csrf()))
                .andExpect(status().isMethodNotAllowed()).andExpect(header().exists("Allow"))
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"))
                .andExpect(jsonPath("$.errors").isArray());
        mvc.perform(post("/api/v1/listings").with(csrf()).contentType(MediaType.TEXT_PLAIN).content(VALID_LISTING))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
        mvc.perform(get("/api/v1/missing"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"nullBrand", "nestedBrand", "textBoolean", "longTitle", "longDescription",
            "longBrand", "priceOverflow", "tooManyAttributes"})
    void rejectsBoundaryAndAttributeViolations(String scenario) throws Exception {
        var body = (com.fasterxml.jackson.databind.node.ObjectNode) objectMapper.readTree(VALID_LISTING);
        var attributes = (com.fasterxml.jackson.databind.node.ObjectNode) body.get("attributes");
        switch (scenario) {
            case "nullBrand" -> attributes.putNull("brand");
            case "nestedBrand" -> attributes.putObject("brand").put("name", "Lenovo");
            case "textBoolean" -> attributes.put("original_packaging", "false");
            case "longTitle" -> body.put("title", "x".repeat(201));
            case "longDescription" -> body.put("description", "x".repeat(10001));
            case "longBrand" -> attributes.put("brand", "x".repeat(1001));
            case "priceOverflow" -> body.put("price", new BigDecimal("1000000000000.00"));
            case "tooManyAttributes" -> {
                for (int index = 0; index < 101; index++) { attributes.put("extra_" + index, true); }
            }
            default -> throw new IllegalArgumentException(scenario);
        }
        mvc.perform(post("/api/v1/listings").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body.toString()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors[0].field").isNotEmpty());
        assertThat(dsl.fetchCount(LISTINGS)).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.01", "999999999999.99"})
    void acceptsMoneyBoundariesWithoutPrecisionLoss(String price) throws Exception {
        var body = (com.fasterxml.jackson.databind.node.ObjectNode) objectMapper.readTree(VALID_LISTING);
        body.put("price", new BigDecimal(price));
        var response = mvc.perform(post("/api/v1/listings").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body.toString()))
                .andExpect(status().isCreated()).andReturn().getResponse();
        long id = objectMapper.readTree(response.getContentAsString()).get("id").longValue();
        assertThat(dsl.select(LISTINGS.PRICE).from(LISTINGS).where(LISTINGS.ID.eq(id)).fetchSingle(LISTINGS.PRICE))
                .isEqualByComparingTo(price);
    }

    @Test
    void categorySpecificDefinitionsAndOptionalAttributesAreRespected() throws Exception {
        String book = """
                {"categoryId":2,"title":"Book","description":"Used book","listingType":"FIXED_PRICE",
                 "price":15,"currency":"EUR","attributes":{"author":"Test author"}}
                """;
        mvc.perform(post("/api/v1/listings").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(book))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.attributes.author").value("Test author"));
        mvc.perform(post("/api/v1/listings").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(book.replace("\"author\":\"Test author\"", "\"brand\":\"Lenovo\"")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_LISTING"));
        var electronics = (com.fasterxml.jackson.databind.node.ObjectNode) objectMapper.readTree(VALID_LISTING);
        var attributes = (com.fasterxml.jackson.databind.node.ObjectNode) electronics.get("attributes");
        attributes.remove("year");
        attributes.putNull("original_packaging");
        mvc.perform(post("/api/v1/listings").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(electronics.toString()))
                .andExpect(status().isCreated());
        assertThat(dsl.fetchCount(LISTINGS)).isEqualTo(2);
    }

    @Test
    void paginationIsStableWhenCreationTimestampsMatch() throws Exception {
        for (int index = 0; index < 3; index++) {
            mvc.perform(post("/api/v1/listings").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                    .content(VALID_LISTING.replace("Used laptop", "Laptop " + index))).andExpect(status().isCreated());
        }
        dsl.update(LISTINGS).set(LISTINGS.CREATED_AT, java.time.OffsetDateTime.parse("2026-11-01T00:00:00Z")).execute();
        mvc.perform(get("/api/v1/listings?limit=2&offset=0"))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Laptop 2"))
                .andExpect(jsonPath("$[1].title").value("Laptop 1"));
        mvc.perform(get("/api/v1/listings?limit=2&offset=2"))
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].title").value("Laptop 0"));
    }

    @ParameterizedTest
    @CsvSource(value = {"null|1|23514", "-1|1|23514", "10|999999|23503"}, delimiter = '|', nullValues = "null")
    void databaseEnforcesPriceAndSellerConstraints(String price, long sellerId, String sqlState) throws SQLException {
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement("""
                INSERT INTO listings (seller_id, category_id, title, description, listing_type, price, currency)
                VALUES (?, 1, 'Constraint test', 'Test', 'FIXED_PRICE', ?, 'RON')
                """)) {
            statement.setLong(1, sellerId);
            statement.setBigDecimal(2, price == null ? null : new BigDecimal(price));
            assertThatExceptionOfType(SQLException.class).isThrownBy(statement::executeUpdate)
                    .satisfies(exception -> assertThat(exception.getSQLState()).isEqualTo(sqlState));
        }
        assertThat(dsl.fetchCount(LISTINGS)).isZero();
    }

    @Test
    void openApiDescribesAllOperationsAndCreationExample() throws Exception {
        var response = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        var api = objectMapper.readTree(response);
        assertThat(api.at("/info/title").asText()).isEqualTo("Multi-Category Marketplace API");
        assertThat(api.get("paths").size()).isEqualTo(9);
        var paths = api.get("paths");
        assertThat(paths.get("/api/v1/categories").has("get")).isTrue();
        assertThat(paths.get("/api/v1/categories/{id}").has("get")).isTrue();
        assertThat(paths.get("/api/v1/listings/{id}").has("get")).isTrue();
        assertThat(paths.get("/api/v1/listings").has("get")).isTrue();
        var create = paths.get("/api/v1/listings").get("post");
        assertThat(create.at("/responses/201/headers/Location").isMissingNode()).isFalse();
        assertThat(create.at("/responses/400/$ref").asText()).isEqualTo("#/components/responses/BadRequest");
        assertThat(create.at("/responses/404/$ref").asText()).isEqualTo("#/components/responses/NotFound");
        assertThat(create.at("/requestBody/content/application~1json/examples").isEmpty()).isFalse();
        assertThat(api.at("/components/schemas/ApiProblem/properties/errors/type").asText()).isEqualTo("array");
        assertThat(api.at("/components/responses/InternalError/content/application~1problem+json/example/status").asInt()).isEqualTo(500);
        assertThat(paths.get("/api/v1/auth/register").at("/post/responses/201").isMissingNode()).isFalse();
        assertThat(paths.get("/api/v1/auth/logout").at("/post/responses/204").isMissingNode()).isFalse();
        assertThat(paths.get("/api/v1/auth/me").at("/get/responses/401/$ref").asText()).isEqualTo("#/components/responses/Unauthorized");
    }

    @Test
    void swaggerUiAndItsAssetsAreServed() throws Exception {
        mvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/swagger-ui/index.html"));
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("swagger-ui")));
        mvc.perform(get("/swagger-ui/swagger-ui-bundle.js")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs/swagger-config")).andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("/v3/api-docs"));
    }

    @Test
    @WithAnonymousUser
    void registrationLoginOwnershipAndLogoutWorkWithRealCsrfTokens() throws Exception {
        // This flow exercises the real saved session, not the class-level mock security context.
        org.springframework.security.test.context.TestSecurityContextHolder.clearContext();
        var csrfResult = mvc.perform(get("/api/v1/auth/csrf")).andExpect(status().isOk()).andReturn();
        var session = (MockHttpSession) csrfResult.getRequest().getSession(false);
        String previousSessionId = session.getId();
        var token = objectMapper.readTree(csrfResult.getResponse().getContentAsString());
        String email = "seller-" + java.util.UUID.randomUUID() + "@example.com";
        String password = "A-long-password-2026!";
        String username = "seller_" + java.util.UUID.randomUUID().toString().substring(0, 8);
        String registration = objectMapper.writeValueAsString(java.util.Map.of("email", email.toUpperCase(),
                "username", username, "password", password, "displayName", " New Seller "));
        var register = mvc.perform(post("/api/v1/auth/register").session(session)
                        .header(token.get("headerName").asText(), token.get("token").asText())
                        .contentType(MediaType.APPLICATION_JSON).content(registration))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.displayName").value("New Seller"))
                .andExpect(jsonPath("$.password").doesNotExist()).andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn();
        long userId = objectMapper.readTree(register.getResponse().getContentAsString()).get("id").longValue();
        String hash = dsl.select(USERS.PASSWORD_HASH).from(USERS).where(USERS.ID.eq(userId)).fetchSingle(USERS.PASSWORD_HASH);
        assertThat(hash).isNotEqualTo(password).startsWith("$2a$12$");
        assertThat(passwordEncoder.matches(password, hash)).isTrue();
        String login = objectMapper.writeValueAsString(java.util.Map.of("email", email, "password", password));
        mvc.perform(post("/api/v1/auth/login").session(session)
                        .header(token.get("headerName").asText(), token.get("token").asText())
                        .contentType(MediaType.APPLICATION_JSON).content(login))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(userId));
        assertThat(session.getId()).isNotEqualTo(previousSessionId);
        mvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(userId));
        var listingRequest = post("/api/v1/listings").session(session).contentType(MediaType.APPLICATION_JSON).content(VALID_LISTING);
        mvc.perform(listingRequest.header(token.get("headerName").asText(), token.get("token").asText()))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("INVALID_CSRF_TOKEN"));
        var renewed = mvc.perform(get("/api/v1/auth/csrf").session(session)).andExpect(status().isOk()).andReturn();
        token = objectMapper.readTree(renewed.getResponse().getContentAsString());
        mvc.perform(post("/api/v1/listings").session(session)
                        .header(token.get("headerName").asText(), token.get("token").asText())
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_LISTING))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.sellerId").value(userId));
        mvc.perform(post("/api/v1/auth/logout").session(session)
                        .header(token.get("headerName").asText(), token.get("token").asText()))
                .andExpect(status().isNoContent());
        assertThat(session.isInvalid()).isTrue();
        mvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithAnonymousUser
    void publicReadsRemainAvailableButCreationRequiresAuthenticationAndCsrf() throws Exception {
        mvc.perform(get("/api/v1/categories")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/listings")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        mvc.perform(post("/api/v1/listings").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(VALID_LISTING))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/listings").contentType(MediaType.APPLICATION_JSON).content(VALID_LISTING))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("INVALID_CSRF_TOKEN"));
        assertThat(dsl.fetchCount(LISTINGS)).isZero();
    }

    @Test
    @WithAnonymousUser
    void duplicateEmailIsCaseInsensitiveAndPasswordsAreNotReturned() throws Exception {
        String email = "duplicate-" + java.util.UUID.randomUUID() + "@example.com";
        String username = "duplicate_" + java.util.UUID.randomUUID().toString().substring(0, 8);
        var body = objectMapper.writeValueAsString(java.util.Map.of("email", email,
                "username", username, "password", "Another-long-password!", "displayName", "Seller"));
        mvc.perform(post("/api/v1/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body.replace(email, email.toUpperCase()).replace(username, username + "x")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("DATA_CONFLICT"));
    }

    @ParameterizedTest
    @WithAnonymousUser
    @ValueSource(strings = {"unknown", "legacy", "wrongPassword", "tooManyPasswordBytes"})
    void loginFailuresUseGeneric401(String scenario) throws Exception {
        String email = "absent@example.com";
        String password = "Incorrect-password!";
        if (scenario.equals("legacy")) {
            email = "seller@marketplace.test";
        }
        if (scenario.equals("tooManyPasswordBytes")) {
            password = "é".repeat(50);
        }
        if (scenario.equals("wrongPassword")) {
            email = "wrong-password-" + java.util.UUID.randomUUID() + "@example.com";
            var registration = objectMapper.writeValueAsString(java.util.Map.of("email", email,
                    "username", "wrong_" + java.util.UUID.randomUUID().toString().substring(0, 8),
                    "password", "Correct-password-2026!", "displayName", "Seller"));
            mvc.perform(post("/api/v1/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(registration))
                    .andExpect(status().isCreated());
        }
        var body = objectMapper.writeValueAsString(java.util.Map.of("email", email, "password", password));
        mvc.perform(post("/api/v1/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.detail").value("Invalid credentials or authentication required"));
    }

    @ParameterizedTest
    @WithAnonymousUser
    @ValueSource(strings = {"invalidEmail", "shortPassword", "unicodePassword", "blankName", "extraRole", "invalidUsername", "missingUsername"})
    void invalidRegistrationDoesNotCreateAnAccount(String scenario) throws Exception {
        String email = "invalid-" + java.util.UUID.randomUUID() + "@example.com";
        var body = objectMapper.createObjectNode().put("email", email).put("password", "A-valid-password-2026!")
                .put("displayName", "Seller").put("username", "invalid_" + java.util.UUID.randomUUID().toString().substring(0, 8));
        switch (scenario) {
            case "invalidEmail" -> body.put("email", "not-an-email");
            case "shortPassword" -> body.put("password", "short");
            case "unicodePassword" -> body.put("password", "é".repeat(50));
            case "blankName" -> body.put("displayName", " ");
            case "extraRole" -> body.put("role", "ADMIN");
            case "invalidUsername" -> body.put("username", "someone@example.com");
            case "missingUsername" -> body.remove("username");
            default -> throw new IllegalArgumentException(scenario);
        }
        int count = dsl.fetchCount(USERS);
        mvc.perform(post("/api/v1/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body.toString()))
                .andExpect(status().isBadRequest());
        assertThat(dsl.fetchCount(USERS)).isEqualTo(count);
    }

    @Test
    @WithAnonymousUser
    void tokensCannotBeReusedInAnotherSession() throws Exception {
        var response = mvc.perform(get("/api/v1/auth/csrf")).andExpect(status().isOk()).andReturn();
        var token = objectMapper.readTree(response.getResponse().getContentAsString());
        mvc.perform(post("/api/v1/auth/login").session(new MockHttpSession())
                        .header(token.get("headerName").asText(), token.get("token").asText())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"user@example.com\",\"password\":\"password\"}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("INVALID_CSRF_TOKEN"));
    }

    @ParameterizedTest
    @WithAnonymousUser
    @ValueSource(strings = {"username", "email", "uppercaseUsername", "spacedUsername"})
    void logsInWithUsernameOrEmail(String mode) throws Exception {
        String username = "login_" + java.util.UUID.randomUUID().toString().substring(0, 8);
        String email = username + "@example.test";
        String password = "Username-password-2026!";
        var registration = objectMapper.writeValueAsString(java.util.Map.of("username", username.toUpperCase(),
                "email", email, "password", password, "displayName", "Username Test"));
        mvc.perform(post("/api/v1/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(registration))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.username").value(username));
        String identifier = switch (mode) {
            case "email" -> email.toUpperCase();
            case "uppercaseUsername" -> username.toUpperCase();
            case "spacedUsername" -> "  " + username + "  ";
            default -> username;
        };
        var login = objectMapper.writeValueAsString(java.util.Map.of("identifier", identifier, "password", password));
        mvc.perform(post("/api/v1/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(login))
                .andExpect(status().isOk()).andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.email").value(email)).andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @WithAnonymousUser
    void usernameIsUniqueIgnoringCaseEvenForDifferentEmails() throws Exception {
        String username = "unique_" + java.util.UUID.randomUUID().toString().substring(0, 8);
        var registration = objectMapper.createObjectNode().put("username", username).put("email", username + "@example.test")
                .put("password", "Username-password-2026!").put("displayName", "Seller");
        mvc.perform(post("/api/v1/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(registration.toString()))
                .andExpect(status().isCreated());
        registration.put("username", username.toUpperCase()).put("email", username + "-other@example.test");
        mvc.perform(post("/api/v1/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(registration.toString()))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("DATA_CONFLICT"));
    }

    @Test
    void migrationAssignsUsernameToExistingM1Seller() {
        assertThat(dsl.select(USERS.USERNAME).from(USERS).where(USERS.EMAIL.eq("seller@marketplace.test"))
                .fetchSingle(USERS.USERNAME)).isEqualTo("user_1");
    }
}
