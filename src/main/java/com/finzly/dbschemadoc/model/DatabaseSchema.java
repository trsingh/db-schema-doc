package com.finzly.dbschemadoc.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Represents complete database schema information.
 */
@Data
@Builder
public class DatabaseSchema {
    private String databaseName;
    private String databaseProductName;
    private String databaseProductVersion;
    private String userName;
    private String url;
    private List<String> schemas;
    private List<TableSchema> tables;
    private Map<String, String> databaseInfo;
}
