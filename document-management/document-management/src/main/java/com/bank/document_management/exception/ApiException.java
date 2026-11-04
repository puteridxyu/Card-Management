package com.bank.document_management.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {

	private final HttpStatus status;
	private final ErrorCode errorCode;

	public ApiException(HttpStatus status, ErrorCode errorCode, String message) {
		super(message);
		this.status = status;
		this.errorCode = errorCode;
	}
}
