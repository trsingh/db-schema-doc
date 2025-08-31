package com.finzly.dbschemadoc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for extracting database metadata information.
 * All operations are read-only and do not perform any DDL/DML.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DatabaseMetadataService {

    private final DataSource dataSource;

    /**
     * Get basic database information.
     */
    public Map<String, String> getDatabaseInfo() {
        Map<String, String> info = new HashMap<>();
        
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            
            info.put("databaseProductName", metaData.getDatabaseProductName());
            info.put("databaseProductVersion", metaData.getDatabaseProductVersion());
            info.put("driverName", metaData.getDriverName());
            info.put("driverVersion", metaData.getDriverVersion());
            info.put("url", metaData.getURL());
            info.put("userName", metaData.getUserName());
            
            log.info("Retrieved database info: {}", info);
        } catch (SQLException e) {
            log.error("Error retrieving database info: {}", e.getMessage());
            throw new RuntimeException("Failed to retrieve database information", e);
        }
        
        return info;
    }

    /**
     * Get list of all schemas/catalogs in the database.
     */
    public List<String> getSchemas() {
        List<String> schemas = new ArrayList<>();
        
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String productName = metaData.getDatabaseProductName().toLowerCase();
            
            if (productName.contains("mysql")) {
                // For MySQL, use catalogs (databases) instead of schemas
                try (ResultSet rs = metaData.getCatalogs()) {
                    while (rs.next()) {
                        String catalogName = rs.getString("TABLE_CAT");
                        // Skip system databases
                        if (catalogName != null && 
                            !catalogName.toLowerCase().equals("information_schema") &&
                            !catalogName.toLowerCase().equals("performance_schema") &&
                            !catalogName.toLowerCase().equals("mysql") &&
                            !catalogName.toLowerCase().equals("sys")) {
                            schemas.add(catalogName);
                        }
                    }
                }
            } else {
                // For other databases, use standard schemas
                try (ResultSet rs = metaData.getSchemas()) {
                    while (rs.next()) {
                        String schemaName = rs.getString("TABLE_SCHEM");
                        schemas.add(schemaName);
                    }
                }
            }
            
            log.info("Found {} schemas for {} database", schemas.size(), productName);
        } catch (SQLException e) {
            log.error("Error retrieving schemas: {}", e.getMessage());
            throw new RuntimeException("Failed to retrieve schemas", e);
        }
        
        return schemas;
    }

    /**
     * Get list of all tables in a specific schema.
     */
    public List<Map<String, String>> getTables(String schemaName) {
        List<Map<String, String>> tables = new ArrayList<>();
        
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String productName = metaData.getDatabaseProductName().toLowerCase();
            
            ResultSet rs;
            if (productName.contains("mysql")) {
                // For MySQL, use catalog (database name) instead of schema
                rs = metaData.getTables(schemaName, null, "%", new String[]{"TABLE", "VIEW"});
            } else {
                // For other databases, use standard schema
                rs = metaData.getTables(null, schemaName, "%", new String[]{"TABLE", "VIEW"});
            }
            
            try {
                while (rs.next()) {
                    Map<String, String> table = new HashMap<>();
                    table.put("tableName", rs.getString("TABLE_NAME"));
                    table.put("tableType", rs.getString("TABLE_TYPE"));
                    table.put("remarks", rs.getString("REMARKS"));
                    table.put("schemaName", schemaName);
                    tables.add(table);
                }
            } finally {
                rs.close();
            }
            
            log.info("Found {} tables in schema: {} for {} database", tables.size(), schemaName, productName);
        } catch (SQLException e) {
            log.error("Error retrieving tables for schema {}: {}", schemaName, e.getMessage());
        }
        
        return tables;
    }

    /**
     * Get list of all columns for a specific table.
     */
    public List<Map<String, Object>> getTableColumns(String schemaName, String tableName) {
        List<Map<String, Object>> columns = new ArrayList<>();
        
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String productName = metaData.getDatabaseProductName().toLowerCase();
            
            ResultSet rs;
            if (productName.contains("mysql")) {
                // For MySQL, use catalog (database name) instead of schema
                rs = metaData.getColumns(schemaName, null, tableName, "%");
            } else {
                // For other databases, use standard schema
                rs = metaData.getColumns(null, schemaName, tableName, "%");
            }
            
            try {
                while (rs.next()) {
                    Map<String, Object> column = new HashMap<>();
                    column.put("columnName", rs.getString("COLUMN_NAME"));
                    column.put("dataType", rs.getString("TYPE_NAME"));
                    column.put("columnSize", rs.getInt("COLUMN_SIZE"));
                    column.put("nullable", rs.getBoolean("NULLABLE"));
                    column.put("defaultValue", rs.getString("COLUMN_DEF"));
                    column.put("ordinalPosition", rs.getInt("ORDINAL_POSITION"));
                    column.put("remarks", rs.getString("REMARKS"));
                    columns.add(column);
                }
            } finally {
                rs.close();
            }
            
            log.info("Found {} columns for table: {}.{} in {} database", columns.size(), schemaName, tableName, productName);
        } catch (SQLException e) {
            log.error("Error retrieving columns for table {}.{}: {}", schemaName, tableName, e.getMessage());
        }
        
        return columns;
    }

    /**
     * Get primary keys for a specific table.
     */
    public List<String> getPrimaryKeys(String schemaName, String tableName) {
        List<String> primaryKeys = new ArrayList<>();
        
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String productName = metaData.getDatabaseProductName().toLowerCase();
            
            ResultSet rs;
            if (productName.contains("mysql")) {
                rs = metaData.getPrimaryKeys(schemaName, null, tableName);
            } else {
                rs = metaData.getPrimaryKeys(null, schemaName, tableName);
            }
            
            try {
                while (rs.next()) {
                    primaryKeys.add(rs.getString("COLUMN_NAME"));
                }
            } finally {
                rs.close();
            }
            
            log.info("Found {} primary keys for table: {}.{}", primaryKeys.size(), schemaName, tableName);
        } catch (SQLException e) {
            log.error("Error retrieving primary keys for table {}.{}: {}", schemaName, tableName, e.getMessage());
        }
        
        return primaryKeys;
    }
}