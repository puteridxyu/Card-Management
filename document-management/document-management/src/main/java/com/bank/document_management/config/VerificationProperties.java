package com.bank.document_management.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.verification")
public record VerificationProperties(String baseUrl, Duration timeout) {
}
