package com.bank.document_management.exception;

import org.springframework.http.HttpStatus;

public class DuplicateResourceException extends ApiException {

	public DuplicateResourceException(String message) {
		super(HttpStatus.CONFLICT, ErrorCode.DUPLICATE_RESOURCE, message);
	}
}
