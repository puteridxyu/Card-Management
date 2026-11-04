package com.bank.document_management.dto.response;

import java.time.OffsetDateTime;

public record CustomerResponse(
		Long customerId,
		String fullName,
		String idNo,
		String emailAddress,
		OffsetDateTime createdDate) {
}
