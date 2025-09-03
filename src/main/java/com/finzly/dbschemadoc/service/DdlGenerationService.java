package com.finzly.dbschemadoc.service;

import com.finzly.dbschemadoc.model.ColumnSchema;
import com.finzly.dbschemadoc.model.DatabaseSchema;
import com.finzly.dbschemadoc.model.TableSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for generating DDL (Data Definition Language) scripts to recreate database structure.
 * Provides functionality to generate CREATE TABLE statements and other DDL from database schema.
 */
@Service
@Slf4j
public class DdlGenerationService {

    /**
     * Generate complete DDL script for the entire database schema.
     * 
     * @param databaseSchema The database schema to convert to DDL
     * @return DDL script as a string containing CREATE TABLE statements
     */
    public String generateDdlScript(DatabaseSchema databaseSchema) {
        log.info("Generating DDL script for database: {}", databaseSchema.getDatabaseName());
        
        StringBuilder ddlScript = new StringBuilder();
        
        // Add header comment
        ddlScript.append("-- DDL Script for Database: ").append(databaseSchema.getDatabaseName()).append("\n");
        ddlScript.append("-- Generated from ").append(databaseSchema.getDatabaseProductName()).append(" ").append(databaseSchema.getDatabaseProductVersion()).append("\n");
        ddlScript.append("-- Total Tables: ").append(databaseSchema.getTables().size()).append("\n");
        ddlScript.append("-- Generated on: ").append(java.time.LocalDateTime.now()).append("\n\n");
        
        // Get database type for SQL dialect-specific generation
        String databaseType = getDatabaseType(databaseSchema.getDatabaseProductName());
        
        // Generate CREATE TABLE statements for each table
        for (TableSchema table : databaseSchema.getTables()) {
            String tableCreateScript = generateCreateTableStatement(table, databaseType);
            ddlScript.append(tableCreateScript).append("\n\n");
        }
        
        // Generate indexes (after all tables are created)
        ddlScript.append("-- Indexes\n");
        for (TableSchema table : databaseSchema.getTables()) {
            String indexScript = generateIndexStatements(table, databaseType);
            if (!indexScript.trim().isEmpty()) {
                ddlScript.append(indexScript).append("\n");
            }
        }
        
        // Generate foreign key constraints (after all tables are created)
        ddlScript.append("-- Foreign Key Constraints\n");
        for (TableSchema table : databaseSchema.getTables()) {
            String foreignKeyScript = generateForeignKeyConstraints(table, databaseType);
            if (!foreignKeyScript.trim().isEmpty()) {
                ddlScript.append(foreignKeyScript).append("\n");
            }
        }
        
        log.info("Generated DDL script with {} tables", databaseSchema.getTables().size());
        return ddlScript.toString();
    }

    /**
     * Generate CREATE TABLE statement for a single table.
     */
    private String generateCreateTableStatement(TableSchema table, String databaseType) {
        StringBuilder createTable = new StringBuilder();
        
        // Table comment
        if (table.getRemarks() != null && !table.getRemarks().isEmpty()) {
            createTable.append("-- ").append(table.getRemarks()).append("\n");
        }
        
        // CREATE TABLE statement start
        createTable.append("CREATE TABLE ");
        if (table.getSchemaName() != null && !table.getSchemaName().isEmpty() && !databaseType.equals("mysql")) {
            createTable.append(escapeIdentifier(table.getSchemaName(), databaseType)).append(".");
        }
        createTable.append(escapeIdentifier(table.getTableName(), databaseType)).append(" (\n");
        
        // Column definitions
        List<String> columnDefinitions = table.getColumns().stream()
                .map(column -> generateColumnDefinition(column, databaseType))
                .collect(Collectors.toList());
        
        // Primary key constraint
        if (!table.getPrimaryKeys().isEmpty()) {
            String primaryKeyConstraint = generatePrimaryKeyConstraint(table.getPrimaryKeys(), databaseType);
            columnDefinitions.add(primaryKeyConstraint);
        }
        
        // Join all column definitions and constraints
        createTable.append("    ").append(String.join(",\n    ", columnDefinitions)).append("\n");
        
        createTable.append(")");
        
        // Add table-specific options based on database type
        if (databaseType.equals("mysql")) {
            createTable.append(" ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");
        }
        
        createTable.append(";");
        
        return createTable.toString();
    }

