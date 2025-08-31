package com.finzly.dbschemadoc.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
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
                ));
    }

    private Info apiInfo() {
        return new Info()
                .title("Database Schema Documentation API")
                .description("""
                    **Database Schema Documentation API** provides comprehensive database schema extraction and reporting capabilities.
                    
                    ### Features:
                    - **Complete Schema Extraction**: Tables, columns, primary keys, foreign keys, and metadata
                    - **Multiple Export Formats**: JSON (API), HTML (Web), DOCX (Document)
                    - **Read-Only Operations**: Safe metadata queries with no database modifications
                    - **Rich Documentation**: External descriptions and business-friendly formatting
                    
                    ### Use Cases:
                    - **API Integration**: JSON format for system integration and automation
                    - **Documentation**: HTML and DOCX formats for human-readable documentation
                    - **Database Analysis**: Comprehensive schema information for development teams
                    - **Compliance Reporting**: Professional documentation for audits and compliance
                    
                    ### Security:
                    - Database connections are configured as read-only
                    - No DDL or DML operations are performed
                    - Only metadata queries are executed
                    
                    ### Getting Started:
                    1. Use `/schema/json` for programmatic access
                    2. Use `/schema/html` for web-based viewing
                    3. Use `/schema/docx` for downloadable documentation
                    """)
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
