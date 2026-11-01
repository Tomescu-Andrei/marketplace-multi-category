package com.marketplace.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI marketplaceOpenApi() {
        var error = new ObjectSchema()
                .addProperty("field", new StringSchema().example("price"))
                .addProperty("message", new StringSchema().example("A fixed-price listing requires a price"));
        var problem = new ObjectSchema()
                .addProperty("type", new StringSchema().format("uri").example("about:blank"))
                .addProperty("title", new StringSchema().example("Bad Request"))
                .addProperty("status", new IntegerSchema().example(400))
                .addProperty("detail", new StringSchema().example("Request validation failed"))
                .addProperty("instance", new StringSchema().example("/api/v1/listings"))
                .addProperty("code", new StringSchema().example("VALIDATION_FAILED"))
                .addProperty("errors", new ArraySchema().items(error));
        var components = new Components().addSchemas("ApiProblem", problem)
                .addSecuritySchemes("sessionCookie", new SecurityScheme().type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.COOKIE).name("JSESSIONID"));
        components.addResponses("BadRequest", problemResponse(400, "VALIDATION_FAILED", "Invalid request body, parameter or listing attributes"));
        components.addResponses("NotFound", problemResponse(404, "NOT_FOUND", "The requested category or listing does not exist"));
        components.addResponses("Conflict", problemResponse(409, "DATA_CONFLICT", "The operation conflicts with existing data"));
        components.addResponses("InternalError", problemResponse(500, "INTERNAL_ERROR", "An unexpected error occurred"));
        components.addResponses("Unauthorized", problemResponse(401, "UNAUTHENTICATED", "Authentication required or credentials invalid"));
        components.addResponses("Forbidden", problemResponse(403, "INVALID_CSRF_TOKEN", "CSRF token missing or invalid"));
        return new OpenAPI().info(new Info().title("Multi-Category Marketplace API").version("v1")
                        .description("M2 authentication foundation. Register and log in using a session cookie. "
                                + "Fetch /api/v1/auth/csrf before POST requests and again after login. "
                                + "AUCTION stores metadata only; bidding is not implemented."))
                .components(components);
    }

    @Bean
    OpenApiCustomizer errorResponses() {
        return api -> api.getPaths().forEach((path, item) -> item.readOperationsMap().forEach((method, operation) -> {
            var responses = operation.getResponses();
            responses.addApiResponse("500", new ApiResponse().$ref("#/components/responses/InternalError"));
            if (path.contains("{id}") || path.equals("/api/v1/listings") || method == PathItem.HttpMethod.POST) {
                responses.addApiResponse("400", new ApiResponse().$ref("#/components/responses/BadRequest"));
            }
            if (path.contains("{id}") || (path.equals("/api/v1/listings") && method == PathItem.HttpMethod.POST)) {
                responses.addApiResponse("404", new ApiResponse().$ref("#/components/responses/NotFound"));
            }
            if (method == PathItem.HttpMethod.POST) {
                if (!path.endsWith("/logout")) {
                    responses.addApiResponse("409", new ApiResponse().$ref("#/components/responses/Conflict"));
                }
                responses.addApiResponse("403", new ApiResponse().$ref("#/components/responses/Forbidden"));
                operation.addParametersItem(new Parameter().in("header").name("X-CSRF-TOKEN").required(true)
                        .description("Token returned by GET /api/v1/auth/csrf in this browser session; renew after login/logout")
                        .schema(new StringSchema()));
            }
            if ((path.equals("/api/v1/listings") && method == PathItem.HttpMethod.POST) || path.endsWith("/me")) {
                operation.addSecurityItem(new SecurityRequirement().addList("sessionCookie"));
                responses.addApiResponse("401", new ApiResponse().$ref("#/components/responses/Unauthorized"));
            }
            if (path.endsWith("/login")) {
                responses.addApiResponse("401", new ApiResponse().$ref("#/components/responses/Unauthorized"));
            }
        }));
    }

    private ApiResponse problemResponse(int status, String code, String description) {
        return new ApiResponse().description(description).content(new Content().addMediaType("application/problem+json",
                new MediaType().schema(new Schema<>().$ref("#/components/schemas/ApiProblem"))
                        .example(Map.of("type", "about:blank", "title", HttpStatus.valueOf(status).getReasonPhrase(),
                                "status", status, "detail", description, "instance", "/api/v1/listings", "code", code, "errors", List.of()))));
    }
}