    /**
     * Generate column definition for a single column.
     */
    private String generateColumnDefinition(ColumnSchema column, String databaseType) {
        StringBuilder columnDef = new StringBuilder();
        
        // Column name
        columnDef.append(escapeIdentifier(column.getColumnName(), databaseType));
        
        // Data type with size
        columnDef.append(" ").append(mapDataType(column.getDataType(), column.getColumnSize(), databaseType));
        
        // NULL/NOT NULL constraint
        if (!column.isNullable()) {
            columnDef.append(" NOT NULL");
        }
        
        // Default value
        if (column.getDefaultValue() != null && !column.getDefaultValue().isEmpty()) {
            columnDef.append(" DEFAULT ").append(formatDefaultValue(column.getDefaultValue(), column.getDataType()));
        }
        
        // Column comment
        if (column.getRemarks() != null && !column.getRemarks().isEmpty()) {
            if (databaseType.equals("mysql")) {
                columnDef.append(" COMMENT '").append(escapeString(column.getRemarks())).append("'");
            } else if (databaseType.equals("postgresql")) {
                // PostgreSQL column comments are added separately with COMMENT ON COLUMN
            }
        }
        
        return columnDef.toString();
    }

    /**
     * Generate primary key constraint.
     */
    private String generatePrimaryKeyConstraint(List<String> primaryKeys, String databaseType) {
        StringBuilder constraint = new StringBuilder();
        constraint.append("PRIMARY KEY (");
        
        List<String> escapedKeys = primaryKeys.stream()
                .map(key -> escapeIdentifier(key, databaseType))
                .collect(Collectors.toList());
        
        constraint.append(String.join(", ", escapedKeys));
        constraint.append(")");
        
        return constraint.toString();
    }

    /**
     * Generate foreign key constraints for a table.
     */
    private String generateForeignKeyConstraints(TableSchema table, String databaseType) {
        if (table.getForeignKeys().isEmpty()) {
            return "";
        }
        
        StringBuilder foreignKeys = new StringBuilder();
        
        for (TableSchema.ForeignKey fk : table.getForeignKeys()) {
            foreignKeys.append("ALTER TABLE ");
            if (table.getSchemaName() != null && !table.getSchemaName().isEmpty() && !databaseType.equals("mysql")) {
                foreignKeys.append(escapeIdentifier(table.getSchemaName(), databaseType)).append(".");
            }
            foreignKeys.append(escapeIdentifier(table.getTableName(), databaseType));
            
            foreignKeys.append(" ADD CONSTRAINT ");
            if (fk.getConstraintName() != null && !fk.getConstraintName().isEmpty()) {
                foreignKeys.append(escapeIdentifier(fk.getConstraintName(), databaseType));
            } else {
                // Generate constraint name if not provided
                String constraintName = "fk_" + table.getTableName() + "_" + fk.getColumnName();
                foreignKeys.append(escapeIdentifier(constraintName, databaseType));
            }
            
            foreignKeys.append(" FOREIGN KEY (").append(escapeIdentifier(fk.getColumnName(), databaseType)).append(")");
            foreignKeys.append(" REFERENCES ").append(escapeIdentifier(fk.getReferencedTable(), databaseType));
            foreignKeys.append(" (").append(escapeIdentifier(fk.getReferencedColumn(), databaseType)).append(")");
            foreignKeys.append(";\n");
        }
        
        return foreignKeys.toString();
    }

    /**
     * Generate CREATE INDEX statements for a table.
     */
    private String generateIndexStatements(TableSchema table, String databaseType) {
        if (table.getIndexes() == null || table.getIndexes().isEmpty()) {
            return "";
        }
        
        StringBuilder indexStatements = new StringBuilder();
        
        for (TableSchema.IndexInfo index : table.getIndexes()) {
            if (index.getColumns() == null || index.getColumns().isEmpty()) {
                continue;
            }
            
            indexStatements.append("CREATE ");
            
            // Add UNIQUE if the index is unique
            if (index.isUnique()) {
                indexStatements.append("UNIQUE ");
            }
            
            indexStatements.append("INDEX ");
            indexStatements.append(escapeIdentifier(index.getIndexName(), databaseType));
            indexStatements.append(" ON ");
            
            // Add schema name if needed
            if (table.getSchemaName() != null && !table.getSchemaName().isEmpty() && !databaseType.equals("mysql")) {
                indexStatements.append(escapeIdentifier(table.getSchemaName(), databaseType)).append(".");
            }
            indexStatements.append(escapeIdentifier(table.getTableName(), databaseType));
            
            // Add column list
            indexStatements.append(" (");
            List<String> escapedColumns = index.getColumns().stream()
                    .map(column -> escapeIdentifier(column, databaseType))
                    .collect(Collectors.toList());
            indexStatements.append(String.join(", ", escapedColumns));
            indexStatements.append(")");
            
            // Add database-specific options
            if (databaseType.equals("mysql")) {
                indexStatements.append(" USING BTREE");
            }
            
            indexStatements.append(";\n");
        }
        
        return indexStatements.toString();
    }

