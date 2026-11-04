package com.bank.document_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CustomerSearchRequest(
		@NotBlank @Pattern(regexp = "\\d{12}", message = "must be 12 digits without dashes") String idNo) {
}
