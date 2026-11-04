package com.bank.document_management.dto.external;

/** Payload sent to the 3rd-party verification API. */
public record ThirdPartyVerifyRequest(Long documentId, String documentType, String fileName) {
}
