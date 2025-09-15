package com.finzly.dbschemadoc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Service for exporting table data to CSV format with support for large datasets.
 * Implements streaming and file splitting to handle memory constraints.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CsvDataExportService {

    private final DataSource dataSource;
    
    @Value("${app.reports.output.directory:./reports}")
    private String reportsOutputDirectory;
    
    @Value("${app.database.default.schema:galaxy_compliance_mcbankny}")
    private String defaultSchema;
    
    @Value("${app.export.csv.batch.size:10000}")
    private int batchSize;
    
    @Value("${app.export.csv.max.records.per.file:100000}")
    private int maxRecordsPerFile;
    
    @Value("${app.export.csv.connection.timeout.seconds:300}")
    private int connectionTimeoutSeconds;
    
    // SQL validation patterns - More flexible to handle newlines, tabs, and multiple spaces
    private static final Pattern SELECT_PATTERN = Pattern.compile("^\\s*SELECT\\s", Pattern.CASE_INSENSITIVE);
    private static final Pattern DANGEROUS_KEYWORDS = Pattern.compile(
        "\\b(INSERT|UPDATE|DELETE|DROP|CREATE|ALTER|EXEC|EXECUTE|CALL|DECLARE|TRUNCATE|MERGE|REPLACE)\\b", 
        Pattern.CASE_INSENSITIVE
    );
    
    /**
     * Export table data to CSV files with date range filtering.
     * 
     * @param tableName Name of the table to export
     * @param startDay Start day of month (1-31)
     * @param endDay End day of month (1-31)
     * @param month Month (1-12), null for current month
     * @param year Year, null for current year
     * @param dateColumn Name of the date column to filter on (optional)
     * @return List of generated CSV file paths
     */
    public List<String> exportTableData(String tableName, Integer startDay, Integer endDay, 
                                      Integer month, Integer year, String dateColumn) {
        
        log.info("Starting CSV export for table: {} with date range: {}-{}, month: {}, year: {}", 
                tableName, startDay, endDay, month, year);
        
        List<String> generatedFiles = new ArrayList<>();
        
        try {
            // Validate and prepare parameters
            validateParameters(tableName, startDay, endDay, month, year);
            
            // Create output directory if it doesn't exist
            Path outputDir = Paths.get(reportsOutputDirectory);
            Files.createDirectories(outputDir);
            
            // Build SQL query with date filtering
            String query = buildDateFilterQuery(tableName, startDay, endDay, month, year, dateColumn);
            log.info("Executing query: {}", query);
            
            // Execute export with streaming
            generatedFiles = executeStreamingExport(tableName, query, startDay, endDay, month, year);
            
            log.info("CSV export completed successfully. Generated {} files", generatedFiles.size());
            return generatedFiles;
            
        } catch (Exception e) {
            log.error("Error during CSV export: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to export table data to CSV", e);
        }
    }
    
    /**
     * Export table data with weekly filter.
     * 
     * @param tableName Name of the table to export
     * @param weekNumber Week number (1-52)
     * @param year Year, null for current year
     * @param dateColumn Name of the date column to filter on (optional)
     * @return List of generated CSV file paths
     */
    public List<String> exportTableDataWeekly(String tableName, Integer weekNumber, 
                                            Integer year, String dateColumn) {
        
        log.info("Starting weekly CSV export for table: {} for week: {}, year: {}", 
                tableName, weekNumber, year);
        
        try {
            // Calculate date range for the specified week
            int targetYear = year != null ? year : LocalDate.now().getYear();
            LocalDate startOfWeek = LocalDate.ofYearDay(targetYear, 1)
                    .plusWeeks(weekNumber - 1);
            LocalDate endOfWeek = startOfWeek.plusDays(6);
            
            // Build weekly filter query
            String query = buildWeeklyFilterQuery(tableName, weekNumber, targetYear, dateColumn);
            log.info("Executing weekly query: {}", query);
            
            // Execute export with streaming
            List<String> generatedFiles = executeStreamingExport(tableName, query, 
                    startOfWeek.getDayOfMonth(), endOfWeek.getDayOfMonth(), 
                    startOfWeek.getMonthValue(), targetYear);
            
            log.info("Weekly CSV export completed successfully. Generated {} files", generatedFiles.size());
            return generatedFiles;
            
        } catch (Exception e) {
            log.error("Error during weekly CSV export: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to export weekly table data to CSV", e);
        }
    }
    
    /**
     * Export table data with monthly filter.
     * 
     * @param tableName Name of the table to export
     * @param month Month (1-12)
     * @param year Year, null for current year
     * @param dateColumn Name of the date column to filter on (optional)
     * @return List of generated CSV file paths
     */
    public List<String> exportTableDataMonthly(String tableName, Integer month, 
                                             Integer year, String dateColumn) {
        
        log.info("Starting monthly CSV export for table: {} for month: {}, year: {}", 
                tableName, month, year);
        
        try {
            int targetYear = year != null ? year : LocalDate.now().getYear();
            int targetMonth = month != null ? month : LocalDate.now().getMonthValue();
            
            // Get first and last day of the month
            LocalDate firstDayOfMonth = LocalDate.of(targetYear, targetMonth, 1);
            LocalDate lastDayOfMonth = firstDayOfMonth.withDayOfMonth(firstDayOfMonth.lengthOfMonth());
            
            // Build monthly filter query
            String query = buildMonthlyFilterQuery(tableName, targetMonth, targetYear, dateColumn);
            log.info("Executing monthly query: {}", query);
            
            // Execute export with streaming
            List<String> generatedFiles = executeStreamingExport(tableName, query, 
                    1, lastDayOfMonth.getDayOfMonth(), targetMonth, targetYear);
            
            log.info("Monthly CSV export completed successfully. Generated {} files", generatedFiles.size());
            return generatedFiles;
            
        } catch (Exception e) {
            log.error("Error during monthly CSV export: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to export monthly table data to CSV", e);
        }
    }
    
    /**
     * Execute streaming export with file splitting support.
     */
    private List<String> executeStreamingExport(String tableName, String query, 
                                              Integer startDay, Integer endDay, 
                                              Integer month, Integer year) throws SQLException, IOException {
        
        List<String> generatedFiles = new ArrayList<>();
        
        try (Connection connection = dataSource.getConnection()) {
            // Configure connection for streaming
            connection.setReadOnly(true);
            connection.setAutoCommit(false);
            
            // Set query timeout
            try (PreparedStatement statement = connection.prepareStatement(query, 
                    ResultSet.TYPE_FORWARD_ONLY, 
                    ResultSet.CONCUR_READ_ONLY)) {
                
                statement.setQueryTimeout(connectionTimeoutSeconds);
                statement.setFetchSize(batchSize);
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    ResultSetMetaData metaData = resultSet.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    
                    // Generate base filename
                    String baseFilename = generateBaseFilename(tableName, startDay, endDay, month, year);
                    
                    // Process results with file splitting
                    generatedFiles = processResultSetWithSplitting(resultSet, metaData, 
                            columnCount, baseFilename);
                }
            }
        }
        
        return generatedFiles;
    }
    
    /**
     * Process ResultSet with automatic file splitting for large datasets.
     */
    private List<String> processResultSetWithSplitting(ResultSet resultSet, 
                                                       ResultSetMetaData metaData, 
                                                       int columnCount, 
                                                       String baseFilename) throws SQLException, IOException {
        
        List<String> generatedFiles = new ArrayList<>();
        int recordCount = 0;
        int fileNumber = 1;
        
        BufferedWriter writer = null;
        String currentFilename = null;
        
        try {
            while (resultSet.next()) {
                // Start new file if needed
                if (recordCount % maxRecordsPerFile == 0) {
                    // Close previous file
                    if (writer != null) {
                        writer.close();
                        log.info("Completed CSV file: {} with {} records", currentFilename, 
                                recordCount % maxRecordsPerFile == 0 ? 
                                maxRecordsPerFile : recordCount % maxRecordsPerFile);
                    }
                    
                    // Create new file
                    currentFilename = generateSplitFilename(baseFilename, fileNumber);
                    Path filePath = Paths.get(reportsOutputDirectory, currentFilename);
                    writer = Files.newBufferedWriter(filePath);
                    generatedFiles.add(filePath.toString());
                    
                    // Write CSV header
                    writeCSVHeader(writer, metaData, columnCount);
                    fileNumber++;
                }
                
                // Write data row
                writeCSVRow(writer, resultSet, columnCount);
                recordCount++;
                
                // Log progress for large datasets
                if (recordCount % (batchSize * 10) == 0) {
                    log.info("Processed {} records, current file: {}", recordCount, currentFilename);
                }
            }
            
        } finally {
            if (writer != null) {
                writer.close();
                log.info("Completed final CSV file: {} with {} total records processed", 
                        currentFilename, recordCount);
            }
        }
        
        log.info("Export completed. Total records: {}, Files generated: {}", recordCount, generatedFiles.size());
        return generatedFiles;
    }
    
    /**
     * Write CSV header row.
     */
    private void writeCSVHeader(BufferedWriter writer, ResultSetMetaData metaData, 
                               int columnCount) throws SQLException, IOException {
        
        StringBuilder header = new StringBuilder();
        for (int i = 1; i <= columnCount; i++) {
            if (i > 1) {
                header.append(",");
            }
            header.append(escapeCSVValue(metaData.getColumnName(i)));
        }
        writer.write(header.toString());
        writer.newLine();
    }
    
    /**
     * Write CSV data row.
     */
    private void writeCSVRow(BufferedWriter writer, ResultSet resultSet, 
                            int columnCount) throws SQLException, IOException {
        
        StringBuilder row = new StringBuilder();
        for (int i = 1; i <= columnCount; i++) {
            if (i > 1) {
                row.append(",");
            }
            
            Object value = resultSet.getObject(i);
            String stringValue = value != null ? value.toString() : "";
            row.append(escapeCSVValue(stringValue));
        }
        writer.write(row.toString());
        writer.newLine();
    }
    
    /**
     * Escape CSV values properly.
     */
    private String escapeCSVValue(String value) {
        if (value == null) {
            return "";
        }
        
        // If value contains comma, quote, or newline, wrap in quotes and escape quotes
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        
        return value;
    }
    
    /**
     * Build SQL query with date range filtering.
     */
    private String buildDateFilterQuery(String tableName, Integer startDay, Integer endDay, 
                                       Integer month, Integer year, String dateColumn) {
        
        StringBuilder query = new StringBuilder();
        query.append("SELECT * FROM ");
        
        // Add schema prefix if needed
        if (!tableName.contains(".")) {
            query.append(defaultSchema).append(".");
        }
        query.append(tableName);
        
        // Add date filtering if date column is specified
        if (dateColumn != null && !dateColumn.trim().isEmpty()) {
            query.append(" WHERE ");
            
            // Day of month filter
            if (startDay != null && endDay != null) {
                query.append("DAYOFMONTH(").append(dateColumn).append(") BETWEEN ")
                      .append(startDay).append(" AND ").append(endDay);
            }
            
            // Month filter
            if (month != null) {
                if (startDay != null && endDay != null) {
                    query.append(" AND ");
                }
                query.append("MONTH(").append(dateColumn).append(") = ").append(month);
            }
            
            // Year filter
            if (year != null) {
                if ((startDay != null && endDay != null) || month != null) {
                    query.append(" AND ");
                }
                query.append("YEAR(").append(dateColumn).append(") = ").append(year);
            }
        }
        
        query.append(" ORDER BY ");
        query.append(dateColumn != null ? dateColumn : "1"); // Order by date column or first column
        
        return query.toString();
    }
    
    /**
     * Build weekly filter query.
     */
    private String buildWeeklyFilterQuery(String tableName, Integer weekNumber, Integer year, String dateColumn) {
        StringBuilder query = new StringBuilder();
        query.append("SELECT * FROM ");
        
        if (!tableName.contains(".")) {
            query.append(defaultSchema).append(".");
        }
        query.append(tableName);
        
        if (dateColumn != null && !dateColumn.trim().isEmpty()) {
            query.append(" WHERE WEEK(").append(dateColumn).append(") = ").append(weekNumber);
            if (year != null) {
                query.append(" AND YEAR(").append(dateColumn).append(") = ").append(year);
            }
        }
        
        query.append(" ORDER BY ");
        query.append(dateColumn != null ? dateColumn : "1");
        
        return query.toString();
    }
    
    /**
     * Build monthly filter query.
     */
    private String buildMonthlyFilterQuery(String tableName, Integer month, Integer year, String dateColumn) {
        StringBuilder query = new StringBuilder();
        query.append("SELECT * FROM ");
        
        if (!tableName.contains(".")) {
            query.append(defaultSchema).append(".");
        }
        query.append(tableName);
        
        if (dateColumn != null && !dateColumn.trim().isEmpty()) {
            query.append(" WHERE MONTH(").append(dateColumn).append(") = ").append(month);
            if (year != null) {
                query.append(" AND YEAR(").append(dateColumn).append(") = ").append(year);
            }
        }
        
        query.append(" ORDER BY ");
        query.append(dateColumn != null ? dateColumn : "1");
        
        return query.toString();
    }
    
    /**
     * Generate base filename for CSV files.
     */
    private String generateBaseFilename(String tableName, Integer startDay, Integer endDay, 
                                       Integer month, Integer year) {
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String timestamp = LocalDateTime.now().format(formatter);
        
        StringBuilder filename = new StringBuilder();
        filename.append("export_").append(tableName);
        
        if (startDay != null && endDay != null) {
            filename.append("_days_").append(startDay).append("-").append(endDay);
        }
        if (month != null) {
            filename.append("_month_").append(month);
        }
        if (year != null) {
            filename.append("_year_").append(year);
        }
        
        filename.append("_").append(timestamp);
        
        return filename.toString();
    }
    
    /**
     * Generate filename for split files.
     */
    private String generateSplitFilename(String baseFilename, int fileNumber) {
        if (fileNumber == 1) {
            return baseFilename + ".csv";
        } else {
            return baseFilename + "_part" + String.format("%03d", fileNumber) + ".csv";
        }
    }
    
    /**
     * Validate input parameters.
     */
    private void validateParameters(String tableName, Integer startDay, Integer endDay, 
                                   Integer month, Integer year) {
        
        if (tableName == null || tableName.trim().isEmpty()) {
            throw new IllegalArgumentException("Table name cannot be null or empty");
        }
        
        if (startDay != null && (startDay < 1 || startDay > 31)) {
            throw new IllegalArgumentException("Start day must be between 1 and 31");
        }
        
        if (endDay != null && (endDay < 1 || endDay > 31)) {
            throw new IllegalArgumentException("End day must be between 1 and 31");
        }
        
        if (startDay != null && endDay != null && startDay > endDay) {
            throw new IllegalArgumentException("Start day cannot be greater than end day");
        }
        
        if (month != null && (month < 1 || month > 12)) {
            throw new IllegalArgumentException("Month must be between 1 and 12");
        }
        
        if (year != null && (year < 1900 || year > 2100)) {
            throw new IllegalArgumentException("Year must be between 1900 and 2100");
        }
    }
    
    /**
     * Export data using a custom SQL query to CSV files.
     * Supports complex queries with joins across multiple tables.
     * 
     * @param sqlQuery Custom SELECT query to execute
     * @param exportName Name for the export (used in filename)
     * @return List of generated CSV file paths
     */
    public List<String> exportCustomQuery(String sqlQuery, String exportName) {
        log.info("Starting custom SQL query export with name: {}", exportName);
        log.debug("Executing query: {}", sqlQuery);
        
        List<String> generatedFiles = new ArrayList<>();
        
        try {
            // Validate the SQL query
            validateSqlQuery(sqlQuery);
            
            // Create output directory if it doesn't exist
            Path outputDir = Paths.get(reportsOutputDirectory);
            Files.createDirectories(outputDir);
            
            // Execute export with streaming
            generatedFiles = executeCustomQueryExport(sqlQuery, exportName);
            
            log.info("Custom query export completed successfully. Generated {} files", generatedFiles.size());
            return generatedFiles;
            
        } catch (Exception e) {
            log.error("Error during custom query export: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to export custom query data to CSV", e);
        }
    }
    
    /**
     * Execute custom query export with streaming and file splitting.
     */
    private List<String> executeCustomQueryExport(String sqlQuery, String exportName) throws SQLException, IOException {
        List<String> generatedFiles = new ArrayList<>();
        
        try (Connection connection = dataSource.getConnection()) {
            // Configure connection for streaming
            connection.setReadOnly(true);
            connection.setAutoCommit(false);
            
            // Set query timeout for complex joins
            int extendedTimeout = connectionTimeoutSeconds * 2; // Double timeout for complex queries
            
            try (PreparedStatement statement = connection.prepareStatement(sqlQuery, 
                    ResultSet.TYPE_FORWARD_ONLY, 
                    ResultSet.CONCUR_READ_ONLY)) {
                
                statement.setQueryTimeout(extendedTimeout);
                statement.setFetchSize(batchSize);
                
                log.info("Executing custom query with timeout: {} seconds", extendedTimeout);
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    ResultSetMetaData metaData = resultSet.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    
                    // Generate base filename for custom query
                    String baseFilename = generateCustomQueryFilename(exportName);
                    
                    // Process results with file splitting
                    generatedFiles = processResultSetWithSplitting(resultSet, metaData, 
                            columnCount, baseFilename);
                }
            }
        }
        
        return generatedFiles;
    }
    
    /**
     * Validate SQL query for security and correctness.
     */
    private void validateSqlQuery(String sqlQuery) {
        log.info("Validating SQL query - Length: {}, Content: [{}]", 
                sqlQuery != null ? sqlQuery.length() : 0, sqlQuery != null ? sqlQuery : "NULL");
        if (sqlQuery == null || sqlQuery.trim().isEmpty()) {
            log.error("SQL query validation failed: query is null or empty");
            throw new IllegalArgumentException("SQL query cannot be null or empty");
        }
        
        String trimmedQuery = sqlQuery.trim();
        log.info("Trimmed query for validation: [{}]", trimmedQuery);
        log.info("First 50 characters: [{}]", trimmedQuery.length() > 50 ? trimmedQuery.substring(0, 50) : trimmedQuery);
        
        // Check if query starts with SELECT
        boolean selectMatches = SELECT_PATTERN.matcher(trimmedQuery).find();
        log.info("SELECT pattern match result: {} for query starting with: [{}]", 
                selectMatches, trimmedQuery.length() > 20 ? trimmedQuery.substring(0, 20) : trimmedQuery);
        
        if (!selectMatches) {
            log.error("Query failed SELECT validation. Pattern: {}, Query start: [{}]", 
                    SELECT_PATTERN.pattern(), trimmedQuery.length() > 100 ? trimmedQuery.substring(0, 100) : trimmedQuery);
            throw new IllegalArgumentException("Only SELECT queries are allowed");
        }
        
        // Check for dangerous SQL keywords
        if (DANGEROUS_KEYWORDS.matcher(trimmedQuery).find()) {
            throw new IllegalArgumentException("Query contains prohibited SQL keywords. Only SELECT queries are allowed.");
        }
        
        // Basic length validation
        if (trimmedQuery.length() > 10000) {
            throw new IllegalArgumentException("Query length exceeds maximum allowed limit of 10,000 characters");
        }
        
        // Check for multiple statements (basic semicolon check)
        String[] statements = trimmedQuery.split(";");
        if (statements.length > 1) {
            // Allow trailing semicolon
            boolean hasMultipleStatements = false;
            for (int i = 1; i < statements.length; i++) {
                if (!statements[i].trim().isEmpty()) {
                    hasMultipleStatements = true;
                    break;
                }
            }
            if (hasMultipleStatements) {
                throw new IllegalArgumentException("Multiple SQL statements are not allowed");
            }
        }
        
        log.debug("SQL query validation passed for query length: {} characters", trimmedQuery.length());
    }
    
    /**
     * Public method for validating SQL queries (used by controller).
     */
    public void validateSqlQuerySafely(String sqlQuery) {
        validateSqlQuery(sqlQuery);
    }
    
    /**
     * Generate filename for custom query exports.
     */
    private String generateCustomQueryFilename(String exportName) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String timestamp = LocalDateTime.now().format(formatter);
        
        // Sanitize export name for filename
        String sanitizedName = exportName.replaceAll("[^a-zA-Z0-9_-]", "_");
        if (sanitizedName.length() > 50) {
            sanitizedName = sanitizedName.substring(0, 50);
        }
        
        return "export_custom_" + sanitizedName + "_" + timestamp;
    }
    
    /**
     * Export data using a custom SQL query with additional metadata in response.
     * 
     * @param sqlQuery Custom SELECT query to execute
     * @param exportName Name for the export (used in filename)
     * @return CustomQueryExportResult with files and metadata
     */
    public CustomQueryExportResult exportCustomQueryWithMetadata(String sqlQuery, String exportName) {
        log.info("Starting custom SQL query export with metadata for: {}", exportName);
        log.info("SQL Query received in service - Length: {}, Content: [{}]", 
                sqlQuery != null ? sqlQuery.length() : 0, sqlQuery != null ? sqlQuery : "NULL");
        
        long startTime = System.currentTimeMillis();
        List<String> generatedFiles = new ArrayList<>();
        long totalRecords = 0;
        String[] columnNames = null;
        
        try {
            // Validate the SQL query
            validateSqlQuery(sqlQuery);
            
            // Create output directory if it doesn't exist
            Path outputDir = Paths.get(reportsOutputDirectory);
            Files.createDirectories(outputDir);
            
            try (Connection connection = dataSource.getConnection()) {
                connection.setReadOnly(true);
                connection.setAutoCommit(false);
                
                int extendedTimeout = connectionTimeoutSeconds * 2;
                
                log.info("About to prepare statement with SQL: [{}]", sqlQuery != null ? sqlQuery : "NULL");
                try (PreparedStatement statement = connection.prepareStatement(sqlQuery, 
                        ResultSet.TYPE_FORWARD_ONLY, 
                        ResultSet.CONCUR_READ_ONLY)) {
                    
                    statement.setQueryTimeout(extendedTimeout);
                    statement.setFetchSize(batchSize);
                    
                    try (ResultSet resultSet = statement.executeQuery()) {
                        ResultSetMetaData metaData = resultSet.getMetaData();
                        int columnCount = metaData.getColumnCount();
                        
                        // Extract column names
                        columnNames = new String[columnCount];
                        for (int i = 1; i <= columnCount; i++) {
                            columnNames[i - 1] = metaData.getColumnName(i);
                        }
                        
                        // Generate base filename
                        String baseFilename = generateCustomQueryFilename(exportName);
                        
                        // Process results and count records
                        generatedFiles = processResultSetWithSplitting(resultSet, metaData, 
                                columnCount, baseFilename);
                        
                        // Count total records (this is approximate based on files generated)
                        totalRecords = (long) (generatedFiles.size() - 1) * maxRecordsPerFile;
                        if (generatedFiles.size() == 1) {
                            // For single file, we'd need to count differently
                            // For now, we'll leave it as estimated
                        }
                    }
                }
            }
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            log.info("Custom query export completed in {} ms. Generated {} files with estimated {} records", 
                    executionTime, generatedFiles.size(), totalRecords);
            
            return CustomQueryExportResult.builder()
                    .success(true)
                    .filePaths(generatedFiles)
                    .filesGenerated(generatedFiles.size())
                    .estimatedRecords(totalRecords)
                    .columnNames(columnNames)
                    .executionTimeMs(executionTime)
                    .exportName(exportName)
                    .queryLength(sqlQuery != null ? sqlQuery.length() : 0)
                    .build();
            
        } catch (Exception e) {
            log.error("Error during custom query export: {}", e.getMessage(), e);
            
            return CustomQueryExportResult.builder()
                    .success(false)
                    .error(e.getMessage())
                    .executionTimeMs(System.currentTimeMillis() - startTime)
                    .exportName(exportName)
                    .queryLength(sqlQuery != null ? sqlQuery.length() : 0)
                    .build();
        }
    }
    
    /**
     * Result object for custom query exports with metadata.
     */
    @lombok.Data
    @lombok.Builder
    public static class CustomQueryExportResult {
        private boolean success;
        private String error;
        private List<String> filePaths;
        private Integer filesGenerated;
        private Long estimatedRecords;
        private String[] columnNames;
        private Long executionTimeMs;
        private String exportName;
        private Integer queryLength;
    }
}
