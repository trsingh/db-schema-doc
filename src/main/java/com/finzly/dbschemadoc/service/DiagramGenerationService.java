package com.finzly.dbschemadoc.service;

import com.finzly.dbschemadoc.model.ColumnSchema;
import com.finzly.dbschemadoc.model.DatabaseSchema;
import com.finzly.dbschemadoc.model.TableSchema;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for generating database schema diagrams using PlantUML.
 * Creates visual representations of database tables, columns, and relationships.
 */
@Service
@Slf4j
public class DiagramGenerationService {

    /**
     * Generate database schema diagram as PNG image bytes.
     */
    public byte[] generateDiagramPNG(DatabaseSchema schema) throws IOException {
        log.info("Generating database diagram for schema: {}", schema.getDatabaseName());
        
        String plantUmlSource = generatePlantUmlSource(schema);
        log.debug("Generated PlantUML source:\n{}", plantUmlSource);
        
        SourceStringReader reader = new SourceStringReader(plantUmlSource);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        reader.outputImage(outputStream, new FileFormatOption(FileFormat.PNG));
        
        byte[] imageBytes = outputStream.toByteArray();
        log.info("Generated diagram PNG image: {} bytes", imageBytes.length);
        
        return imageBytes;
    }

    /**
     * Generate database schema diagram as Base64 encoded PNG for HTML embedding.
     */
    public String generateDiagramBase64(DatabaseSchema schema) throws IOException {
        byte[] imageBytes = generateDiagramPNG(schema);
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        log.info("Generated Base64 diagram image: {} characters", base64Image.length());
        return base64Image;
    }

    /**
     * Generate PlantUML source code for the database schema.
     */
    private String generatePlantUmlSource(DatabaseSchema schema) {
        StringBuilder plantuml = new StringBuilder();
        
        // Header
        plantuml.append("@startuml\n");
        plantuml.append("!theme vibrant\n");
        plantuml.append("skinparam backgroundColor #FAFAFA\n");
        plantuml.append("skinparam entity {\n");
        plantuml.append("  BackgroundColor #E3F2FD\n");
        plantuml.append("  BorderColor #1976D2\n");
        plantuml.append("  FontSize 10\n");
        plantuml.append("}\n");
        plantuml.append("skinparam title {\n");
        plantuml.append("  FontSize 16\n");
        plantuml.append("  FontColor #1976D2\n");
        plantuml.append("}\n\n");
        
        // Title
        plantuml.append("title Database Schema: ").append(schema.getDatabaseName()).append("\\n");
        plantuml.append("Database: ").append(schema.getDatabaseProductName())
                .append(" ").append(schema.getDatabaseProductVersion()).append("\\n");
        plantuml.append("Generated: ").append(java.time.LocalDateTime.now().toString().substring(0, 19)).append("\n\n");

        // Generate entities for each table
        List<TableSchema> tables = schema.getTables();
        if (tables == null || tables.isEmpty()) {
            plantuml.append("note as N1\n");
            plantuml.append("  No tables found in schema\n");
            plantuml.append("end note\n");
        } else {
            log.info("Generating diagram for {} tables", tables.size());
            
            // Limit tables for readability (show max 20 tables)
            List<TableSchema> limitedTables = tables.stream()
                    .limit(20)
                    .collect(Collectors.toList());
            
            if (tables.size() > 20) {
                log.info("Limiting diagram to first 20 tables out of {} total", tables.size());
            }
            
            for (TableSchema table : limitedTables) {
                generateEntityDefinition(plantuml, table);
            }

            // Generate relationships (foreign keys)
            generateRelationships(plantuml, limitedTables);
            
            if (tables.size() > 20) {
                plantuml.append("\nnote as MoreTables\n");
                plantuml.append("  ... and ").append(tables.size() - 20).append(" more tables\n");
                plantuml.append("  (showing first 20 for clarity)\n");
                plantuml.append("end note\n");
            }
        }

        plantuml.append("\n@enduml\n");
        
        return plantuml.toString();
    }

