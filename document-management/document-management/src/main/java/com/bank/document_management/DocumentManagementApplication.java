package com.bank.document_management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DocumentManagementApplication {

	public static void main(String[] args) {
		SpringApplication.run(DocumentManagementApplication.class, args);
	}

}
