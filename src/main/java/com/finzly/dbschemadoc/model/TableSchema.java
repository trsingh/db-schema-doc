package com.finzly.dbschemadoc.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Represents database table schema information.
 */
@Data
@Builder
public class TableSchema {
    private String tableName;
    private String tableType;
    private String remarks;
    private String schemaName;
    private List<ColumnSchema> columns;
    private List<String> primaryKeys;
    private List<ForeignKey> foreignKeys;
    
    @Data
    @Builder
    public static class ForeignKey {
        private String columnName;
        private String referencedTable;
        private String referencedColumn;
        private String constraintName;
    }
}
