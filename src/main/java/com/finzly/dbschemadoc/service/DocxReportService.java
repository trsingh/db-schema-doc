package com.finzly.dbschemadoc.service;

import com.finzly.dbschemadoc.model.ColumnSchema;
import com.finzly.dbschemadoc.model.DatabaseSchema;
import com.finzly.dbschemadoc.model.TableSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service for generating DOCX reports from database schema.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocxReportService {

    private final DiagramGenerationService diagramGenerationService;

    public byte[] generateDocxReport(DatabaseSchema schema) throws IOException {
        log.info("Generating DOCX report for database: {}", schema.getDatabaseName());
        
        XWPFDocument document = new XWPFDocument();
        
        try {
            // Title page
            createTitlePage(document, schema);
            
            // Database information
            createDatabaseInfoSection(document, schema);
            
            // Statistics
            createStatisticsSection(document, schema);
            
            // Tables section
            createTablesSection(document, schema);
            
            // Convert to byte array
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.write(out);
            return out.toByteArray();
            
        } finally {
            document.close();
        }
    }

    private void createTitlePage(XWPFDocument document, DatabaseSchema schema) {
        // Title
        XWPFParagraph title = document.createParagraph();
        title.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun titleRun = title.createRun();
        titleRun.setText("Database Schema Documentation");
        titleRun.setBold(true);
        titleRun.setFontSize(24);
        titleRun.setFontFamily("Calibri");
        
        // Subtitle
        XWPFParagraph subtitle = document.createParagraph();
        subtitle.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun subtitleRun = subtitle.createRun();
        subtitleRun.setText(schema.getDatabaseProductName() + " Database: " + schema.getDatabaseName());
        subtitleRun.setBold(true);
        subtitleRun.setFontSize(16);
        subtitleRun.setFontFamily("Calibri");
        
        // Date
        XWPFParagraph date = document.createParagraph();
        date.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun dateRun = date.createRun();
        dateRun.setText("Generated on: " + getCurrentTimestamp());
        dateRun.setFontSize(12);
        dateRun.setFontFamily("Calibri");
        
        // Page break
        XWPFParagraph pageBreak = document.createParagraph();
        pageBreak.createRun().addBreak(BreakType.PAGE);

        // Database schema diagram disabled for performance
    }

    private void createDatabaseInfoSection(XWPFDocument document, DatabaseSchema schema) {
        // Section header
        XWPFParagraph header = document.createParagraph();
        XWPFRun headerRun = header.createRun();
        headerRun.setText("Database Information");
        headerRun.setBold(true);
        headerRun.setFontSize(18);
        headerRun.setFontFamily("Calibri");
        
        // Create table for database info
        XWPFTable table = document.createTable(5, 2);
        table.setWidth("100%");
        
        // Set table style
        table.getCTTbl().getTblPr().unsetTblBorders();
        
        // Add data
        setTableCell(table.getRow(0).getCell(0), "Database Name:", true);
        setTableCell(table.getRow(0).getCell(1), schema.getDatabaseName(), false);
        
        setTableCell(table.getRow(1).getCell(0), "Database Type:", true);
        setTableCell(table.getRow(1).getCell(1), schema.getDatabaseProductName(), false);
        
        setTableCell(table.getRow(2).getCell(0), "Version:", true);
        setTableCell(table.getRow(2).getCell(1), schema.getDatabaseProductVersion(), false);
        
        setTableCell(table.getRow(3).getCell(0), "User:", true);
        setTableCell(table.getRow(3).getCell(1), schema.getUserName(), false);
        
        setTableCell(table.getRow(4).getCell(0), "URL:", true);
        setTableCell(table.getRow(4).getCell(1), schema.getUrl(), false);
        
        document.createParagraph(); // Add space
    }

    private void createStatisticsSection(XWPFDocument document, DatabaseSchema schema) {
        // Section header
        XWPFParagraph header = document.createParagraph();
        XWPFRun headerRun = header.createRun();
        headerRun.setText("Database Statistics");
        headerRun.setBold(true);
        headerRun.setFontSize(18);
        headerRun.setFontFamily("Calibri");
        
        // Create table for statistics
        XWPFTable table = document.createTable(4, 2);
        table.setWidth("100%");
        
        // Add statistics
        setTableCell(table.getRow(0).getCell(0), "Total Schemas:", true);
        setTableCell(table.getRow(0).getCell(1), String.valueOf(schema.getSchemas().size()), false);
        
        setTableCell(table.getRow(1).getCell(0), "Total Tables:", true);
        setTableCell(table.getRow(1).getCell(1), String.valueOf(schema.getTables().size()), false);
        
        setTableCell(table.getRow(2).getCell(0), "Total Columns:", true);
        setTableCell(table.getRow(2).getCell(1), String.valueOf(getTotalColumns(schema)), false);
        
        setTableCell(table.getRow(3).getCell(0), "Total Views:", true);
        setTableCell(table.getRow(3).getCell(1), String.valueOf(getViewCount(schema)), false);
        
        document.createParagraph(); // Add space
    }

    private void createTablesSection(XWPFDocument document, DatabaseSchema schema) {
        // Section header
        XWPFParagraph header = document.createParagraph();
        XWPFRun headerRun = header.createRun();
        headerRun.setText("Tables and Columns");
        headerRun.setBold(true);
        headerRun.setFontSize(18);
        headerRun.setFontFamily("Calibri");
        
        for (TableSchema table : schema.getTables()) {
            createTableSection(document, table);
        }
    }

    private void createTableSection(XWPFDocument document, TableSchema table) {
        // Table name
        XWPFParagraph tableName = document.createParagraph();
        XWPFRun tableNameRun = tableName.createRun();
        tableNameRun.setText(table.getTableName());
        tableNameRun.setBold(true);
        tableNameRun.setFontSize(14);
        tableNameRun.setFontFamily("Calibri");
        
        // Table info
        XWPFParagraph tableInfo = document.createParagraph();
        XWPFRun tableInfoRun = tableInfo.createRun();
        tableInfoRun.setText("Type: " + table.getTableType() + " | Schema: " + table.getSchemaName());
        tableInfoRun.setItalic(true);
        tableInfoRun.setFontSize(10);
        tableInfoRun.setFontFamily("Calibri");
        
        // Table description
        if (table.getRemarks() != null && !table.getRemarks().isEmpty()) {
            XWPFParagraph description = document.createParagraph();
            XWPFRun descRun = description.createRun();
            descRun.setText("Description: " + table.getRemarks());
            descRun.setFontSize(11);
            descRun.setFontFamily("Calibri");
        }
        
        // Columns table
        if (!table.getColumns().isEmpty()) {
            XWPFTable columnsTable = document.createTable(table.getColumns().size() + 1, 7);
            columnsTable.setWidth("100%");
            
            // Header row
            XWPFTableRow headerRow = columnsTable.getRow(0);
            setTableCell(headerRow.getCell(0), "Column", true);
            setTableCell(headerRow.getCell(1), "Data Type", true);
            setTableCell(headerRow.getCell(2), "Size", true);
            setTableCell(headerRow.getCell(3), "Nullable", true);
            setTableCell(headerRow.getCell(4), "Default", true);
            setTableCell(headerRow.getCell(5), "Key", true);
            setTableCell(headerRow.getCell(6), "Remarks", true);
            
            // Data rows
            for (int i = 0; i < table.getColumns().size(); i++) {
                ColumnSchema column = table.getColumns().get(i);
                XWPFTableRow row = columnsTable.getRow(i + 1);
                
                setTableCell(row.getCell(0), column.getColumnName(), column.isPrimaryKey());
                setTableCell(row.getCell(1), column.getDataType(), false);
                setTableCell(row.getCell(2), column.getColumnSize() != null ? column.getColumnSize().toString() : "-", false);
                setTableCell(row.getCell(3), column.isNullable() ? "YES" : "NO", false);
                setTableCell(row.getCell(4), column.getDefaultValue() != null ? column.getDefaultValue() : "-", false);
                
                String keyInfo = "";
                if (column.isPrimaryKey()) keyInfo += "PK ";
                if (column.isForeignKey()) keyInfo += "FK ";
                if (keyInfo.isEmpty()) keyInfo = "-";
                setTableCell(row.getCell(5), keyInfo.trim(), false);
                
                setTableCell(row.getCell(6), column.getRemarks() != null ? column.getRemarks() : "-", false);
            }
        }
        
        // Foreign keys section
        if (!table.getForeignKeys().isEmpty()) {
            XWPFParagraph fkHeader = document.createParagraph();
            XWPFRun fkHeaderRun = fkHeader.createRun();
            fkHeaderRun.setText("Relationships:");
            fkHeaderRun.setBold(true);
            fkHeaderRun.setFontSize(12);
            fkHeaderRun.setFontFamily("Calibri");
            
            for (TableSchema.ForeignKey fk : table.getForeignKeys()) {
                XWPFParagraph fkInfo = document.createParagraph();
                XWPFRun fkRun = fkInfo.createRun();
                fkRun.setText("• " + fk.getColumnName() + " → " + fk.getReferencedTable() + "." + fk.getReferencedColumn());
                fkRun.setFontSize(11);
                fkRun.setFontFamily("Calibri");
            }
        }
        
        document.createParagraph(); // Add space between tables
    }

    private void setTableCell(XWPFTableCell cell, String text, boolean bold) {
        XWPFParagraph paragraph = cell.getParagraphs().get(0);
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setBold(bold);
        run.setFontSize(10);
        run.setFontFamily("Calibri");
        
        if (bold) {
            // Set background color for header cells
            cell.setColor("F0F0F0");
        }
    }

    private int getTotalColumns(DatabaseSchema schema) {
        return schema.getTables().stream()
                .mapToInt(table -> table.getColumns().size())
                .sum();
    }

    private int getViewCount(DatabaseSchema schema) {
        return (int) schema.getTables().stream()
                .filter(table -> "VIEW".equalsIgnoreCase(table.getTableType()))
                .count();
    }

    private String getCurrentTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * Add database schema diagram to the document.
     */
    private void addDatabaseDiagram(XWPFDocument document, DatabaseSchema schema) {
        try {
            log.info("Adding database diagram to DOCX report");

            // Add diagram section heading
            XWPFParagraph diagramHeading = document.createParagraph();
            diagramHeading.setAlignment(ParagraphAlignment.LEFT);
            XWPFRun headingRun = diagramHeading.createRun();
            headingRun.setText("📊 Database Schema Diagram");
            headingRun.setBold(true);
            headingRun.setFontSize(18);
            headingRun.setColor("2E74B5");
            diagramHeading.createRun().addBreak();

            // Generate diagram image
            byte[] imageBytes = diagramGenerationService.generateDiagramPNG(schema);

            // Add image to document
            XWPFParagraph imageParagraph = document.createParagraph();
            imageParagraph.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun imageRun = imageParagraph.createRun();

            // Calculate image dimensions (max width: 6 inches)
            int maxWidthEMU = 6 * 914400; // 6 inches in EMU
            int maxHeightEMU = 8 * 914400; // 8 inches in EMU

            imageRun.addPicture(
                new java.io.ByteArrayInputStream(imageBytes),
                XWPFDocument.PICTURE_TYPE_PNG,
                "database-schema-diagram.png",
                maxWidthEMU, maxHeightEMU
            );

            // Add diagram description
            XWPFParagraph descParagraph = document.createParagraph();
            descParagraph.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun descRun = descParagraph.createRun();
            descRun.setText("Figure: Database schema showing table relationships and structure");
            descRun.setItalic(true);
            descRun.setFontSize(10);
            descRun.setColor("666666");

            // Add space after diagram
            document.createParagraph().createRun().addBreak();
            document.createParagraph().createRun().addBreak();
            
            log.info("Successfully added database diagram to DOCX report");

        } catch (Exception e) {
            log.error("Failed to add database diagram to DOCX: {}", e.getMessage(), e);
            
            // Add error message instead
            XWPFParagraph errorParagraph = document.createParagraph();
            XWPFRun errorRun = errorParagraph.createRun();
            errorRun.setText("⚠️ Unable to generate database diagram: " + e.getMessage());
            errorRun.setColor("CC0000");
            errorParagraph.createRun().addBreak();
        }
    }
}
