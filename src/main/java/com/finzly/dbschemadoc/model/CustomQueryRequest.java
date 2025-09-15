package com.finzly.dbschemadoc.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Request object for custom SQL query execution.
 * Encapsulates the SQL query and related parameters for CSV export.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request object for custom SQL query execution")
public class CustomQueryRequest {
    
    @Schema(
        description = "SQL SELECT query to execute. Only SELECT statements are allowed for security. " +
                     "Can include JOINs, WHERE clauses, GROUP BY, ORDER BY, etc.",
        example = "SELECT * FROM your_table_name WHERE condition = 'value' LIMIT 100",
        required = true
    )
    private String sqlQuery;
    
    @Schema(
        description = "Name for the export (used in filename generation). " +
                     "Will be sanitized to remove special characters.",
        example = "user_data_export",
        required = true
    )
    private String exportName;
    
    @Schema(
        description = "Optional description for the export operation",
        example = "Monthly user data export for reporting",
        required = false
    )
    private String description;
}
