package com.finzly.dbschemadoc.controller;

import com.finzly.dbschemadoc.model.DatabaseSchema;
import com.finzly.dbschemadoc.service.DatabaseSchemaService;
import com.finzly.dbschemadoc.service.DocxReportService;
import com.finzly.dbschemadoc.service.HtmlReportService;
import com.finzly.dbschemadoc.service.DdlGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * REST Controller for schema reporting endpoints.
 * Provides different formats for database schema export.
 */
@RestController
@RequestMapping("/schema")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Schema Reports", description = "Generate comprehensive database schema reports in multiple formats")
public class SchemaController {

    private final DatabaseSchemaService databaseSchemaService;
    private final HtmlReportService htmlReportService;
    private final DocxReportService docxReportService;
    private final DdlGenerationService ddlGenerationService;

    @GetMapping(value = "/json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Get complete database schema as JSON",
        description = "Retrieve the entire database schema including tables, columns, primary keys, foreign keys, and metadata in JSON format. This endpoint provides a machine-readable representation of the database structure suitable for API consumption and integration.",
        operationId = "getSchemaAsJson"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved database schema",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = DatabaseSchema.class),
                examples = @ExampleObject(
                    name = "Database Schema Example",
                    description = "Example database schema response",
                    value = "{\"databaseName\":\"galaxy_compliance_mcbankny\",\"databaseProductName\":\"MySQL\",\"userName\":\"root\",\"tables\":[{\"tableName\":\"users\",\"tableType\":\"TABLE\",\"columns\":[{\"columnName\":\"id\",\"dataType\":\"BIGINT\",\"nullable\":false,\"isPrimaryKey\":true}]}]}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error - Database connection or processing failure",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = @ExampleObject(
                    name = "Error Response",
                    value = "{\"error\": \"Database connection failed\", \"timestamp\": \"2023-12-01T10:00:00Z\"}"
                )
            )
        )
    })
    public ResponseEntity<DatabaseSchema> getSchemaAsJson(
            @Parameter(description = "Schema name (optional, uses default if not provided)", example = "galaxy_compliance_mcbankny")
            @RequestParam(required = false) String schema) {
        log.info("Generating JSON schema report for schema: {}", schema);
        DatabaseSchema databaseSchema = databaseSchemaService.fetchSchema(schema);
        return ResponseEntity.ok(databaseSchema);
    }

    @GetMapping(value = "/html", produces = MediaType.TEXT_HTML_VALUE)
    @Operation(
        summary = "Get complete database schema as HTML",
        description = "Generate and retrieve the entire database schema as a human-readable HTML report. The HTML includes styled tables, column details, relationships, and is suitable for documentation or browser viewing.",
        operationId = "getSchemaAsHtml"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully generated HTML schema report",
            content = @Content(
                mediaType = MediaType.TEXT_HTML_VALUE,
                examples = @ExampleObject(
                    name = "HTML Schema Report",
                    description = "HTML formatted database schema report",
                    value = "<!DOCTYPE html><html><head><title>Database Schema Report</title></head><body><h1>Database Schema</h1><h2>Tables</h2><table><tr><th>Column</th><th>Type</th><th>Constraints</th></tr></table></body></html>"
                )
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error - Failed to generate HTML report",
            content = @Content(mediaType = MediaType.TEXT_HTML_VALUE)
        )
    })
    public ResponseEntity<String> getSchemaAsHtml(
            @Parameter(description = "Schema name (optional, uses default if not provided)", example = "galaxy_compliance_mcbankny")
            @RequestParam(required = false) String schema) {
        log.info("Generating HTML schema report for schema: {}", schema);
        DatabaseSchema databaseSchema = databaseSchemaService.fetchSchema(schema);
        String htmlReport = htmlReportService.generateHtmlReport(databaseSchema);
        
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(htmlReport);
    }

    @GetMapping(value = "/docx", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Operation(
        summary = "Get complete database schema as DOCX",
        description = "Generate and download the entire database schema as a Microsoft Word (DOCX) document. The document includes professionally formatted tables, comprehensive schema information, and is suitable for offline documentation or reporting purposes.",
        operationId = "getSchemaAsDocx"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully generated and returned DOCX schema report",
            content = @Content(
                mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                schema = @Schema(type = "string", format = "binary", description = "DOCX file content as binary data")
            ),
            headers = {
                @io.swagger.v3.oas.annotations.headers.Header(
                    name = "Content-Disposition",
                    description = "Attachment header with filename",
                    schema = @Schema(type = "string", example = "attachment; filename=\"database_schema_mydb_1638360000000.docx\"")
                ),
                @io.swagger.v3.oas.annotations.headers.Header(
                    name = "Content-Length",
                    description = "Size of the DOCX file in bytes",
                    schema = @Schema(type = "integer", example = "25600")
                )
            }
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error - Failed to generate DOCX report",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
        )
    })
    public ResponseEntity<byte[]> getSchemaAsDocx(
            @Parameter(description = "Schema name (optional, uses default if not provided)", example = "galaxy_compliance_mcbankny")
            @RequestParam(required = false) String schema) throws IOException {
        log.info("Generating DOCX schema report for schema: {}", schema);
        DatabaseSchema databaseSchema = databaseSchemaService.fetchSchema(schema);
        byte[] docxReport = docxReportService.generateDocxReport(databaseSchema);
        
        String filename = "database_schema_" + (schema != null ? schema : "default") + "_" + getCurrentTimestamp() + ".docx";
        
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(docxReport);
    }

    @GetMapping(value = "/ddl", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(
        summary = "Get complete database schema as DDL script",
        description = "Generate and retrieve the entire database schema as DDL (Data Definition Language) script. The script includes CREATE TABLE statements with columns, primary keys, foreign keys, and constraints to recreate the complete database structure. This is useful for database migration, backup, or replication purposes.",
        operationId = "getSchemaAsDdl"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully generated DDL script",
            content = @Content(
                mediaType = MediaType.TEXT_PLAIN_VALUE,
                examples = @ExampleObject(
                    name = "DDL Script Example",
                    description = "Example DDL script output",
                    value = "-- DDL Script for Database: galaxy_compliance_mcbankny\n-- Generated from MySQL 8.0.28\n\nCREATE TABLE users (\n    id BIGINT NOT NULL,\n    username VARCHAR(50) NOT NULL,\n    email VARCHAR(100),\n    PRIMARY KEY (id)\n) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;\n\n-- Foreign Key Constraints\nALTER TABLE users ADD CONSTRAINT fk_users_department FOREIGN KEY (department_id) REFERENCES departments (id);"
                )
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error - Failed to generate DDL script",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = @ExampleObject(
                    name = "Error Response",
                    value = "{\"error\": \"Failed to generate DDL script\", \"timestamp\": \"2023-12-01T10:00:00Z\"}"
                )
            )
        )
    })
    public ResponseEntity<String> getSchemaAsDdl(
            @Parameter(description = "Schema name (optional, uses default if not provided)", example = "galaxy_compliance_mcbankny")
            @RequestParam(required = false) String schema) {
        log.info("Generating DDL script for schema: {}", schema);
        DatabaseSchema databaseSchema = databaseSchemaService.fetchSchema(schema);
        String ddlScript = ddlGenerationService.generateDdlScript(databaseSchema);
        
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(ddlScript);
    }

    @GetMapping(value = "/ddl/download", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Operation(
        summary = "Download complete database schema as SQL file",
        description = "Generate and download the entire database schema as a SQL file containing DDL statements. The file includes CREATE TABLE statements with columns, primary keys, foreign keys, and constraints to recreate the complete database structure.",
        operationId = "downloadSchemaAsSql"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully generated and returned SQL file",
            content = @Content(
                mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                schema = @Schema(type = "string", format = "binary", description = "SQL file content as binary data")
            ),
            headers = {
                @io.swagger.v3.oas.annotations.headers.Header(
                    name = "Content-Disposition",
                    description = "Attachment header with filename",
                    schema = @Schema(type = "string", example = "attachment; filename=\"database_schema_galaxy_compliance_mcbankny_1693776000000.sql\"")
                )
            }
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error - Failed to generate SQL file",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
        )
    })
    public ResponseEntity<byte[]> downloadSchemaAsSql(
            @Parameter(description = "Schema name (optional, uses default if not provided)", example = "galaxy_compliance_mcbankny")
            @RequestParam(required = false) String schema) {
        log.info("Generating SQL file download for schema: {}", schema);
        DatabaseSchema databaseSchema = databaseSchemaService.fetchSchema(schema);
        String ddlScript = ddlGenerationService.generateDdlScript(databaseSchema);
        
        String filename = "database_schema_" + (schema != null ? schema : databaseSchema.getDatabaseName()) + "_" + getCurrentTimestamp() + ".sql";
        
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(ddlScript.getBytes());
    }

    private String getCurrentTimestamp() {
        return String.valueOf(System.currentTimeMillis());
    }
}
