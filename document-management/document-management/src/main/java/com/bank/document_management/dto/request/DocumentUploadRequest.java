package com.bank.document_management.dto.request;

import java.util.Locale;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record DocumentUploadRequest(
		@NotNull @Positive Long customerId,
		@NotBlank @Pattern(regexp = "NRIC|SALARY_SLIP|BANK_STATEMENT",
				message = "must be one of NRIC, SALARY_SLIP, BANK_STATEMENT") String documentType) {

	public DocumentUploadRequest {
		if (documentType != null) {
			documentType = documentType.toUpperCase(Locale.ROOT);
		}
	}
}
