package com.bank.document_management.dto.request;

import java.util.Locale;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DocumentStatusUpdateRequest(
		@NotBlank @Pattern(regexp = "APPROVED|REJECTED", message = "must be APPROVED or REJECTED") String status,
		@Size(max = 500) String remarks) {

	public DocumentStatusUpdateRequest {
		if (status != null) {
			status = status.trim().toUpperCase(Locale.ROOT);
		}
		if (remarks != null) {
			remarks = remarks.trim().isEmpty() ? null : remarks.trim();
		}
	}
}
