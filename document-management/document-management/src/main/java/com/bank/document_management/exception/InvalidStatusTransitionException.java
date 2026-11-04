package com.bank.document_management.exception;

import org.springframework.http.HttpStatus;

public class InvalidStatusTransitionException extends ApiException {

	public InvalidStatusTransitionException(String message) {
		super(HttpStatus.CONFLICT, ErrorCode.INVALID_STATUS_TRANSITION, message);
	}
}
