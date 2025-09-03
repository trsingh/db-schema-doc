package com.finzly.dbschemadoc.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Represents complete database schema information.
 */
@Data
@Builder
@Schema(description = "Complete database schema information including metadata and table structures")
public class DatabaseSchema {
    
    @Schema(description = "Database name", example = "galaxy_compliance_mcbankny")
    private String databaseName;
    
    @Schema(description = "Database product name", example = "MySQL")
    private String databaseProductName;
    
    @Schema(description = "Database product version", example = "8.0.25")
    private String databaseProductVersion;
    
    @Schema(description = "Database user name", example = "root")
    private String userName;
    
    @Schema(description = "Database connection URL", example = "jdbc:mysql://localhost:3309/galaxy_compliance_mcbankny")
    private String url;
    
    @Schema(description = "List of schema names in the database")
    private List<String> schemas;
    
    @Schema(description = "List of tables and their complete schema information")
    private List<TableSchema> tables;
    
    @Schema(description = "Additional database metadata information")
    private Map<String, String> databaseInfo;
}
