package com.bank.document_management.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(String uploadDir, long maxFileSizeBytes) {
}
