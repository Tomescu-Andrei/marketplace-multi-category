package com.marketplace.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
        var components = new Components().addSchemas("ApiProblem", problem);
        components.addResponses("BadRequest", problemResponse("Invalid request body, parameter or listing attributes"));
        components.addResponses("NotFound", problemResponse("The requested category or listing does not exist"));
        components.addResponses("Conflict", problemResponse("The operation conflicts with existing data"));
        components.addResponses("InternalError", problemResponse("Unexpected error; internal details are not returned"));
        return new OpenAPI().info(new Info().title("Multi-Category Marketplace API").version("v1")
                        .description("M1 backend. Uses a predefined local test seller; no authentication is required. "
                                + "AUCTION stores metadata only; bidding is not implemented."))
                .components(components);
    }

    @Bean
    OpenApiCustomizer errorResponses() {
        return api -> api.getPaths().forEach((path, item) -> item.readOperationsMap().forEach((method, operation) -> {
            var responses = operation.getResponses();
            responses.addApiResponse("500", new ApiResponse().$ref("#/components/responses/InternalError"));
            if (path.contains("{id}") || path.equals("/api/v1/listings")) {
                responses.addApiResponse("400", new ApiResponse().$ref("#/components/responses/BadRequest"));
            }
            if (path.contains("{id}") || method == PathItem.HttpMethod.POST) {
                responses.addApiResponse("404", new ApiResponse().$ref("#/components/responses/NotFound"));
            }
            if (method == PathItem.HttpMethod.POST) {
                responses.addApiResponse("409", new ApiResponse().$ref("#/components/responses/Conflict"));
            }
        }));
    }

    private ApiResponse problemResponse(String description) {
        return new ApiResponse().description(description).content(new Content().addMediaType("application/problem+json",
                new MediaType().schema(new Schema<>().$ref("#/components/schemas/ApiProblem"))));
    }
}
