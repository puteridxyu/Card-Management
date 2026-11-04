package com.bank.document_management.dto.response;

import java.time.OffsetDateTime;

import com.bank.document_management.enums.DocumentStatus;

public record DocumentHistoryResponse(
		Long historyId,
		DocumentStatus previousStatus,
		DocumentStatus newStatus,
		String remarks,
		String updatedBy,
		OffsetDateTime updatedDate) {
}
