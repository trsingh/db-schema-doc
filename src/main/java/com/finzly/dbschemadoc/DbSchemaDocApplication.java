package com.finzly.dbschemadoc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
public class DbSchemaDocApplication {

	public static void main(String[] args) {
		SpringApplication.run(DbSchemaDocApplication.class, args);
	}

}