    /**
     * Determine database type from product name.
     */
    private String getDatabaseType(String databaseProductName) {
        if (databaseProductName == null) {
            return "generic";
        }
        
        String productName = databaseProductName.toLowerCase();
        if (productName.contains("mysql")) {
            return "mysql";
        } else if (productName.contains("postgresql")) {
            return "postgresql";
        } else if (productName.contains("oracle")) {
            return "oracle";
        } else if (productName.contains("sql server")) {
            return "sqlserver";
        } else if (productName.contains("h2")) {
            return "h2";
        }
        
        return "generic";
    }

    /**
     * Map database-specific data types to standard SQL types.
     */
    private String mapDataType(String dataType, Integer columnSize, String databaseType) {
        if (dataType == null) {
            return "VARCHAR(255)";
        }
        
        String mappedType = dataType.toUpperCase();
        
        // Add size if applicable and not already included
        if (columnSize != null && columnSize > 0 && 
            (mappedType.equals("VARCHAR") || mappedType.equals("CHAR") || 
             mappedType.equals("NVARCHAR") || mappedType.equals("NCHAR"))) {
            
            if (!mappedType.contains("(")) {
                mappedType += "(" + columnSize + ")";
            }
        }
        
        // Database-specific type mappings
        switch (databaseType) {
            case "mysql":
                return mapMySqlDataType(mappedType);
            case "postgresql":
                return mapPostgreSqlDataType(mappedType);
            default:
                return mappedType;
        }
    }

    /**
     * Map MySQL-specific data types.
     */
    private String mapMySqlDataType(String dataType) {
        // Common MySQL type mappings
        switch (dataType.toUpperCase()) {
            case "BIT":
                return "BOOLEAN";
            case "TINYINT UNSIGNED":
                return "TINYINT UNSIGNED";
            case "LONGTEXT":
                return "LONGTEXT";
            case "MEDIUMTEXT":
                return "MEDIUMTEXT";
            default:
                return dataType;
        }
    }

    /**
     * Map PostgreSQL-specific data types.
     */
    private String mapPostgreSqlDataType(String dataType) {
        // Common PostgreSQL type mappings
        switch (dataType.toUpperCase()) {
            case "SERIAL":
                return "SERIAL";
            case "BIGSERIAL":
                return "BIGSERIAL";
            case "TEXT":
                return "TEXT";
            case "BOOLEAN":
                return "BOOLEAN";
            default:
                return dataType;
        }
    }

    /**
     * Escape identifiers (table names, column names) based on database type.
     */
    private String escapeIdentifier(String identifier, String databaseType) {
        if (identifier == null || identifier.isEmpty()) {
            return identifier;
        }
        
        // Check if identifier needs escaping (contains spaces, special chars, or is a reserved word)
        boolean needsEscaping = identifier.contains(" ") || identifier.contains("-") || 
                              isReservedWord(identifier, databaseType);
        
        if (!needsEscaping) {
            return identifier;
        }
        
        switch (databaseType) {
            case "mysql":
                return "`" + identifier + "`";
            case "postgresql":
            case "oracle":
                return "\"" + identifier + "\"";
            case "sqlserver":
                return "[" + identifier + "]";
            default:
                return "\"" + identifier + "\"";
        }
    }

    /**
     * Check if identifier is a reserved word.
     */
    private boolean isReservedWord(String identifier, String databaseType) {
        // Basic list of common reserved words - could be expanded
        String[] commonReservedWords = {
            "SELECT", "INSERT", "UPDATE", "DELETE", "CREATE", "DROP", "ALTER", "TABLE", 
            "INDEX", "VIEW", "DATABASE", "SCHEMA", "USER", "GROUP", "ORDER", "BY", 
            "WHERE", "AND", "OR", "NOT", "NULL", "PRIMARY", "KEY", "FOREIGN", "REFERENCES"
        };
        
        String upperIdentifier = identifier.toUpperCase();
        for (String reserved : commonReservedWords) {
            if (reserved.equals(upperIdentifier)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Format default value based on data type.
     */
    private String formatDefaultValue(String defaultValue, String dataType) {
        if (defaultValue == null || defaultValue.isEmpty()) {
            return "NULL";
        }
        
        String upperDataType = dataType.toUpperCase();
        
        // Check if it's a string type that needs quotes
        if (upperDataType.contains("VARCHAR") || upperDataType.contains("CHAR") || 
            upperDataType.contains("TEXT") || upperDataType.contains("STRING")) {
            
            // Don't add quotes if already quoted or if it's a function call
            if (defaultValue.startsWith("'") && defaultValue.endsWith("'")) {
                return defaultValue;
            }
            if (defaultValue.toUpperCase().contains("CURRENT_TIMESTAMP") || 
                defaultValue.toUpperCase().contains("NOW()")) {
                return defaultValue;
            }
            
            return "'" + escapeString(defaultValue) + "'";
        }
        
        return defaultValue;
    }

    /**
     * Escape special characters in strings.
     */
    private String escapeString(String value) {
        if (value == null) {
            return "";
        }
        
        return value.replace("'", "''").replace("\\", "\\\\");
    }
}
