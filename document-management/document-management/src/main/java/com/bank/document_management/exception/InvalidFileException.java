package com.bank.document_management.exception;

import org.springframework.http.HttpStatus;

public class InvalidFileException extends ApiException {

	public InvalidFileException(String message) {
		super(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_FILE, message);
	}

	public InvalidFileException(HttpStatus status, String message) {
		super(status, ErrorCode.INVALID_FILE, message);
	}
}
