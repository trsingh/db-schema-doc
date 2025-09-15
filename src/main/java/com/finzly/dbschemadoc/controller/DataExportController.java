package com.finzly.dbschemadoc.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.finzly.dbschemadoc.model.CustomQueryRequest;
import com.finzly.dbschemadoc.service.CsvDataExportService;
import com.finzly.dbschemadoc.service.CsvDataExportService.CustomQueryExportResult;
import com.finzly.dbschemadoc.service.DatabaseMetadataService;

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

/**
 * REST Controller for data export operations.
 * Provides CSV export functionality with date range filtering and file splitting for large datasets.
 */
@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Data Export", description = "Export table data to CSV format with date range filtering and large dataset support")
public class DataExportController {

    private final CsvDataExportService csvDataExportService;
    private final DatabaseMetadataService databaseMetadataService;

    @PostMapping("/csv/date-range")
    @Operation(
        summary = "Export table data to CSV with date range filter",
        description = "Export table data to CSV files with date range filtering. Supports large datasets with automatic file splitting. " +
                     "Filter by specific days of the month (e.g., DAYOFMONTH(created_date) BETWEEN 1 AND 15). " +
                     "Files are automatically split if they exceed the maximum size limit and saved to the configured reports directory.",
        operationId = "exportTableDataWithDateRange"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully exported table data to CSV files",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ExportResponse.class),
                examples = @ExampleObject(
                    name = "Successful Export",
                    description = "Example response for successful export",
                    value = "{\"success\": true, \"message\": \"Export completed successfully\", \"filesGenerated\": 2, \"filePaths\": [\"C:/finzly-poc/db-schema-doc/reports/export_users_days_1-15_month_12_year_2023_20231201_143022.csv\", \"C:/finzly-poc/db-schema-doc/reports/export_users_days_1-15_month_12_year_2023_20231201_143022_part002.csv\"], \"totalRecords\": 150000}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request - Invalid parameters",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Invalid Parameters",
                    value = "{\"success\": false, \"error\": \"Start day cannot be greater than end day\", \"timestamp\": \"2023-12-01T14:30:22Z\"}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error - Export failed",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Export Error",
                    value = "{\"success\": false, \"error\": \"Failed to export table data to CSV\", \"timestamp\": \"2023-12-01T14:30:22Z\"}"
                )
            )
        )
    })
    public ResponseEntity<ExportResponse> exportTableDataWithDateRange(
            @Parameter(description = "Name of the table to export", required = true, example = "users")
            @RequestParam String tableName,
            
            @Parameter(description = "Start day of month (1-31)", required = true, example = "1")
            @RequestParam Integer startDay,
            
            @Parameter(description = "End day of month (1-31)", required = true, example = "15")
            @RequestParam Integer endDay,
            
            @Parameter(description = "Month (1-12), defaults to current month if not provided", example = "12")
            @RequestParam(required = false) Integer month,
            
            @Parameter(description = "Year (e.g., 2023), defaults to current year if not provided", example = "2023")
            @RequestParam(required = false) Integer year,
            
            @Parameter(description = "Name of the date column to filter on (required for date filtering)", example = "created_date")
            @RequestParam(required = false) String dateColumn) {
        
        log.info("CSV export request - Table: {}, Date range: {}-{}, Month: {}, Year: {}, Date column: {}", 
                tableName, startDay, endDay, month, year, dateColumn);
        
        try {
            List<String> generatedFiles = csvDataExportService.exportTableData(
                    tableName, startDay, endDay, month, year, dateColumn);
            
            ExportResponse response = ExportResponse.builder()
                    .success(true)
                    .message("Export completed successfully")
                    .filesGenerated(generatedFiles.size())
                    .filePaths(generatedFiles)
                    .totalRecords(null)
                    .build();
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid parameters for CSV export: {}", e.getMessage());
            ExportResponse response = ExportResponse.builder()
                    .success(false)
                    .error(e.getMessage())
                    .build();
            return ResponseEntity.badRequest().body(response);
            
        } catch (Exception e) {
            log.error("Error during CSV export: {}", e.getMessage(), e);
            ExportResponse response = ExportResponse.builder()
                    .success(false)
                    .error("Failed to export table data: " + e.getMessage())
                    .build();
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/csv/weekly")
    @Operation(
        summary = "Export table data to CSV with weekly filter",
        description = "Export table data to CSV files filtered by week number and year. " +
                     "Automatically handles large datasets with file splitting.",
        operationId = "exportTableDataWeekly"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully exported weekly table data to CSV files",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ExportResponse.class),
                examples = @ExampleObject(
                    name = "Successful Weekly Export",
                    value = "{\"success\": true, \"message\": \"Export completed successfully\", \"filesGenerated\": 1, \"filePaths\": [\"C:/finzly-poc/db-schema-doc/reports/export_transactions_week_48_year_2023_20231201_143022.csv\"]}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request - Invalid week number or parameters"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error - Export failed"
        )
    })
    public ResponseEntity<ExportResponse> exportTableDataWeekly(
            @Parameter(description = "Name of the table to export", required = true, example = "transactions")
            @RequestParam String tableName,
            
            @Parameter(description = "Week number (1-52)", required = true, example = "48")
            @RequestParam Integer weekNumber,
            
            @Parameter(description = "Year (e.g., 2023), defaults to current year if not provided", example = "2023")
            @RequestParam(required = false) Integer year,
            
            @Parameter(description = "Name of the date column to filter on", example = "transaction_date")
            @RequestParam(required = false) String dateColumn) {
        
        log.info("Weekly CSV export request - Table: {}, Week: {}, Year: {}, Date column: {}", 
                tableName, weekNumber, year, dateColumn);
        
        try {
            if (weekNumber < 1 || weekNumber > 52) {
                throw new IllegalArgumentException("Week number must be between 1 and 52");
            }
            
            List<String> generatedFiles = csvDataExportService.exportTableDataWeekly(
                    tableName, weekNumber, year, dateColumn);
            
            ExportResponse response = ExportResponse.builder()
                    .success(true)
                    .message("Weekly export completed successfully")
                    .filesGenerated(generatedFiles.size())
                    .filePaths(generatedFiles)
                    .build();
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid parameters for weekly CSV export: {}", e.getMessage());
            ExportResponse response = ExportResponse.builder()
                    .success(false)
                    .error(e.getMessage())
                    .build();
            return ResponseEntity.badRequest().body(response);
            
        } catch (Exception e) {
            log.error("Error during weekly CSV export: {}", e.getMessage(), e);
            ExportResponse response = ExportResponse.builder()
                    .success(false)
                    .error("Failed to export weekly table data: " + e.getMessage())
                    .build();
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/csv/monthly")
    @Operation(
        summary = "Export table data to CSV with monthly filter",
        description = "Export table data to CSV files filtered by month and year. " +
                     "Automatically handles large datasets with file splitting.",
        operationId = "exportTableDataMonthly"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully exported monthly table data to CSV files",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ExportResponse.class),
                examples = @ExampleObject(
                    name = "Successful Monthly Export",
                    value = "{\"success\": true, \"message\": \"Export completed successfully\", \"filesGenerated\": 3, \"filePaths\": [\"C:/finzly-poc/db-schema-doc/reports/export_orders_month_12_year_2023_20231201_143022.csv\", \"C:/finzly-poc/db-schema-doc/reports/export_orders_month_12_year_2023_20231201_143022_part002.csv\", \"C:/finzly-poc/db-schema-doc/reports/export_orders_month_12_year_2023_20231201_143022_part003.csv\"]}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request - Invalid month or parameters"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error - Export failed"
        )
    })
    public ResponseEntity<ExportResponse> exportTableDataMonthly(
            @Parameter(description = "Name of the table to export", required = true, example = "orders")
            @RequestParam String tableName,
            
            @Parameter(description = "Month (1-12)", required = true, example = "12")
            @RequestParam Integer month,
            
            @Parameter(description = "Year (e.g., 2023), defaults to current year if not provided", example = "2023")
            @RequestParam(required = false) Integer year,
            
            @Parameter(description = "Name of the date column to filter on", example = "order_date")
            @RequestParam(required = false) String dateColumn) {
        
        log.info("Monthly CSV export request - Table: {}, Month: {}, Year: {}, Date column: {}", 
                tableName, month, year, dateColumn);
        
        try {
            List<String> generatedFiles = csvDataExportService.exportTableDataMonthly(
                    tableName, month, year, dateColumn);
            
            ExportResponse response = ExportResponse.builder()
                    .success(true)
                    .message("Monthly export completed successfully")
                    .filesGenerated(generatedFiles.size())
                    .filePaths(generatedFiles)
                    .build();
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid parameters for monthly CSV export: {}", e.getMessage());
            ExportResponse response = ExportResponse.builder()
                    .success(false)
                    .error(e.getMessage())
                    .build();
            return ResponseEntity.badRequest().body(response);
            
        } catch (Exception e) {
            log.error("Error during monthly CSV export: {}", e.getMessage(), e);
            ExportResponse response = ExportResponse.builder()
                    .success(false)
                    .error("Failed to export monthly table data: " + e.getMessage())
                    .build();
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/tables")
    @Operation(
        summary = "Get list of available tables for export",
        description = "Retrieve a list of all available tables in the default schema that can be exported.",
        operationId = "getAvailableTables"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved list of available tables",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Available Tables",
                    value = "{\"success\": true, \"tables\": [\"users\", \"transactions\", \"orders\", \"products\", \"audit_logs\"]}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error - Failed to retrieve tables"
        )
    })
    public ResponseEntity<Map<String, Object>> getAvailableTables() {
        log.info("Retrieving available tables for export");
        
        try {
            String defaultSchema = "galaxy_compliance_mcbankny";
            
            List<Map<String, String>> tableMetadata = databaseMetadataService.getTables(defaultSchema);
            
            List<String> tableNames = tableMetadata.stream()
                    .map(table -> table.get("tableName"))
                    .filter(name -> name != null && !name.trim().isEmpty())
                    .sorted()
                    .toList();
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "Successfully retrieved available tables",
                "schema", defaultSchema,
                "tableCount", tableNames.size(),
                "tables", tableNames
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error retrieving available tables: {}", e.getMessage(), e);
            Map<String, Object> response = Map.of(
                "success", false,
                "error", "Failed to retrieve available tables: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/csv/custom-query")
    @Operation(
        summary = "Export data using custom SQL query to CSV",
        description = "Execute a custom SELECT query and export the results to CSV files. " +
                     "Supports complex queries with millions of records using automatic file splitting and streaming. " +
                     "Only SELECT queries are allowed for security. Provide the SQL query and export name in the request body.",
        operationId = "exportCustomQueryToCsv"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully exported custom query results to CSV files",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CustomQueryExportResponse.class),
                examples = @ExampleObject(
                    name = "Successful Custom Query Export",
                    description = "Example response for successful custom query export",
                    value = "{\"success\": true, \"message\": \"Custom query export completed successfully\", \"filesGenerated\": 3, \"filePaths\": [\"C:/finzly-poc/db-schema-doc/reports/export_custom_user_orders_20231201_143022.csv\"], \"estimatedRecords\": 250000, \"columnNames\": [\"user_id\", \"username\", \"order_id\", \"order_date\", \"total_amount\"], \"executionTimeMs\": 45000, \"queryLength\": 245}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request - Invalid SQL query or parameters",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Invalid Query",
                    value = "{\"success\": false, \"error\": \"Only SELECT queries are allowed\"}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error - Query execution failed"
        )
    })
    public ResponseEntity<CustomQueryExportResponse> exportCustomQueryToCsv(
            @Parameter(
                description = "Custom query request containing SQL query and export parameters",
                required = true
            )
            @org.springframework.web.bind.annotation.RequestBody CustomQueryRequest request) {
        
        log.info("Custom SQL query export request received - Request: [{}]", request);
        
        // Extract values from request object
        String sqlQuery = request.getSqlQuery();
        String exportName = request.getExportName();
        
        log.info("Custom SQL query export request - Export name: [{}], Query length: {}", 
                exportName, sqlQuery != null ? sqlQuery.length() : 0);
        log.info("SQL Query received in controller: [{}]", sqlQuery != null ? sqlQuery : "NULL");
        
        // Basic validation - Spring will ensure request is not null for @RequestBody
        
        if (sqlQuery == null || sqlQuery.trim().isEmpty()) {
            log.error("SQL query is null or empty. Query: [{}]", sqlQuery);
            CustomQueryExportResponse response = CustomQueryExportResponse.builder()
                    .success(false)
                    .error("SQL query cannot be null or empty")
                    .exportName(exportName != null ? exportName : "unknown")
                    .queryLength(sqlQuery != null ? sqlQuery.length() : 0)
                    .build();
            return ResponseEntity.badRequest().body(response);
        }
        
        if (exportName == null || exportName.trim().isEmpty()) {
            log.error("Export name is null or empty. Export name: [{}]", exportName);
            CustomQueryExportResponse response = CustomQueryExportResponse.builder()
                    .success(false)
                    .error("Export name cannot be null or empty")
                    .exportName(exportName != null ? exportName : "unknown")
                    .queryLength(sqlQuery.length())
                    .build();
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            CustomQueryExportResult result = csvDataExportService.exportCustomQueryWithMetadata(sqlQuery.trim(), exportName.trim());
            
            CustomQueryExportResponse response = CustomQueryExportResponse.builder()
                    .success(result.isSuccess())
                    .message(result.isSuccess() ? "Custom query export completed successfully" : null)
                    .error(result.getError())
                    .filesGenerated(result.getFilesGenerated())
                    .filePaths(result.getFilePaths())
                    .estimatedRecords(result.getEstimatedRecords())
                    .columnNames(result.getColumnNames())
                    .executionTimeMs(result.getExecutionTimeMs())
                    .exportName(result.getExportName())
                    .queryLength(result.getQueryLength())
                    .build();
            
            if (result.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid SQL query for custom export: {}", e.getMessage());
            CustomQueryExportResponse response = CustomQueryExportResponse.builder()
                    .success(false)
                    .error(e.getMessage())
                    .exportName(exportName)
                    .queryLength(sqlQuery != null ? sqlQuery.length() : 0)
                    .build();
            return ResponseEntity.badRequest().body(response);
            
        } catch (Exception e) {
            log.error("Error during custom SQL query export: {}", e.getMessage(), e);
            CustomQueryExportResponse response = CustomQueryExportResponse.builder()
                    .success(false)
                    .error("Failed to execute custom query export: " + e.getMessage())
                    .exportName(exportName != null ? exportName : "unknown")
                    .queryLength(sqlQuery != null ? sqlQuery.length() : 0)
                    .build();
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping(value = "/csv/custom-query-file", consumes = "multipart/form-data")
    @Operation(
        summary = "Export data using SQL file upload to CSV",
        description = "Upload a .sql file containing a SELECT query and export the results to CSV files. " +
                     "This approach is recommended for complex queries with special formatting, comments, or multi-line structure. " +
                     "Only SELECT queries are allowed for security. The file will be validated before execution.",
        operationId = "exportCustomQueryFromFile"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully exported SQL file query results to CSV files",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CustomQueryExportResponse.class),
                examples = @ExampleObject(
                    name = "Successful File Query Export",
                    description = "Example response for successful SQL file export",
                    value = "{\"success\": true, \"message\": \"SQL file query export completed successfully\", \"filesGenerated\": 2, \"filePaths\": [\"C:/finzly-poc/db-schema-doc/reports/export_file_query_20231201_143022.csv\"], \"estimatedRecords\": 150000, \"columnNames\": [\"id\", \"name\", \"email\", \"created_at\"], \"executionTimeMs\": 35000, \"queryLength\": 180, \"fileName\": \"user_export.sql\"}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request - Invalid file, query, or parameters",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Invalid File",
                    value = "{\"success\": false, \"error\": \"Only SELECT queries are allowed\"}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error - File processing or query execution failed"
        )
    })
    public ResponseEntity<CustomQueryExportResponse> exportCustomQueryFromFile(
            @Parameter(
                description = "SQL file containing SELECT query (.sql file recommended). " +
                             "File should contain a single SELECT statement. Comments and formatting are preserved.",
                required = true
            )
            @RequestParam("sqlFile") MultipartFile sqlFile,
            
            @Parameter(
                description = "Name for the export (used in filename generation). " +
                             "Will be sanitized to remove special characters.",
                required = true,
                example = "user_data_export"
            )
            @RequestParam("exportName") String exportName) {
        
        log.info("SQL file upload export request - File: [{}], Export name: [{}], File size: {} bytes", 
                sqlFile.getOriginalFilename(), exportName, sqlFile.getSize());
        
        // Basic validation
        if (sqlFile.isEmpty()) {
            log.error("Uploaded SQL file is empty");
            CustomQueryExportResponse response = CustomQueryExportResponse.builder()
                    .success(false)
                    .error("SQL file cannot be empty")
                    .exportName(exportName != null ? exportName : "unknown")
                    .queryLength(0)
                    .build();
            return ResponseEntity.badRequest().body(response);
        }
        
        if (exportName == null || exportName.trim().isEmpty()) {
            log.error("Export name is null or empty. Export name: [{}]", exportName);
            CustomQueryExportResponse response = CustomQueryExportResponse.builder()
                    .success(false)
                    .error("Export name cannot be null or empty")
                    .exportName("unknown")
                    .queryLength(0)
                    .build();
            return ResponseEntity.badRequest().body(response);
        }
        
        // File type validation
        String fileName = sqlFile.getOriginalFilename();
        if (fileName == null || (!fileName.toLowerCase().endsWith(".sql") && !fileName.toLowerCase().endsWith(".txt"))) {
            log.warn("File upload with unexpected extension: [{}]", fileName);
        }
        
        try {
            // Read file content
            String sqlQuery = new String(sqlFile.getBytes(), StandardCharsets.UTF_8);
            
            log.info("SQL content read from file - Length: {}, First 100 chars: [{}]", 
                    sqlQuery.length(), 
                    sqlQuery.length() > 100 ? sqlQuery.substring(0, 100) : sqlQuery);
            
            // Clean up the query (remove extra whitespace, but preserve intentional formatting)
            sqlQuery = sqlQuery.trim();
            
            if (sqlQuery.isEmpty()) {
                log.error("SQL file content is empty after trimming");
                CustomQueryExportResponse response = CustomQueryExportResponse.builder()
                        .success(false)
                        .error("SQL file content cannot be empty")
                        .exportName(exportName)
                        .queryLength(0)
                        .build();
                return ResponseEntity.badRequest().body(response);
            }
            
            // Execute the query
            CustomQueryExportResult result = csvDataExportService.exportCustomQueryWithMetadata(sqlQuery, exportName.trim());
            
            CustomQueryExportResponse response = CustomQueryExportResponse.builder()
                    .success(result.isSuccess())
                    .message(result.isSuccess() ? "SQL file query export completed successfully" : null)
                    .error(result.getError())
                    .filesGenerated(result.getFilesGenerated())
                    .filePaths(result.getFilePaths())
                    .estimatedRecords(result.getEstimatedRecords())
                    .columnNames(result.getColumnNames())
                    .executionTimeMs(result.getExecutionTimeMs())
                    .exportName(exportName)
                    .queryLength(sqlQuery.length())
                    .build();
            
            return ResponseEntity.ok(response);
            
        } catch (IOException e) {
            log.error("Error reading SQL file: {}", e.getMessage(), e);
            CustomQueryExportResponse response = CustomQueryExportResponse.builder()
                    .success(false)
                    .error("Failed to read SQL file: " + e.getMessage())
                    .exportName(exportName)
                    .queryLength(0)
                    .build();
            return ResponseEntity.badRequest().body(response);
            
        } catch (Exception e) {
            log.error("Error during SQL file query export: {}", e.getMessage(), e);
            CustomQueryExportResponse response = CustomQueryExportResponse.builder()
                    .success(false)
                    .error("Failed to execute SQL file query export: " + e.getMessage())
                    .exportName(exportName != null ? exportName : "unknown")
                    .queryLength(0)
                    .build();
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/csv/validate-query")
    @Operation(
        summary = "Validate SQL query before export",
        description = "Validate a custom SELECT query to ensure it's safe to execute. " +
                     "This endpoint checks for security issues, syntax problems, and provides feedback " +
                     "without actually executing the query.",
        operationId = "validateSqlQuery"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Query validation result",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Valid Query",
                    value = "{\"valid\": true, \"message\": \"Query validation passed\", \"queryLength\": 245, \"estimatedComplexity\": \"MEDIUM\"}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid query"
        )
    })
    public ResponseEntity<Map<String, Object>> validateSqlQuery(
            @Parameter(
                description = "SQL query to validate",
                required = true,
                example = "SELECT u.*, o.* FROM users u JOIN orders o ON u.user_id = o.user_id"
            )
            @RequestParam(value = "sqlQuery", required = true) String sqlQuery) {
        
        log.info("SQL query validation request - Query length: {}", sqlQuery != null ? sqlQuery.length() : 0);
        log.debug("SQL Query for validation: [{}]", sqlQuery);
        
        try {
            // Use the same validation as the export service
            csvDataExportService.validateSqlQuerySafely(sqlQuery);
            
            Map<String, Object> response = Map.of(
                "valid", true,
                "message", "Query validation passed",
                "queryLength", sqlQuery != null ? sqlQuery.length() : 0,
                "estimatedComplexity", estimateQueryComplexity(sqlQuery)
            );
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.debug("Query validation failed: {}", e.getMessage());
            Map<String, Object> response = Map.of(
                "valid", false,
                "error", e.getMessage(),
                "queryLength", sqlQuery != null ? sqlQuery.length() : 0
            );
            return ResponseEntity.badRequest().body(response);
            
        } catch (Exception e) {
            log.debug("Query validation error: {}", e.getMessage());
            Map<String, Object> response = Map.of(
                "valid", false,
                "error", "Query validation failed: " + e.getMessage(),
                "queryLength", sqlQuery != null ? sqlQuery.length() : 0
            );
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Estimate query complexity based on keywords and structure.
     */
    private String estimateQueryComplexity(String sqlQuery) {
        if (sqlQuery == null) return "UNKNOWN";
        
        String upperQuery = sqlQuery.toUpperCase();
        int complexity = 0;
        
        // Count JOIN operations
        complexity += countOccurrences(upperQuery, "JOIN") * 2;
        
        // Count subqueries
        complexity += countOccurrences(upperQuery, "SELECT") - 1; // -1 for main SELECT
        
        // Count aggregate functions
        complexity += countOccurrences(upperQuery, "GROUP BY");
        complexity += countOccurrences(upperQuery, "ORDER BY");
        complexity += countOccurrences(upperQuery, "HAVING");
        
        // Count complex functions
        String[] complexFunctions = {"SUM(", "COUNT(", "AVG(", "MAX(", "MIN(", "DISTINCT"};
        for (String func : complexFunctions) {
            complexity += countOccurrences(upperQuery, func);
        }
        
        if (complexity <= 2) return "LOW";
        else if (complexity <= 5) return "MEDIUM";
        else if (complexity <= 10) return "HIGH";
        else return "VERY_HIGH";
    }
    
    /**
     * Count occurrences of a substring in a string.
     */
    private int countOccurrences(String text, String pattern) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(pattern, index)) != -1) {
            count++;
            index += pattern.length();
        }
        return count;
    }

    /**
     * Response model for export operations.
     */
    @Schema(description = "Response model for data export operations")
    public static class ExportResponse {
        @Schema(description = "Whether the export operation was successful", example = "true")
        private boolean success;
        
        @Schema(description = "Success or error message", example = "Export completed successfully")
        private String message;
        
        @Schema(description = "Error message if operation failed", example = "Table not found")
        private String error;
        
        @Schema(description = "Number of CSV files generated", example = "2")
        private Integer filesGenerated;
        
        @Schema(description = "List of file paths for generated CSV files")
        private List<String> filePaths;
        
        @Schema(description = "Total number of records exported", example = "150000")
        private Long totalRecords;

        public ExportResponse() {}

        public static ExportResponseBuilder builder() {
            return new ExportResponseBuilder();
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getError() { return error; }
        public void setError(String error) { this.error = error; }

        public Integer getFilesGenerated() { return filesGenerated; }
        public void setFilesGenerated(Integer filesGenerated) { this.filesGenerated = filesGenerated; }

        public List<String> getFilePaths() { return filePaths; }
        public void setFilePaths(List<String> filePaths) { this.filePaths = filePaths; }

        public Long getTotalRecords() { return totalRecords; }
        public void setTotalRecords(Long totalRecords) { this.totalRecords = totalRecords; }

        public static class ExportResponseBuilder {
            private boolean success;
            private String message;
            private String error;
            private Integer filesGenerated;
            private List<String> filePaths;
            private Long totalRecords;

            public ExportResponseBuilder success(boolean success) {
                this.success = success;
                return this;
            }

            public ExportResponseBuilder message(String message) {
                this.message = message;
                return this;
            }

            public ExportResponseBuilder error(String error) {
                this.error = error;
                return this;
            }

            public ExportResponseBuilder filesGenerated(Integer filesGenerated) {
                this.filesGenerated = filesGenerated;
                return this;
            }

            public ExportResponseBuilder filePaths(List<String> filePaths) {
                this.filePaths = filePaths;
                return this;
            }

            public ExportResponseBuilder totalRecords(Long totalRecords) {
                this.totalRecords = totalRecords;
                return this;
            }

            public ExportResponse build() {
                ExportResponse response = new ExportResponse();
                response.setSuccess(this.success);
                response.setMessage(this.message);
                response.setError(this.error);
                response.setFilesGenerated(this.filesGenerated);
                response.setFilePaths(this.filePaths);
                response.setTotalRecords(this.totalRecords);
                return response;
            }
        }
    }

    /**
     * Response model for custom query export operations.
     */
    @Schema(description = "Response model for custom SQL query export operations")
    public static class CustomQueryExportResponse {
        @Schema(description = "Whether the export operation was successful", example = "true")
        private boolean success;
        
        @Schema(description = "Success message", example = "Custom query export completed successfully")
        private String message;
        
        @Schema(description = "Error message if operation failed", example = "Only SELECT queries are allowed")
        private String error;
        
        @Schema(description = "Number of CSV files generated", example = "3")
        private Integer filesGenerated;
        
        @Schema(description = "List of file paths for generated CSV files")
        private List<String> filePaths;
        
        @Schema(description = "Estimated number of records exported", example = "250000")
        private Long estimatedRecords;
        
        @Schema(description = "Array of column names in the result set", example = "[\"user_id\", \"username\", \"order_id\", \"order_date\", \"total_amount\"]")
        private String[] columnNames;
        
        @Schema(description = "Query execution time in milliseconds", example = "45000")
        private Long executionTimeMs;
        
        @Schema(description = "Export name used for file generation", example = "user_orders_2023")
        private String exportName;
        
        @Schema(description = "Length of the SQL query in characters", example = "245")
        private Integer queryLength;
        
        @Schema(description = "Name of the uploaded SQL file (for file upload operations)", example = "user_query.sql")
        private String fileName;

        public CustomQueryExportResponse() {}

        public static CustomQueryExportResponseBuilder builder() {
            return new CustomQueryExportResponseBuilder();
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getError() { return error; }
        public void setError(String error) { this.error = error; }

        public Integer getFilesGenerated() { return filesGenerated; }
        public void setFilesGenerated(Integer filesGenerated) { this.filesGenerated = filesGenerated; }

        public List<String> getFilePaths() { return filePaths; }
        public void setFilePaths(List<String> filePaths) { this.filePaths = filePaths; }

        public Long getEstimatedRecords() { return estimatedRecords; }
        public void setEstimatedRecords(Long estimatedRecords) { this.estimatedRecords = estimatedRecords; }

        public String[] getColumnNames() { return columnNames; }
        public void setColumnNames(String[] columnNames) { this.columnNames = columnNames; }

        public Long getExecutionTimeMs() { return executionTimeMs; }
        public void setExecutionTimeMs(Long executionTimeMs) { this.executionTimeMs = executionTimeMs; }

        public String getExportName() { return exportName; }
        public void setExportName(String exportName) { this.exportName = exportName; }

        public Integer getQueryLength() { return queryLength; }
        public void setQueryLength(Integer queryLength) { this.queryLength = queryLength; }

        public static class CustomQueryExportResponseBuilder {
            private boolean success;
            private String message;
            private String error;
            private Integer filesGenerated;
            private List<String> filePaths;
            private Long estimatedRecords;
            private String[] columnNames;
            private Long executionTimeMs;
            private String exportName;
            private Integer queryLength;

            public CustomQueryExportResponseBuilder success(boolean success) {
                this.success = success;
                return this;
            }

            public CustomQueryExportResponseBuilder message(String message) {
                this.message = message;
                return this;
            }

            public CustomQueryExportResponseBuilder error(String error) {
                this.error = error;
                return this;
            }

            public CustomQueryExportResponseBuilder filesGenerated(Integer filesGenerated) {
                this.filesGenerated = filesGenerated;
                return this;
            }

            public CustomQueryExportResponseBuilder filePaths(List<String> filePaths) {
                this.filePaths = filePaths;
                return this;
            }

            public CustomQueryExportResponseBuilder estimatedRecords(Long estimatedRecords) {
                this.estimatedRecords = estimatedRecords;
                return this;
            }

            public CustomQueryExportResponseBuilder columnNames(String[] columnNames) {
                this.columnNames = columnNames;
                return this;
            }

            public CustomQueryExportResponseBuilder executionTimeMs(Long executionTimeMs) {
                this.executionTimeMs = executionTimeMs;
                return this;
            }

            public CustomQueryExportResponseBuilder exportName(String exportName) {
                this.exportName = exportName;
                return this;
            }

            public CustomQueryExportResponseBuilder queryLength(Integer queryLength) {
                this.queryLength = queryLength;
                return this;
            }

            public CustomQueryExportResponse build() {
                CustomQueryExportResponse response = new CustomQueryExportResponse();
                response.setSuccess(this.success);
                response.setMessage(this.message);
                response.setError(this.error);
                response.setFilesGenerated(this.filesGenerated);
                response.setFilePaths(this.filePaths);
                response.setEstimatedRecords(this.estimatedRecords);
                response.setColumnNames(this.columnNames);
                response.setExecutionTimeMs(this.executionTimeMs);
                response.setExportName(this.exportName);
                response.setQueryLength(this.queryLength);
                return response;
            }
        }
    }
}