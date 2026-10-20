package com.ZyroTrade.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springdoc.core.customizers.OpenApiCustomizer;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;

@Configuration
@OpenAPIDefinition(info = @Info(title = "ZyroTrade API", version = "v1",
        description = "Stock trading simulator API. Monetary JSON values are decimal numbers with four-decimal HALF_EVEN business rounding. Timestamps include an explicit offset. Orders execute synchronously at the latest accepted quote; the response status is EXECUTED. There are no pending or cancellable orders."))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
    @Bean
    OpenApiCustomizer commonErrors() {
        return openApi -> {
            Schema<Object> error = new Schema<>();
            error.type("object");
            error.addProperty("timestamp", new StringSchema().format("date-time"));
            error.addProperty("status", new Schema<>().type("integer"));
            error.addProperty("code", new StringSchema());
            error.addProperty("message", new StringSchema());
            Schema<Object> details = new Schema<>();
            details.type("object");
            details.setAdditionalProperties(new StringSchema());
            error.addProperty("details", details);
            openApi.getComponents().addSchemas("ApiError", error);
            openApi.getPaths().values().forEach(path -> path.readOperations().forEach(operation -> {
                addError(operation.getResponses(), "400", "Validation or request error", errorContent());
                addError(operation.getResponses(), "401", "Missing or invalid authentication", errorContent());
                addError(operation.getResponses(), "403", "Role is not permitted", errorContent());
                addError(operation.getResponses(), "404", "Resource not found or not owned by caller", errorContent());
                addError(operation.getResponses(), "409", "Financial or idempotency conflict", errorContent());
                addError(operation.getResponses(), "503", "Market data provider is unavailable", errorContent());
                addError(operation.getResponses(), "500", "Unexpected server failure", errorContent());
            }));
        };
    }

    private Content errorContent() {
        return new Content().addMediaType("application/json",
                new MediaType().schema(new Schema<>().$ref("#/components/schemas/ApiError")));
    }

    private void addError(io.swagger.v3.oas.models.responses.ApiResponses responses, String code,
                          String description, Content content) {
        if (!responses.containsKey(code)) responses.addApiResponse(code,
                new ApiResponse().description(description).content(content));
    }
}
