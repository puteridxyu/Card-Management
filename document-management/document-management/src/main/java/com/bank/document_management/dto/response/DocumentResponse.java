package com.bank.document_management.dto.response;

import java.time.OffsetDateTime;

import com.bank.document_management.enums.DocumentStatus;

public record DocumentResponse(
		Long documentId,
		Long customerId,
		String documentType,
		String fileName,
		String contentType,
		Long fileSize,
		DocumentStatus status,
		OffsetDateTime uploadedDate,
		OffsetDateTime updatedDate) {
}
