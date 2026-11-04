package com.bank.document_management.dto.external;

/** Payload returned by the 3rd-party verification API. */
public record ThirdPartyVerifyResponse(boolean verified) {
}
