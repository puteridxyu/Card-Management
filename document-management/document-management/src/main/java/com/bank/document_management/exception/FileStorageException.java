package com.bank.document_management.exception;

import org.springframework.http.HttpStatus;

public class FileStorageException extends ApiException {

	public FileStorageException(String message, Throwable cause) {
		super(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.FILE_STORAGE_ERROR, message);
		initCause(cause);
	}
}
