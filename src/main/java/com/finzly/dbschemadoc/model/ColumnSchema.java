package com.finzly.dbschemadoc.model;

import lombok.Builder;
import lombok.Data;

/**
 * Represents database column schema information.
 */
@Data
@Builder
public class ColumnSchema {
    private String columnName;
    private String dataType;
    private Integer columnSize;
    private boolean nullable;
    private String defaultValue;
    private String remarks;
    private int ordinalPosition;
    private boolean isPrimaryKey;
    private boolean isForeignKey;
    private String referencedTable;
    private String referencedColumn;
}