    /**
     * Generate entity definition for a table.
     */
    private void generateEntityDefinition(StringBuilder plantuml, TableSchema table) {
        String tableName = sanitizeEntityName(table.getTableName());
        
        plantuml.append("entity \"").append(table.getTableName()).append("\" as ").append(tableName).append(" {\n");
        
        if (table.getColumns() != null && !table.getColumns().isEmpty()) {
            // Primary key columns first
            List<ColumnSchema> primaryColumns = table.getColumns().stream()
                    .filter(ColumnSchema::isPrimaryKey)
                    .collect(Collectors.toList());
            
            List<ColumnSchema> regularColumns = table.getColumns().stream()
                    .filter(col -> !col.isPrimaryKey())
                    .collect(Collectors.toList());
            
            // Add primary key columns
            for (ColumnSchema column : primaryColumns) {
                plantuml.append("  * ").append(column.getColumnName())
                        .append(" : ").append(column.getDataType());
                if (column.getColumnSize() > 0 && !isTextType(column.getDataType())) {
                    plantuml.append("(").append(column.getColumnSize()).append(")");
                }
                plantuml.append(" <<PK>>\n");
            }
            
            // Add separator if we have both PK and regular columns
            if (!primaryColumns.isEmpty() && !regularColumns.isEmpty()) {
                plantuml.append("  --\n");
            }
            
            // Add regular columns (limit to first 10 for readability)
            List<ColumnSchema> displayColumns = regularColumns.stream()
                    .limit(10)
                    .collect(Collectors.toList());
            
            for (ColumnSchema column : displayColumns) {
                plantuml.append("  ");
                
                if (column.isForeignKey()) {
                    plantuml.append("+ ");
                }
                
                plantuml.append(column.getColumnName())
                        .append(" : ").append(column.getDataType());
                
                if (column.getColumnSize() > 0 && !isTextType(column.getDataType())) {
                    plantuml.append("(").append(column.getColumnSize()).append(")");
                }
                
                if (!column.isNullable()) {
                    plantuml.append(" NOT NULL");
                }
                
                if (column.isForeignKey()) {
                    plantuml.append(" <<FK>>");
                }
                
                plantuml.append("\n");
            }
            
            // Show indication if there are more columns
            if (regularColumns.size() > 10) {
                plantuml.append("  ... and ").append(regularColumns.size() - 10).append(" more columns\n");
            }
        }
        
        plantuml.append("}\n\n");
    }

    /**
     * Generate relationships between entities based on foreign keys.
     */
    private void generateRelationships(StringBuilder plantuml, List<TableSchema> tables) {
        Map<String, String> tableNameMap = tables.stream()
                .collect(Collectors.toMap(
                    TableSchema::getTableName, 
                    t -> sanitizeEntityName(t.getTableName())
                ));

        for (TableSchema table : tables) {
            if (table.getForeignKeys() != null) {
                for (TableSchema.ForeignKey fk : table.getForeignKeys()) {
                    String referencedTableEntity = tableNameMap.get(fk.getReferencedTable());
                    String currentTableEntity = tableNameMap.get(table.getTableName());
                    
                    if (referencedTableEntity != null && currentTableEntity != null) {
                        // Generate relationship arrow
                        plantuml.append(referencedTableEntity)
                                .append(" ||--o{ ")
                                .append(currentTableEntity)
                                .append(" : \"")
                                .append(fk.getColumnName())
                                .append("\"\n");
                    }
                }
            }
        }
    }

    /**
     * Sanitize table name for use as PlantUML entity identifier.
     */
    private String sanitizeEntityName(String tableName) {
        return tableName.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    /**
     * Check if data type is a text/blob type that doesn't need size display.
     */
    private boolean isTextType(String dataType) {
        if (dataType == null) return false;
        String type = dataType.toUpperCase();
        return type.contains("TEXT") || type.contains("BLOB") || 
               type.contains("JSON") || type.contains("XML");
    }
}
