package com.bank.document_management.dto.request;

import java.util.Locale;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CustomerRequest(
		@NotBlank @Size(max = 100) String fullName,
		@NotBlank @Pattern(regexp = "\\d{12}", message = "must be 12 digits without dashes") String idNo,
		@NotBlank @Email @Size(max = 100) String emailAddress) {

	public CustomerRequest {
		if (emailAddress != null) {
			emailAddress = emailAddress.toLowerCase(Locale.ROOT);
		}
	}
}
