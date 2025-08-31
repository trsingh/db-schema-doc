package com.finzly.dbschemadoc.controller;

import com.finzly.dbschemadoc.model.DatabaseSchema;
import com.finzly.dbschemadoc.service.DatabaseSchemaService;
import com.finzly.dbschemadoc.service.DocxReportService;
import com.finzly.dbschemadoc.service.HtmlReportService;
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
@Tag(name = "Schema Reports", description = "Database Schema Documentation API - Generate comprehensive reports in multiple formats (JSON, HTML, DOCX)")
public class SchemaController {

    private final DatabaseSchemaService databaseSchemaService;
    private final HtmlReportService htmlReportService;
    private final DocxReportService docxReportService;

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
                    description = "Example database schema with tables and columns",
                    value = "{\n" +
                           "  \"databaseName\": \"galaxy_compliance_mcbankny\",\n" +
                           "  \"databaseProductName\": \"MySQL\",\n" +
                           "  \"userName\": \"root\",\n" +
                           "  \"tables\": [\n" +
                           "    {\n" +
                           "      \"tableName\": \"users\",\n" +
                           "      \"tableType\": \"TABLE\",\n" +
                           "      \"columns\": [\n" +
                           "        {\n" +
                           "          \"columnName\": \"id\",\n" +
                           "          \"dataType\": \"BIGINT\",\n" +
                           "          \"nullable\": false,\n" +
                           "          \"isPrimaryKey\": true\n" +
                           "        }\n" +
                           "      ]\n" +
                           "    }\n" +
                           "  ]\n" +
                           "}"
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
                    value = "<!DOCTYPE html>\n<html>\n<head>\n<title>Database Schema Report</title>\n</head>\n<body>\n<h1>MySQL Database Schema</h1>\n<h2>users</h2>\n<table>\n<tr><th>Column</th><th>Type</th><th>Constraints</th></tr>\n<tr><td>id</td><td>BIGINT</td><td>PRIMARY KEY</td></tr>\n</table>\n</body>\n</html>"
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

    private String getCurrentTimestamp() {
        return String.valueOf(System.currentTimeMillis());
    }
}
