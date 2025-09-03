package com.finzly.dbschemadoc.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Represents database table schema information.
 */
@Data
@Builder
@Schema(description = "Database table schema information including columns, keys, and constraints")
public class TableSchema {
    
    @Schema(description = "Table name", example = "users")
    private String tableName;
    
    @Schema(description = "Table type", example = "TABLE")
    private String tableType;
    
    @Schema(description = "Table comments or remarks", example = "User account information")
    private String remarks;
    
    @Schema(description = "Schema name containing this table", example = "galaxy_compliance_mcbankny")
    private String schemaName;
    
    @Schema(description = "List of columns in this table")
    private List<ColumnSchema> columns;
    
    @Schema(description = "List of primary key column names")
    private List<String> primaryKeys;
    
    @Schema(description = "List of foreign key relationships")
    private List<ForeignKey> foreignKeys;
    
    @Schema(description = "List of indexes on this table")
    private List<IndexInfo> indexes;
    
    @Data
    @Builder
    @Schema(description = "Foreign key relationship information")
    public static class ForeignKey {
        
        @Schema(description = "Source column name", example = "department_id")
        private String columnName;
        
        @Schema(description = "Referenced table name", example = "departments")
        private String referencedTable;
        
        @Schema(description = "Referenced column name", example = "id")
        private String referencedColumn;
        
        @Schema(description = "Foreign key constraint name", example = "fk_user_department")
        private String constraintName;
    }
    
    @Data
    @Builder
    @Schema(description = "Database index information")
    public static class IndexInfo {
        
        @Schema(description = "Index name", example = "idx_user_email")
        private String indexName;
        
        @Schema(description = "List of column names in the index", example = "email")
        private List<String> columns;
        
        @Schema(description = "Whether the index is unique", example = "true")
        private boolean unique;
        
        @Schema(description = "Index type or method", example = "BTREE")
        private String indexType;
    }
}
