package com.finzly.dbschemadoc.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * Represents database column schema information.
 */
@Data
@Builder
@Schema(description = "Database column schema information including data type, constraints, and relationships")
public class ColumnSchema {
    
    @Schema(description = "Column name", example = "id")
    private String columnName;
    
    @Schema(description = "Column data type", example = "BIGINT")
    private String dataType;
    
    @Schema(description = "Column size/length", example = "20")
    private Integer columnSize;
    
    @Schema(description = "Whether the column allows NULL values", example = "false")
    private boolean nullable;
    
    @Schema(description = "Default value for the column", example = "0")
    private String defaultValue;
    
    @Schema(description = "Column comments or remarks", example = "Primary identifier")
    private String remarks;
    
    @Schema(description = "Column position in the table", example = "1")
    private int ordinalPosition;
    
    @Schema(description = "Whether this column is part of the primary key", example = "true")
    private boolean isPrimaryKey;
    
    @Schema(description = "Whether this column is a foreign key", example = "false")
    private boolean isForeignKey;
    
    @Schema(description = "Referenced table name if this is a foreign key", example = "departments")
    private String referencedTable;
    
    @Schema(description = "Referenced column name if this is a foreign key", example = "id")
    private String referencedColumn;
}
