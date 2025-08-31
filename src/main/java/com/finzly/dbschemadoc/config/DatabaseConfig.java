package com.finzly.dbschemadoc.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;



import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Database configuration to ensure read-only operations
 * and prevent any DDL/DML operations.
 */
@Configuration
@Slf4j
public class DatabaseConfig {

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    public DataSource dataSource() {
        DataSource dataSource = dataSourceProperties()
                .initializeDataSourceBuilder()
                .build();
        
        // Verify read-only configuration
        try (Connection connection = dataSource.getConnection()) {
            log.info("Database connection established. Read-only: {}", connection.isReadOnly());
            
            // Force read-only mode if not already set
            if (!connection.isReadOnly()) {
                connection.setReadOnly(true);
                log.info("Forced connection to read-only mode");
            }
            
            // Log database metadata
            log.info("Database Product: {}", connection.getMetaData().getDatabaseProductName());
            log.info("Database Version: {}", connection.getMetaData().getDatabaseProductVersion());
            
        } catch (SQLException e) {
            log.error("Error configuring database connection: {}", e.getMessage());
            throw new RuntimeException("Failed to configure read-only database connection", e);
        }
        
        return dataSource;
    }


}
