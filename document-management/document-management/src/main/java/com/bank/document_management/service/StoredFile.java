package com.bank.document_management.service;

public record StoredFile(
		String originalFileName,
		String storedFileName,
		String relativePath,
		String contentType,
		long size) {
}
