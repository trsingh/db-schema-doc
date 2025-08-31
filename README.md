# DB Schema Doc

A Spring Boot application for generating database schema documentation from metadata queries.

## Features

- Read-only database access (PostgreSQL/MySQL)
- Database schema metadata extraction
- Document generation (DOCX format)
- REST API with OpenAPI documentation
- No DDL/DML operations - metadata queries only

## Requirements

- JDK 21
- Maven 3.6+
- PostgreSQL or MySQL database with read-only access

## Configuration

Update `src/main/resources/application.properties` with your database connection details:

### MySQL (Default)
```properties
spring.datasource.url=jdbc:mysql://localhost:3309/galaxy_compliance_mcbankny
spring.datasource.username=root
spring.datasource.password=root
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
```

### PostgreSQL (Alternative)
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/your_database_name
spring.datasource.username=readonly_user
spring.datasource.password=readonly_password
spring.datasource.driver-class-name=org.postgresql.Driver
```

## Running the Application

```bash
mvn spring-boot:run
```

The application will start on port 8080.

## API Documentation

Once the application is running, you can access:

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/api-docs

## Security

- Application is configured for read-only database access
- Connection pool is set to read-only mode
- JPA/Hibernate is configured to prevent DDL operations
- Transaction management enforces read-only transactions

## Dependencies

- Spring Boot 3.3.3
- Spring Data JPA
- Spring Web
- PostgreSQL/MySQL drivers
- Apache POI (for DOCX generation)
- JSoup (for HTML parsing)
- Lombok
- SpringDoc OpenAPI
