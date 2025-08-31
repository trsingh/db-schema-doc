package com.finzly.dbschemadoc.controller;

import com.finzly.dbschemadoc.model.DatabaseSchema;
import com.finzly.dbschemadoc.service.DatabaseMetadataService;
import com.finzly.dbschemadoc.service.DatabaseSchemaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for database metadata operations.
 * All endpoints are read-only and perform no DDL/DML operations.
 */
@RestController
@RequestMapping("/api/database")
@RequiredArgsConstructor
@Tag(name = "Database Metadata", description = "Operations for retrieving database schema information")
public class DatabaseController {

    private final DatabaseMetadataService databaseMetadataService;
    private final DatabaseSchemaService databaseSchemaService;

    @GetMapping("/info")
    @Operation(summary = "Get database information", description = "Retrieve basic database connection and version information")
    public ResponseEntity<Map<String, String>> getDatabaseInfo() {
        Map<String, String> info = databaseMetadataService.getDatabaseInfo();
        return ResponseEntity.ok(info);
    }

    @GetMapping("/schemas")
    @Operation(summary = "Get all schemas", description = "Retrieve list of all database schemas")
    public ResponseEntity<List<String>> getSchemas() {
        List<String> schemas = databaseMetadataService.getSchemas();
        return ResponseEntity.ok(schemas);
    }

    @GetMapping("/schemas/{schemaName}/tables")
    @Operation(summary = "Get tables in schema", description = "Retrieve all tables in the specified schema")
    public ResponseEntity<List<Map<String, String>>> getTables(
            @Parameter(description = "Name of the schema") @PathVariable String schemaName) {
        List<Map<String, String>> tables = databaseMetadataService.getTables(schemaName);
        return ResponseEntity.ok(tables);
    }

    @GetMapping("/schemas/{schemaName}/tables/{tableName}/columns")
    @Operation(summary = "Get table columns", description = "Retrieve column information for the specified table")
    public ResponseEntity<List<Map<String, Object>>> getTableColumns(
            @Parameter(description = "Name of the schema") @PathVariable String schemaName,
            @Parameter(description = "Name of the table") @PathVariable String tableName) {
        List<Map<String, Object>> columns = databaseMetadataService.getTableColumns(schemaName, tableName);
        return ResponseEntity.ok(columns);
    }

    @GetMapping("/schemas/{schemaName}/tables/{tableName}/primary-keys")
    @Operation(summary = "Get primary keys", description = "Retrieve primary key columns for the specified table")
    public ResponseEntity<List<String>> getPrimaryKeys(
            @Parameter(description = "Name of the schema") @PathVariable String schemaName,
            @Parameter(description = "Name of the table") @PathVariable String tableName) {
        List<String> primaryKeys = databaseMetadataService.getPrimaryKeys(schemaName, tableName);
        return ResponseEntity.ok(primaryKeys);
    }

    @GetMapping("/schema/json")
    @Operation(summary = "Get complete database schema", description = "Retrieve complete database schema with tables, columns, and constraints as JSON")
    public ResponseEntity<DatabaseSchema> getSchemaAsJson() {
        DatabaseSchema schema = databaseSchemaService.fetchSchema();
        return ResponseEntity.ok(schema);
    }
}
