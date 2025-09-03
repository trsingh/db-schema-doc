package com.finzly.dbschemadoc.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for Database Schema Documentation API.
 * Provides comprehensive API documentation with detailed endpoint descriptions.
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                    new Server()
                        .url("http://localhost:" + serverPort)
                        .description("Local Development Server"),
                    new Server()
                        .url("https://api.finzly.com")
                        .description("Production Server")
                ))
                .tags(List.of(
                    new Tag()
                        .name("Schema Reports")
                        .description("Generate comprehensive database schema reports in multiple formats"),
                    new Tag()
                        .name("Database Metadata")
                        .description("Retrieve detailed database metadata including schemas, tables, columns, and constraints")
                ));
    }

    private Info apiInfo() {
        return new Info()
                .title("Database Schema Documentation API")
                .description(
                    "Comprehensive database schema extraction and reporting API. " +
                    "Extract complete database metadata including tables, columns, keys, and constraints. " +
                    "Generate reports in multiple formats: JSON for API integration, HTML for web viewing, " +
                    "and DOCX for professional documentation. All operations are read-only and safe."
                )
                .version("1.0.0")
                .contact(new Contact()
                    .name("Finzly Development Team")
                    .email("support@finzly.com")
                    .url("https://www.finzly.com"))
                .license(new License()
                    .name("MIT License")
                    .url("https://opensource.org/licenses/MIT"));
    }
}
