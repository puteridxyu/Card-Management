package com.bank.document_management.dto.response;

import java.time.OffsetDateTime;

import com.bank.document_management.enums.VerificationResult;

public record VerificationResponse(
		Long documentId,
		VerificationResult result,
		OffsetDateTime verifiedDate) {
}
