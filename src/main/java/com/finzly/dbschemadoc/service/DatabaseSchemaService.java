package com.finzly.dbschemadoc.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finzly.dbschemadoc.model.ColumnSchema;
import com.finzly.dbschemadoc.model.DatabaseSchema;
import com.finzly.dbschemadoc.model.TableSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for extracting complete database schema information.
 * All operations are read-only and do not perform any DDL/DML.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DatabaseSchemaService {

    private final DataSource dataSource;
    private final DatabaseMetadataService databaseMetadataService;
    private final ResourceLoader resourceLoader;
    
    @Value("${app.schema.descriptions.file.path:classpath:schema-descriptions.json}")
    private String schemaDescriptionsFilePath;
    
    @Value("${app.reports.output.directory:./reports}")
    private String reportsOutputDirectory;
    
    @Value("${app.reports.html.filename:database-schema-report.html}")
    private String htmlReportFilename;
    
    @Value("${app.reports.docx.filename:database-schema-report.docx}")
    private String docxReportFilename;
    
    @Value("${app.database.default.schema:galaxy_compliance_mcbankny}")
    private String defaultSchema;

    /**
     * Fetch complete database schema including tables, columns, PKs, FKs.
     */
    public DatabaseSchema fetchSchema() {
        return fetchSchema(null);
    }
    
    /**
     * Fetch complete database schema including tables, columns, PKs, FKs for a specific schema.
     * @param schemaName The schema name to process, or null to use default schema
     */
    public DatabaseSchema fetchSchema(String schemaName) {
        try {
            // Use provided schema name or default
            String targetSchema = (schemaName != null && !schemaName.trim().isEmpty()) ? schemaName.trim() : defaultSchema;
            log.info("Starting database schema extraction for specific schema: {}", targetSchema);
            
            // Get database info
            Map<String, String> dbInfo = databaseMetadataService.getDatabaseInfo();
            
            // Process only the specified schema instead of all schemas
            List<String> schemas = Arrays.asList(targetSchema);
            List<TableSchema> allTables = new ArrayList<>();
            
            log.info("Processing single schema: {}", targetSchema);
            
            // Get tables list first
            List<Map<String, String>> tablesInSchema = databaseMetadataService.getTables(targetSchema);
            log.info("Found {} tables in schema: {}", tablesInSchema.size(), targetSchema);
            
            // Process tables with optimized connection management
            try (Connection connection = dataSource.getConnection()) {
                // Ensure read-only mode for metadata queries
                connection.setReadOnly(true);
                connection.setAutoCommit(true); // Ensure autocommit for metadata queries
                DatabaseMetaData metaData = connection.getMetaData();
                
                for (Map<String, String> tableInfo : tablesInSchema) {
                    String tableName = tableInfo.get("tableName");
                    log.debug("Processing table: {} in schema: {}", tableName, targetSchema);
                    
                    try {
                        // Get all metadata for this table in one go
                        TableSchema tableSchema = buildTableSchemaOptimized(targetSchema, tableName, tableInfo, metaData);
                        allTables.add(tableSchema);
                        
                    } catch (Exception e) {
                        log.warn("Failed to process table {}: {}", tableName, e.getMessage());
                        // Create a basic table schema without detailed metadata
                        TableSchema basicTable = TableSchema.builder()
                                .tableName(tableName)
                                .tableType(tableInfo.get("tableType"))
                                .remarks(tableInfo.get("remarks"))
                                .schemaName(targetSchema)
                                .columns(new ArrayList<>())
                                .primaryKeys(new ArrayList<>())
                                .foreignKeys(new ArrayList<>())
                                .build();
                        allTables.add(basicTable);
                    }
                }
            }
            
            log.info("Extracted schema for 1 schema ({}) with {} total tables", targetSchema, allTables.size());
            
            return DatabaseSchema.builder()
                    .databaseName(targetSchema) // Use the specific schema as database name for clarity
                    .databaseProductName(dbInfo.get("databaseProductName"))
                    .databaseProductVersion(dbInfo.get("databaseProductVersion"))
                    .userName(dbInfo.get("userName"))
                    .url(dbInfo.get("url"))
                    .schemas(schemas)
                    .tables(allTables)
                    .databaseInfo(dbInfo)
                    .build();
                    
        } catch (Exception e) {
            log.error("Error extracting database schema: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to extract database schema", e);
        }
    }

    /**
     * Fetch tables for a specific schema with complete metadata.
     */
    private List<TableSchema> fetchTablesForSchema(String schemaName) {
        List<Map<String, String>> tables = databaseMetadataService.getTables(schemaName);
        List<TableSchema> tableSchemas = new ArrayList<>();
        
        for (Map<String, String> table : tables) {
            String tableName = table.get("tableName");
            
            // Get columns
            List<ColumnSchema> columns = fetchColumnsForTable(schemaName, tableName);
            
            // Get primary keys
            List<String> primaryKeys = databaseMetadataService.getPrimaryKeys(schemaName, tableName);
            
            // Get foreign keys
            List<TableSchema.ForeignKey> foreignKeys = fetchForeignKeysForTable(schemaName, tableName);
            
            // Mark primary key columns
            Set<String> pkSet = new HashSet<>(primaryKeys);
            columns.forEach(col -> col.setPrimaryKey(pkSet.contains(col.getColumnName())));
            
            // Mark foreign key columns
            Map<String, TableSchema.ForeignKey> fkMap = foreignKeys.stream()
                    .collect(Collectors.toMap(TableSchema.ForeignKey::getColumnName, fk -> fk));
            columns.forEach(col -> {
                TableSchema.ForeignKey fk = fkMap.get(col.getColumnName());
                if (fk != null) {
                    col.setForeignKey(true);
                    col.setReferencedTable(fk.getReferencedTable());
                    col.setReferencedColumn(fk.getReferencedColumn());
                }
            });
            
            TableSchema tableSchema = TableSchema.builder()
                    .tableName(tableName)
                    .tableType(table.get("tableType"))
                    .remarks(table.get("remarks"))
                    .schemaName(schemaName)
                    .columns(columns)
                    .primaryKeys(primaryKeys)
                    .foreignKeys(foreignKeys)
                    .build();
                    
            tableSchemas.add(tableSchema);
        }
        
        return tableSchemas;
    }

    /**
     * Fetch columns for a specific table.
     */
    private List<ColumnSchema> fetchColumnsForTable(String schemaName, String tableName) {
        List<Map<String, Object>> columns = databaseMetadataService.getTableColumns(schemaName, tableName);
        
        return columns.stream()
                .map(col -> ColumnSchema.builder()
                        .columnName((String) col.get("columnName"))
                        .dataType((String) col.get("dataType"))
                        .columnSize((Integer) col.get("columnSize"))
                        .nullable((Boolean) col.get("nullable"))
                        .defaultValue((String) col.get("defaultValue"))
                        .remarks((String) col.get("remarks"))
                        .ordinalPosition((Integer) col.get("ordinalPosition"))
                        .isPrimaryKey(false) // Will be set later
                        .isForeignKey(false) // Will be set later
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Fetch foreign keys for a specific table.
     */
    private List<TableSchema.ForeignKey> fetchForeignKeysForTable(String schemaName, String tableName) {
        List<TableSchema.ForeignKey> foreignKeys = new ArrayList<>();
        
        try {
            DatabaseMetaData metaData = dataSource.getConnection().getMetaData();
            ResultSet rs = metaData.getImportedKeys(null, schemaName, tableName);
            
            while (rs.next()) {
                TableSchema.ForeignKey fk = TableSchema.ForeignKey.builder()
                        .columnName(rs.getString("FKCOLUMN_NAME"))
                        .referencedTable(rs.getString("PKTABLE_NAME"))
                        .referencedColumn(rs.getString("PKCOLUMN_NAME"))
                        .constraintName(rs.getString("FK_NAME"))
                        .build();
                        
                foreignKeys.add(fk);
            }
            
            log.debug("Found {} foreign keys for table '{}.{}'", foreignKeys.size(), schemaName, tableName);
            
        } catch (SQLException e) {
            log.error("Error retrieving foreign keys for table '{}.{}': {}", schemaName, tableName, e.getMessage());
            // Don't throw exception, just return empty list
        }
        
        return foreignKeys;
    }

    /**
     * Extract database name from JDBC URL.
     */
    private String extractDatabaseName(String url) {
        if (url == null) return "unknown";
        
        // Handle MySQL URLs: jdbc:mysql://host:port/database
        if (url.contains("mysql")) {
            int lastSlash = url.lastIndexOf('/');
            if (lastSlash > 0 && lastSlash < url.length() - 1) {
                String dbPart = url.substring(lastSlash + 1);
                // Remove query parameters
                int questionMark = dbPart.indexOf('?');
                return questionMark > 0 ? dbPart.substring(0, questionMark) : dbPart;
            }
        }
        
        // Handle PostgreSQL URLs: jdbc:postgresql://host:port/database
        if (url.contains("postgresql")) {
            int lastSlash = url.lastIndexOf('/');
            if (lastSlash > 0 && lastSlash < url.length() - 1) {
                String dbPart = url.substring(lastSlash + 1);
                int questionMark = dbPart.indexOf('?');
                return questionMark > 0 ? dbPart.substring(0, questionMark) : dbPart;
            }
        }
        
        return "unknown";
    }

    /**
     * Generate HTML report from database schema.
     * Creates simple HTML with table headings and column details.
     * 
     * @param schema DatabaseSchema object to generate report from
     * @return HTML string representation of the schema
     */
    public String generateHtmlReport(DatabaseSchema schema) {
        log.info("Generating HTML report for database: {}", schema.getDatabaseName());
        
        StringBuilder html = new StringBuilder();
        
        // HTML document structure
        html.append("<!DOCTYPE html>\n")
            .append("<html>\n")
            .append("<head>\n")
            .append("    <title>Database Schema Report</title>\n")
            .append("    <style>\n")
            .append("        body { font-family: Arial, sans-serif; margin: 20px; }\n")
            .append("        table { border-collapse: collapse; width: 100%; margin-bottom: 20px; }\n")
            .append("        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }\n")
            .append("        th { background-color: #f2f2f2; }\n")
            .append("        .primary-key { font-weight: bold; color: #d9534f; }\n")
            .append("        .foreign-key { color: #f0ad4e; }\n")
            .append("        h1 { color: #333; }\n")
            .append("        h2 { color: #555; border-bottom: 2px solid #eee; padding-bottom: 5px; }\n")
            .append("    </style>\n")
            .append("</head>\n")
            .append("<body>\n");
        
        // Database header
        html.append("    <h1>").append(schema.getDatabaseProductName()).append(" Database Schema</h1>\n");
        html.append("    <p><strong>Database:</strong> ").append(schema.getDatabaseName()).append("</p>\n");
        html.append("    <p><strong>User:</strong> ").append(schema.getUserName()).append("</p>\n");
        
        // Database statistics
        html.append("    <h2>Database Tables</h2>\n");
        html.append("    <p>Total Tables: ").append(schema.getTables().size()).append("</p>\n");
        
        // Generate table sections
        for (TableSchema table : schema.getTables()) {
            // Table name as h2 heading
            html.append("    <h2>").append(table.getTableName()).append("</h2>\n");
            
            if (table.getRemarks() != null && !table.getRemarks().isEmpty()) {
                html.append("    <p><em>").append(table.getRemarks()).append("</em></p>\n");
            }
            
            // Column Count information
            html.append("    <p>Column Count: ").append(table.getColumns().size()).append("</p>\n");
            
            // Table Type information
            html.append("    <p>Table Type: ").append(table.getTableType()).append("</p>\n");
            
            // Create table with column details
            html.append("    <table>\n");
            html.append("        <tr>\n");
            html.append("            <th>Column Name</th>\n");
            html.append("            <th>Type</th>\n");
            html.append("            <th>Constraints</th>\n");
            html.append("            <th>Description</th>\n");
            html.append("        </tr>\n");
            
            for (ColumnSchema column : table.getColumns()) {
                html.append("        <tr>\n");
                
                // Column name (with CSS class if primary key)
                String columnClass = column.isPrimaryKey() ? " class=\"primary-key\"" : "";
                html.append("            <td").append(columnClass).append(">")
                    .append(column.getColumnName()).append("</td>\n");
                
                // Data type with size
                html.append("            <td>").append(column.getDataType());
                if (column.getColumnSize() != null && column.getColumnSize() > 0) {
                    html.append("(").append(column.getColumnSize()).append(")");
                }
                html.append("</td>\n");
                
                // Constraints
                html.append("            <td>");
                List<String> constraints = new ArrayList<>();
                if (column.isPrimaryKey()) {
                    constraints.add("PRIMARY KEY");
                }
                if (column.isForeignKey()) {
                    constraints.add("FOREIGN KEY");
                }
                if (!column.isNullable()) {
                    constraints.add("NOT NULL");
                }
                if (column.getDefaultValue() != null) {
                    constraints.add("DEFAULT: " + column.getDefaultValue());
                }
                if (constraints.isEmpty()) {
                    html.append("-");
                } else {
                    html.append(String.join(", ", constraints));
                }
                html.append("</td>\n");
                
                // Description
                html.append("            <td>")
                    .append(column.getRemarks() != null ? column.getRemarks() : "-")
                    .append("</td>\n");
                
                html.append("        </tr>\n");
            }
            
            html.append("    </table>\n");
            
            // Foreign Key Relationships section (if any)
            if (!table.getForeignKeys().isEmpty()) {
                html.append("    <h3>Foreign Key Relationships</h3>\n");
                html.append("    <ul>\n");
                for (TableSchema.ForeignKey fk : table.getForeignKeys()) {
                    html.append("        <li>").append(fk.getColumnName())
                        .append(" → ").append(fk.getReferencedTable())
                        .append(".").append(fk.getReferencedColumn()).append("</li>\n");
                }
                html.append("    </ul>\n");
            }
        }
        
        // Index Information placeholder
        html.append("    <h2>Index Information</h2>\n");
        html.append("    <p>Index details would be displayed here.</p>\n");
        
        html.append("</body>\n")
            .append("</html>");
        
        log.info("Generated HTML report with {} tables", schema.getTables().size());
        return html.toString();
    }

    /**
     * Generate DOCX report from database schema using Apache POI.
     * Creates a Word document with schema information and table details.
     * 
     * @param schema DatabaseSchema object to generate report from
     * @return byte array of the DOCX document
     * @throws IOException if there's an error creating the document
     */
    public byte[] generateDocxReport(DatabaseSchema schema) throws IOException {
        log.info("Generating DOCX report for database: {}", schema.getDatabaseName());
        
        XWPFDocument document = new XWPFDocument();
        
        try {
            // Add title "Database Schema Documentation"
            XWPFParagraph titleParagraph = document.createParagraph();
            titleParagraph.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = titleParagraph.createRun();
            titleRun.setText("Database Schema Documentation");
            titleRun.setBold(true);
            titleRun.setFontSize(18);
            titleRun.setFontFamily("Calibri");
            
            // Add database information
            XWPFParagraph dbInfoParagraph = document.createParagraph();
            XWPFRun dbInfoRun = dbInfoParagraph.createRun();
            dbInfoRun.setText("Database: " + schema.getDatabaseName() + " (" + schema.getDatabaseProductName() + ")");
            dbInfoRun.setFontSize(12);
            dbInfoRun.setFontFamily("Calibri");
            
            // Add user information
            XWPFParagraph userParagraph = document.createParagraph();
            XWPFRun userRun = userParagraph.createRun();
            userRun.setText("User: " + schema.getUserName());
            userRun.setFontSize(12);
            userRun.setFontFamily("Calibri");
            
            // Add table count
            XWPFParagraph tableCountParagraph = document.createParagraph();
            XWPFRun tableCountRun = tableCountParagraph.createRun();
            tableCountRun.setText("Total Tables: " + schema.getTables().size());
            tableCountRun.setFontSize(12);
            tableCountRun.setFontFamily("Calibri");
            
            // Add space
            document.createParagraph();
            
            // For each table: create a Word table listing columns
            for (TableSchema table : schema.getTables()) {
                // Table name heading
                XWPFParagraph tableNameParagraph = document.createParagraph();
                XWPFRun tableNameRun = tableNameParagraph.createRun();
                tableNameRun.setText("Table: " + table.getTableName());
                tableNameRun.setBold(true);
                tableNameRun.setFontSize(14);
                tableNameRun.setFontFamily("Calibri");
                
                // Table description
                if (table.getRemarks() != null && !table.getRemarks().isEmpty()) {
                    XWPFParagraph descParagraph = document.createParagraph();
                    XWPFRun descRun = descParagraph.createRun();
                    descRun.setText("Description: " + table.getRemarks());
                    descRun.setItalic(true);
                    descRun.setFontSize(11);
                    descRun.setFontFamily("Calibri");
                }
                
                // Create Word table for columns
                if (!table.getColumns().isEmpty()) {
                    XWPFTable wordTable = document.createTable(table.getColumns().size() + 1, 4);
                    wordTable.setWidth("100%");
                    
                    // Header row
                    XWPFTableRow headerRow = wordTable.getRow(0);
                    setTableCellText(headerRow.getCell(0), "Column Name", true);
                    setTableCellText(headerRow.getCell(1), "Data Type", true);
                    setTableCellText(headerRow.getCell(2), "Constraints", true);
                    setTableCellText(headerRow.getCell(3), "Description", true);
                    
                    // Data rows
                    for (int i = 0; i < table.getColumns().size(); i++) {
                        ColumnSchema column = table.getColumns().get(i);
                        XWPFTableRow row = wordTable.getRow(i + 1);
                        
                        // Column name (bold if primary key)
                        setTableCellText(row.getCell(0), column.getColumnName(), column.isPrimaryKey());
                        
                        // Data type with size
                        String dataType = column.getDataType();
                        if (column.getColumnSize() != null && column.getColumnSize() > 0) {
                            dataType += "(" + column.getColumnSize() + ")";
                        }
                        setTableCellText(row.getCell(1), dataType, false);
                        
                        // Constraints
                        List<String> constraints = new ArrayList<>();
                        if (column.isPrimaryKey()) {
                            constraints.add("PRIMARY KEY");
                        }
                        if (column.isForeignKey()) {
                            constraints.add("FOREIGN KEY");
                        }
                        if (!column.isNullable()) {
                            constraints.add("NOT NULL");
                        }
                        if (column.getDefaultValue() != null) {
                            constraints.add("DEFAULT: " + column.getDefaultValue());
                        }
                        String constraintText = constraints.isEmpty() ? "-" : String.join(", ", constraints);
                        setTableCellText(row.getCell(2), constraintText, false);
                        
                        // Description
                        String description = column.getRemarks() != null ? column.getRemarks() : "-";
                        setTableCellText(row.getCell(3), description, false);
                    }
                }
                
                // Add space between tables
                document.createParagraph();
            }
            
            // Convert to byte array
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.write(out);
            byte[] result = out.toByteArray();
            
            log.info("Generated DOCX report with {} tables, size: {} bytes", schema.getTables().size(), result.length);
            return result;
            
        } finally {
            document.close();
        }
    }

    /**
     * Helper method to set text content in a table cell.
     */
    private void setTableCellText(XWPFTableCell cell, String text, boolean bold) {
        XWPFParagraph paragraph = cell.getParagraphs().get(0);
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setBold(bold);
        run.setFontSize(10);
        run.setFontFamily("Calibri");
        
        if (bold) {
            // Set background color for header cells or primary key cells
            cell.setColor("F0F0F0");
        }
    }

    /**
     * Enrich database schema with descriptions from schema-descriptions.json file.
     * This method loads external descriptions and applies them to tables and columns.
     * 
     * @param schema The original database schema to enrich
     * @return Enriched database schema with enhanced descriptions
     */
    public DatabaseSchema enrichSchemaWithDescriptions(DatabaseSchema schema) {
        log.info("Enriching schema with descriptions from schema-descriptions.json");
        
        try {
            // Load descriptions from JSON file
            SchemaDescriptions descriptions = loadSchemaDescriptions();
            
            // Create enriched tables
            List<TableSchema> enrichedTables = schema.getTables().stream()
                    .map(table -> enrichTableWithDescriptions(table, descriptions))
                    .collect(Collectors.toList());
            
            // Return enriched schema
            DatabaseSchema enrichedSchema = DatabaseSchema.builder()
                    .databaseName(schema.getDatabaseName())
                    .databaseProductName(schema.getDatabaseProductName())
                    .databaseProductVersion(schema.getDatabaseProductVersion())
                    .userName(schema.getUserName())
                    .url(schema.getUrl())
                    .schemas(schema.getSchemas())
                    .tables(enrichedTables)
                    .databaseInfo(schema.getDatabaseInfo())
                    .build();
            
            log.info("Successfully enriched schema with descriptions for {} tables", enrichedTables.size());
            return enrichedSchema;
            
        } catch (Exception e) {
            log.warn("Failed to enrich schema with descriptions: {}", e.getMessage());
            // Return original schema if enrichment fails
            return schema;
        }
    }

    /**
     * Load schema descriptions from the configured JSON file path.
     */
    private SchemaDescriptions loadSchemaDescriptions() throws IOException {
        log.info("Loading schema descriptions from: {}", schemaDescriptionsFilePath);
        Resource resource = resourceLoader.getResource(schemaDescriptionsFilePath);
        
        if (!resource.exists()) {
            log.warn("Schema descriptions file not found at: {}. Using empty descriptions.", schemaDescriptionsFilePath);
            return new SchemaDescriptions(); // Return empty descriptions
        }
        
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(resource.getInputStream(), SchemaDescriptions.class);
    }

    /**
     * Enrich a single table with descriptions.
     */
    private TableSchema enrichTableWithDescriptions(TableSchema table, SchemaDescriptions descriptions) {
        TableDescriptions tableDesc = descriptions.getTables().get(table.getTableName());
        
        if (tableDesc == null) {
            // No descriptions available for this table, return as-is
            return table;
        }
        
        // Enrich table description
        String enrichedTableRemarks = table.getRemarks();
        if (tableDesc.getDescription() != null && !tableDesc.getDescription().isEmpty()) {
            enrichedTableRemarks = tableDesc.getDescription();
        }
        
        // Enrich column descriptions
        List<ColumnSchema> enrichedColumns = table.getColumns().stream()
                .map(column -> enrichColumnWithDescriptions(column, tableDesc))
                .collect(Collectors.toList());
        
        return TableSchema.builder()
                .tableName(table.getTableName())
                .tableType(table.getTableType())
                .remarks(enrichedTableRemarks)
                .schemaName(table.getSchemaName())
                .columns(enrichedColumns)
                .primaryKeys(table.getPrimaryKeys())
                .foreignKeys(table.getForeignKeys())
                .build();
    }

    /**
     * Enrich a single column with descriptions.
     */
    private ColumnSchema enrichColumnWithDescriptions(ColumnSchema column, TableDescriptions tableDesc) {
        String columnDescription = tableDesc.getColumns().get(column.getColumnName());
        
        String enrichedColumnRemarks = column.getRemarks();
        if (columnDescription != null && !columnDescription.isEmpty()) {
            enrichedColumnRemarks = columnDescription;
        }
        
        return ColumnSchema.builder()
                .columnName(column.getColumnName())
                .dataType(column.getDataType())
                .columnSize(column.getColumnSize())
                .nullable(column.isNullable())
                .defaultValue(column.getDefaultValue())
                .remarks(enrichedColumnRemarks)
                .ordinalPosition(column.getOrdinalPosition())
                .referencedTable(column.getReferencedTable())
                .referencedColumn(column.getReferencedColumn())
                .isPrimaryKey(column.isPrimaryKey())
                .isForeignKey(column.isForeignKey())
                .build();
    }

    /**
     * Helper classes for JSON deserialization.
     */
    public static class SchemaDescriptions {
        private Map<String, TableDescriptions> tables = new HashMap<>();
        
        public Map<String, TableDescriptions> getTables() {
            return tables;
        }
        
        public void setTables(Map<String, TableDescriptions> tables) {
            this.tables = tables;
        }
    }

    public static class TableDescriptions {
        private String description;
        private Map<String, String> columns = new HashMap<>();
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
        
        public Map<String, String> getColumns() {
            return columns;
        }
        
        public void setColumns(Map<String, String> columns) {
            this.columns = columns;
        }
    }
    
    /**
     * Get columns for a specific table.
     */
    private List<ColumnSchema> getColumnsForTable(String schemaName, String tableName) {
        List<Map<String, Object>> columnMaps = databaseMetadataService.getTableColumns(schemaName, tableName);
        List<ColumnSchema> columns = new ArrayList<>();
        
        for (Map<String, Object> columnMap : columnMaps) {
            ColumnSchema column = ColumnSchema.builder()
                    .columnName((String) columnMap.get("columnName"))
                    .dataType((String) columnMap.get("dataType"))
                    .columnSize((Integer) columnMap.get("columnSize"))
                    .nullable((Boolean) columnMap.get("nullable"))
                    .defaultValue((String) columnMap.get("defaultValue"))
                    .ordinalPosition((Integer) columnMap.get("ordinalPosition"))
                    .remarks((String) columnMap.get("remarks"))
                    .isPrimaryKey(false) // Will be set later when processing primary keys
                    .isForeignKey(false) // Will be set later when processing foreign keys
                    .build();
            columns.add(column);
        }
        
        return columns;
    }
    
    /**
     * Build optimized table schema with single connection.
     */
    private TableSchema buildTableSchemaOptimized(String schemaName, String tableName, 
                                                 Map<String, String> tableInfo, DatabaseMetaData metaData) throws SQLException {
        
        List<ColumnSchema> columns = new ArrayList<>();
        List<String> primaryKeys = new ArrayList<>();
        List<TableSchema.ForeignKey> foreignKeys = new ArrayList<>();
        
        // Get columns with optimized query
        String productName = metaData.getDatabaseProductName().toLowerCase();
        
        try (ResultSet columnsRs = productName.contains("mysql") ? 
             metaData.getColumns(schemaName, null, tableName, "%") :
             metaData.getColumns(null, schemaName, tableName, "%")) {
            
            while (columnsRs.next()) {
                ColumnSchema column = ColumnSchema.builder()
                        .columnName(columnsRs.getString("COLUMN_NAME"))
                        .dataType(columnsRs.getString("TYPE_NAME"))
                        .columnSize(columnsRs.getInt("COLUMN_SIZE"))
                        .nullable(columnsRs.getInt("NULLABLE") == DatabaseMetaData.columnNullable)
                        .defaultValue(columnsRs.getString("COLUMN_DEF"))
                        .ordinalPosition(columnsRs.getInt("ORDINAL_POSITION"))
                        .remarks(columnsRs.getString("REMARKS"))
                        .isPrimaryKey(false)
                        .isForeignKey(false)
                        .build();
                columns.add(column);
            }
        }
        
        // Get primary keys
        try (ResultSet pkRs = productName.contains("mysql") ?
             metaData.getPrimaryKeys(schemaName, null, tableName) :
             metaData.getPrimaryKeys(null, schemaName, tableName)) {
            
            while (pkRs.next()) {
                String pkColumn = pkRs.getString("COLUMN_NAME");
                primaryKeys.add(pkColumn);
                // Mark column as primary key
                columns.stream()
                        .filter(col -> col.getColumnName().equals(pkColumn))
                        .findFirst()
                        .ifPresent(col -> col.setPrimaryKey(true));
            }
        } catch (SQLException e) {
            log.debug("Could not retrieve primary keys for {}.{}: {}", schemaName, tableName, e.getMessage());
        }
        
        // Skip foreign keys for now to improve performance
        // They can be added later if needed
        
        return TableSchema.builder()
                .tableName(tableName)
                .tableType(tableInfo.get("tableType"))
                .remarks(tableInfo.get("remarks"))
                .schemaName(schemaName)
                .columns(columns)
                .primaryKeys(primaryKeys)
                .foreignKeys(foreignKeys)
                .build();
    }
}
