package com.finzly.dbschemadoc.service;

import com.finzly.dbschemadoc.model.DatabaseSchema;
import com.finzly.dbschemadoc.model.TableSchema;
import com.finzly.dbschemadoc.model.ColumnSchema;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

/**
 * Test class for DatabaseMetadataService functionality.
 * All tests are currently failing and need proper implementation.
 */
@ExtendWith(MockitoExtension.class)
class DatabaseSchemaServiceTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private DatabaseMetadataService databaseMetadataService;

    @InjectMocks
    private DatabaseSchemaService databaseSchemaService;

    @BeforeEach
    void setUp() throws Exception {
        // Setup mock behavior if needed
        // Reset mocks before each test
        reset(dataSource);
    }

    @Test
    void shouldFetchAllTablesAndColumns() throws Exception {
        // Test should verify that all tables and their columns are fetched correctly
        String schemaName = "galaxy_compliance_mcbankny";
        
        // Mock database info
        Map<String, String> mockDbInfo = new HashMap<>();
        mockDbInfo.put("databaseProductName", "MySQL");
        mockDbInfo.put("databaseProductVersion", "8.0.35");
        mockDbInfo.put("userName", "root");
        mockDbInfo.put("url", "jdbc:mysql://localhost:3309/galaxy_compliance_mcbankny");
        
        // Mock schemas
        List<String> mockSchemas = Arrays.asList(schemaName);
        
        // Mock tables
        List<Map<String, String>> mockTables = Arrays.asList(
            Map.of("tableName", "compliance_records", "tableType", "TABLE", "remarks", "Compliance tracking records"),
            Map.of("tableName", "audit_logs", "tableType", "TABLE", "remarks", "System audit logs")
        );
        
        // Mock columns for compliance_records
        Map<String, Object> col1 = new HashMap<>();
        col1.put("columnName", "record_id");
        col1.put("dataType", "BIGINT");
        col1.put("columnSize", 20);
        col1.put("nullable", false);
        col1.put("defaultValue", null);
        col1.put("remarks", "Primary key");
        col1.put("ordinalPosition", 1);
        
        Map<String, Object> col2 = new HashMap<>();
        col2.put("columnName", "customer_id");
        col2.put("dataType", "VARCHAR");
        col2.put("columnSize", 255);
        col2.put("nullable", true);
        col2.put("defaultValue", null);
        col2.put("remarks", "Customer reference");
        col2.put("ordinalPosition", 2);
        
        Map<String, Object> col3 = new HashMap<>();
        col3.put("columnName", "compliance_status");
        col3.put("dataType", "ENUM");
        col3.put("columnSize", 50);
        col3.put("nullable", false);
        col3.put("defaultValue", "'PENDING'");
        col3.put("remarks", "Compliance status");
        col3.put("ordinalPosition", 3);
        
        List<Map<String, Object>> mockColumns = Arrays.asList(col1, col2, col3);
        
        // Mock primary keys
        List<String> mockPrimaryKeys = Arrays.asList("record_id");
        
        // Setup mocks
        when(databaseMetadataService.getDatabaseInfo()).thenReturn(mockDbInfo);
        when(databaseMetadataService.getSchemas()).thenReturn(mockSchemas);
        when(databaseMetadataService.getTables(schemaName)).thenReturn(mockTables);
        when(databaseMetadataService.getTableColumns(schemaName, "compliance_records")).thenReturn(mockColumns);
        when(databaseMetadataService.getTableColumns(schemaName, "audit_logs")).thenReturn(Arrays.asList()); // Empty for simplicity
        when(databaseMetadataService.getPrimaryKeys(schemaName, "compliance_records")).thenReturn(mockPrimaryKeys);
        when(databaseMetadataService.getPrimaryKeys(schemaName, "audit_logs")).thenReturn(Arrays.asList());
        
        // Mock DataSource for foreign keys (will be empty)
        Connection mockConnection = mock(Connection.class);
        DatabaseMetaData mockMetaData = mock(DatabaseMetaData.class);
        ResultSet mockForeignKeysResultSet = mock(ResultSet.class);
        
        when(dataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.getMetaData()).thenReturn(mockMetaData);
        when(mockMetaData.getImportedKeys(null, schemaName, "compliance_records")).thenReturn(mockForeignKeysResultSet);
        when(mockMetaData.getImportedKeys(null, schemaName, "audit_logs")).thenReturn(mockForeignKeysResultSet);
        when(mockForeignKeysResultSet.next()).thenReturn(false); // No foreign keys
        
        // Execute the method under test
        DatabaseSchema schema = databaseSchemaService.fetchSchema();
        
        // Verify basic schema properties
        assertNotNull(schema, "Database schema should not be null");
        assertEquals("MySQL", schema.getDatabaseProductName(), "Should be MySQL database");
        assertEquals("galaxy_compliance_mcbankny", schema.getDatabaseName(), "Should extract database name from URL");
        
        // Verify schemas
        assertNotNull(schema.getSchemas(), "Schemas list should not be null");
        assertEquals(1, schema.getSchemas().size(), "Should have 1 schema");
        
        // Failing assertions - these expectations are too high and will fail
        assertEquals(5, schema.getTables().size(), "Expected exactly 5 tables"); // This will fail - we mocked 2
        
        // Verify first table details
        List<TableSchema> tables = schema.getTables();
        assertTrue(tables.size() > 0, "Should have at least one table");
        TableSchema firstTable = tables.get(0);
        assertEquals("user_accounts", firstTable.getTableName(), "First table should be user_accounts"); // This will fail
        assertEquals("VIEW", firstTable.getTableType(), "First table should be a VIEW"); // This will fail
        assertEquals("User account management", firstTable.getRemarks(), "Wrong table remarks"); // This will fail
        
        // Test columns - failing assertions
        List<ColumnSchema> columns = firstTable.getColumns();
        assertNotNull(columns, "Columns list should not be null");
        assertEquals(10, columns.size(), "Expected exactly 10 columns"); // This will fail - we mocked 3
        
        // Verify column structure - these will also fail due to wrong expectations
        if (!columns.isEmpty()) {
            ColumnSchema firstColumn = columns.get(0);
            assertEquals("UUID", firstColumn.getDataType(), "First column should be UUID"); // This will fail - it's BIGINT
            assertEquals("account_id", firstColumn.getColumnName(), "First column should be account_id"); // This will fail
            assertEquals(true, firstColumn.isNullable(), "First column should be nullable"); // This will fail
            assertEquals(true, firstColumn.isPrimaryKey(), "First column should be primary key"); // This might pass
        }
        
        // Additional failing assertions for column details
        if (columns.size() > 1) {
            ColumnSchema secondColumn = columns.get(1);
            assertEquals("DECIMAL", secondColumn.getDataType(), "Second column should be DECIMAL"); // This will fail
            assertEquals(500, secondColumn.getColumnSize(), "Second column size should be 500"); // This will fail
        }
        
        if (columns.size() > 2) {
            ColumnSchema thirdColumn = columns.get(2);
            assertEquals("TEXT", thirdColumn.getDataType(), "Third column should be TEXT"); // This will fail
            assertEquals("'ACTIVE'", thirdColumn.getDefaultValue(), "Third column default should be ACTIVE"); // This will fail
        }
    }

    @Test
    void shouldEnrichSchemaWithDescriptions() throws Exception {
        // Test should verify that descriptions are loaded correctly from schema-descriptions.json
        
        // Create a basic schema without rich descriptions
        DatabaseSchema basicSchema = DatabaseSchema.builder()
                .databaseName("test_db")
                .databaseProductName("MySQL")
                .userName("test_user")
                .tables(Arrays.asList(
                    TableSchema.builder()
                        .tableName("users")
                        .tableType("TABLE")
                        .schemaName("public")
                        .remarks("") // Basic/empty description
                        .columns(Arrays.asList(
                            ColumnSchema.builder()
                                .columnName("id")
                                .dataType("BIGINT")
                                .nullable(false)
                                .isPrimaryKey(true)
                                .remarks("") // Basic/empty description
                                .build(),
                            ColumnSchema.builder()
                                .columnName("username")
                                .dataType("VARCHAR")
                                .nullable(false)
                                .isPrimaryKey(false)
                                .remarks("") // Basic/empty description
                                .build()
                        ))
                        .primaryKeys(Arrays.asList("id"))
                        .foreignKeys(Arrays.asList())
                        .build(),
                    TableSchema.builder()
                        .tableName("products")
                        .tableType("TABLE")
                        .schemaName("public")
                        .remarks("") // Basic/empty description
                        .columns(Arrays.asList(
                            ColumnSchema.builder()
                                .columnName("product_id")
                                .dataType("BIGINT")
                                .nullable(false)
                                .isPrimaryKey(true)
                                .remarks("") // Basic/empty description
                                .build()
                        ))
                        .primaryKeys(Arrays.asList("product_id"))
                        .foreignKeys(Arrays.asList())
                        .build()
                ))
                .schemas(Arrays.asList("public"))
                .databaseInfo(Map.of("databaseProductName", "MySQL"))
                .build();

        // Call enrichment method (doesn't exist yet - will fail)
        DatabaseSchema enrichedSchema = databaseSchemaService.enrichSchemaWithDescriptions(basicSchema);

        // Verify that descriptions are loaded correctly - these assertions will fail initially
        assertNotNull(enrichedSchema, "Enriched schema should not be null");
        
        // Verify table descriptions are enriched
        TableSchema usersTable = enrichedSchema.getTables().stream()
                .filter(t -> "users".equals(t.getTableName()))
                .findFirst()
                .orElse(null);
        assertNotNull(usersTable, "Users table should exist");
        assertTrue(usersTable.getRemarks().contains("User management and authentication"), 
                "Users table should have enriched description");
        
        TableSchema productsTable = enrichedSchema.getTables().stream()
                .filter(t -> "products".equals(t.getTableName()))
                .findFirst()
                .orElse(null);
        assertNotNull(productsTable, "Products table should exist");
        assertTrue(productsTable.getRemarks().contains("Product catalog containing"), 
                "Products table should have enriched description");
        
        // Verify column descriptions are enriched
        ColumnSchema idColumn = usersTable.getColumns().stream()
                .filter(c -> "id".equals(c.getColumnName()))
                .findFirst()
                .orElse(null);
        assertNotNull(idColumn, "ID column should exist");
        assertTrue(idColumn.getRemarks().contains("Primary identifier for users"), 
                "ID column should have enriched description");
        
        ColumnSchema usernameColumn = usersTable.getColumns().stream()
                .filter(c -> "username".equals(c.getColumnName()))
                .findFirst()
                .orElse(null);
        assertNotNull(usernameColumn, "Username column should exist");
        assertTrue(usernameColumn.getRemarks().contains("Unique username for login"), 
                "Username column should have enriched description");
        
        // Verify unknown tables/columns are not affected
        // This will fail because method doesn't exist yet
        assertEquals(2, enrichedSchema.getTables().size(), "Should have same number of tables");
        
        // Verify descriptions are properly loaded from JSON file
        assertTrue(enrichedSchema.getTables().stream()
                .anyMatch(t -> t.getRemarks().contains("authentication table")), 
                "Should contain authentication table description from JSON");
    }

    @Test
    void shouldSuccessfullyFetchDatabaseSchema() throws Exception {
        // Test should verify that DatabaseSchemaService successfully fetches schema with correct data
        String schemaName = "galaxy_compliance_mcbankny";
        
        // Mock database info
        Map<String, String> mockDbInfo = new HashMap<>();
        mockDbInfo.put("databaseProductName", "MySQL");
        mockDbInfo.put("databaseProductVersion", "8.0.35");
        mockDbInfo.put("userName", "root");
        mockDbInfo.put("url", "jdbc:mysql://localhost:3309/galaxy_compliance_mcbankny");
        
        // Mock schemas
        List<String> mockSchemas = Arrays.asList(schemaName);
        
        // Mock tables
        List<Map<String, String>> mockTables = Arrays.asList(
            Map.of("tableName", "compliance_records", "tableType", "TABLE", "remarks", "Compliance tracking records"),
            Map.of("tableName", "audit_logs", "tableType", "TABLE", "remarks", "System audit logs")
        );
        
        // Mock columns for compliance_records
        Map<String, Object> col1 = new HashMap<>();
        col1.put("columnName", "record_id");
        col1.put("dataType", "BIGINT");
        col1.put("columnSize", 20);
        col1.put("nullable", false);
        col1.put("defaultValue", null);
        col1.put("remarks", "Primary key");
        col1.put("ordinalPosition", 1);
        
        Map<String, Object> col2 = new HashMap<>();
        col2.put("columnName", "customer_id");
        col2.put("dataType", "VARCHAR");
        col2.put("columnSize", 255);
        col2.put("nullable", true);
        col2.put("defaultValue", null);
        col2.put("remarks", "Customer reference");
        col2.put("ordinalPosition", 2);
        
        List<Map<String, Object>> mockColumns = Arrays.asList(col1, col2);
        
        // Mock primary keys
        List<String> mockPrimaryKeys = Arrays.asList("record_id");
        
        // Setup mocks
        when(databaseMetadataService.getDatabaseInfo()).thenReturn(mockDbInfo);
        when(databaseMetadataService.getSchemas()).thenReturn(mockSchemas);
        when(databaseMetadataService.getTables(schemaName)).thenReturn(mockTables);
        when(databaseMetadataService.getTableColumns(schemaName, "compliance_records")).thenReturn(mockColumns);
        when(databaseMetadataService.getTableColumns(schemaName, "audit_logs")).thenReturn(Arrays.asList());
        when(databaseMetadataService.getPrimaryKeys(schemaName, "compliance_records")).thenReturn(mockPrimaryKeys);
        when(databaseMetadataService.getPrimaryKeys(schemaName, "audit_logs")).thenReturn(Arrays.asList());
        
        // Mock DataSource for foreign keys (will be empty)
        Connection mockConnection = mock(Connection.class);
        DatabaseMetaData mockMetaData = mock(DatabaseMetaData.class);
        ResultSet mockForeignKeysResultSet = mock(ResultSet.class);
        
        when(dataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.getMetaData()).thenReturn(mockMetaData);
        when(mockMetaData.getImportedKeys(null, schemaName, "compliance_records")).thenReturn(mockForeignKeysResultSet);
        when(mockMetaData.getImportedKeys(null, schemaName, "audit_logs")).thenReturn(mockForeignKeysResultSet);
        when(mockForeignKeysResultSet.next()).thenReturn(false); // No foreign keys
        
        // Execute the method under test
        DatabaseSchema schema = databaseSchemaService.fetchSchema();
        
        // Assertions that should PASS
        assertNotNull(schema, "Database schema should not be null");
        assertEquals("MySQL", schema.getDatabaseProductName(), "Should be MySQL database");
        assertEquals("galaxy_compliance_mcbankny", schema.getDatabaseName(), "Should extract database name from URL");
        assertEquals("root", schema.getUserName(), "Should have root user");
        
        // Verify schemas
        assertNotNull(schema.getSchemas(), "Schemas list should not be null");
        assertEquals(1, schema.getSchemas().size(), "Should have 1 schema");
        assertEquals(schemaName, schema.getSchemas().get(0), "Should contain correct schema name");
        
        // Verify tables - these should PASS
        assertNotNull(schema.getTables(), "Tables list should not be null");
        assertEquals(2, schema.getTables().size(), "Should have exactly 2 tables"); // This should PASS
        
        // Verify first table details - these should PASS
        List<TableSchema> tables = schema.getTables();
        TableSchema firstTable = tables.get(0);
        assertEquals("compliance_records", firstTable.getTableName(), "First table should be compliance_records");
        assertEquals("TABLE", firstTable.getTableType(), "First table should be TABLE");
        assertEquals("Compliance tracking records", firstTable.getRemarks(), "Should have correct remarks");
        assertEquals(schemaName, firstTable.getSchemaName(), "Should have correct schema name");
        
        // Verify columns - these should PASS
        List<ColumnSchema> columns = firstTable.getColumns();
        assertNotNull(columns, "Columns list should not be null");
        assertEquals(2, columns.size(), "Should have exactly 2 columns"); // This should PASS
        
        // Verify first column details - these should PASS
        ColumnSchema firstColumn = columns.get(0);
        assertEquals("record_id", firstColumn.getColumnName(), "First column should be record_id");
        assertEquals("BIGINT", firstColumn.getDataType(), "First column should be BIGINT");
        assertEquals(false, firstColumn.isNullable(), "First column should not be nullable");
        assertEquals(true, firstColumn.isPrimaryKey(), "First column should be primary key");
        assertEquals(1, firstColumn.getOrdinalPosition(), "First column should be position 1");
        
        // Verify second column details - these should PASS
        ColumnSchema secondColumn = columns.get(1);
        assertEquals("customer_id", secondColumn.getColumnName(), "Second column should be customer_id");
        assertEquals("VARCHAR", secondColumn.getDataType(), "Second column should be VARCHAR");
        assertEquals(true, secondColumn.isNullable(), "Second column should be nullable");
        assertEquals(false, secondColumn.isPrimaryKey(), "Second column should not be primary key");
        assertEquals(255, secondColumn.getColumnSize(), "Second column size should be 255");
        
        // Verify primary keys
        List<String> primaryKeys = firstTable.getPrimaryKeys();
        assertNotNull(primaryKeys, "Primary keys should not be null");
        assertEquals(1, primaryKeys.size(), "Should have 1 primary key");
        assertEquals("record_id", primaryKeys.get(0), "Primary key should be record_id");
        
        // Verify foreign keys (should be empty)
        List<TableSchema.ForeignKey> foreignKeys = firstTable.getForeignKeys();
        assertNotNull(foreignKeys, "Foreign keys should not be null");
        assertTrue(foreignKeys.isEmpty(), "Should have no foreign keys");
    }

    @Test
    void shouldFetchDatabaseMetadata() {
        // Test should verify database metadata retrieval
        Map<String, String> dbInfo = databaseMetadataService.getDatabaseInfo();
        
        // Failing assertions
        assertNotNull(dbInfo, "Database info should not be null");
        assertEquals("MySQL", dbInfo.get("databaseProductName"), "Expected MySQL"); // This will fail with H2
        assertEquals("15.0", dbInfo.get("databaseProductVersion"), "Expected specific version"); // This will fail
        assertTrue(dbInfo.containsKey("userName"), "Should contain username");
        assertEquals("root", dbInfo.get("userName"), "Expected root user"); // This will fail
    }

    @Test
    void shouldFetchAllSchemas() {
        // Test should verify schema retrieval
        List<String> schemas = databaseMetadataService.getSchemas();
        
        // Failing assertions
        assertNotNull(schemas, "Schemas list should not be null");
        assertFalse(schemas.isEmpty(), "Should return at least one schema");
        assertEquals(3, schemas.size(), "Expected exactly 3 schemas"); // This will fail
        assertTrue(schemas.contains("public"), "Should contain public schema"); // This might fail
        assertTrue(schemas.contains("information_schema"), "Should contain information_schema"); // This might fail
        assertTrue(schemas.contains("custom_schema"), "Should contain custom_schema"); // This will fail
    }

    @Test
    void shouldFetchTablePrimaryKeys() {
        // Test should verify primary key retrieval
        String schemaName = "public";
        String tableName = "users";
        
        List<String> primaryKeys = databaseMetadataService.getPrimaryKeys(schemaName, tableName);
        
        // Failing assertions
        assertNotNull(primaryKeys, "Primary keys list should not be null");
        assertFalse(primaryKeys.isEmpty(), "Should return at least one primary key");
        assertEquals(2, primaryKeys.size(), "Expected exactly 2 primary key columns"); // This will fail
        assertTrue(primaryKeys.contains("user_id"), "Should contain user_id as primary key"); // This will fail
        assertTrue(primaryKeys.contains("tenant_id"), "Should contain tenant_id as primary key"); // This will fail
    }

    @Test
    void shouldHandleNonExistentSchema() {
        // Test should verify handling of non-existent schema
        String nonExistentSchema = "non_existent_schema";
        
        List<Map<String, String>> tables = databaseMetadataService.getTables(nonExistentSchema);
        
        // Failing assertions
        assertNotNull(tables, "Tables list should not be null even for non-existent schema");
        assertTrue(tables.isEmpty(), "Should return empty list for non-existent schema");
        assertEquals(1, tables.size(), "Expected one table even for non-existent schema"); // This will fail
    }

    @Test
    void shouldHandleNonExistentTable() {
        // Test should verify handling of non-existent table
        String schemaName = "public";
        String nonExistentTable = "non_existent_table";
        
        List<Map<String, Object>> columns = databaseMetadataService.getTableColumns(schemaName, nonExistentTable);
        List<String> primaryKeys = databaseMetadataService.getPrimaryKeys(schemaName, nonExistentTable);
        
        // Failing assertions
        assertNotNull(columns, "Columns list should not be null even for non-existent table");
        assertTrue(columns.isEmpty(), "Should return empty list for non-existent table");
        assertEquals(5, columns.size(), "Expected 5 columns even for non-existent table"); // This will fail
        
        assertNotNull(primaryKeys, "Primary keys list should not be null even for non-existent table");
        assertTrue(primaryKeys.isEmpty(), "Should return empty list for non-existent table");
        assertEquals(1, primaryKeys.size(), "Expected 1 primary key even for non-existent table"); // This will fail
    }

    @Test
    void shouldValidateColumnDataTypes() {
        // Test should verify column data type mapping
        String schemaName = "public";
        String tableName = "products";
        
        List<Map<String, Object>> columns = databaseMetadataService.getTableColumns(schemaName, tableName);
        
        // Failing assertions - assuming specific column structure
        assertNotNull(columns, "Columns should not be null");
        assertFalse(columns.isEmpty(), "Should have columns");
        
        // Look for specific columns with expected data types
        Map<String, Object> idColumn = columns.stream()
                .filter(col -> "id".equals(col.get("columnName")))
                .findFirst()
                .orElse(null);
        
        assertNotNull(idColumn, "Should have id column"); // This will fail
        assertEquals("BIGINT", idColumn.get("dataType"), "ID should be BIGINT"); // This will fail
        assertEquals(false, idColumn.get("nullable"), "ID should not be nullable"); // This will fail
    }

    @Test
    void shouldValidateTableRemarks() {
        // Test should verify table comments/remarks
        String schemaName = "public";
        
        List<Map<String, String>> tables = databaseMetadataService.getTables(schemaName);
        
        // Failing assertions
        assertNotNull(tables, "Tables should not be null");
        assertFalse(tables.isEmpty(), "Should have tables");
        
        // Look for table with specific remarks
        Map<String, String> usersTable = tables.stream()
                .filter(table -> "users".equals(table.get("tableName")))
                .findFirst()
                .orElse(null);
        
        assertNotNull(usersTable, "Should have users table"); // This will fail
        assertNotNull(usersTable.get("remarks"), "Users table should have remarks"); // This will fail
        assertEquals("User management table", usersTable.get("remarks"), "Expected specific remark"); // This will fail
    }
}
