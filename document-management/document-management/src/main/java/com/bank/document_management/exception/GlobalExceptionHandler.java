package com.bank.document_management.exception;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.bank.document_management.dto.response.ApiError;
import com.bank.document_management.dto.response.ApiResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<Object> handleApiException(ApiException ex, WebRequest request) {
		return build(ex.getStatus(), request, List.of(new ApiError(ex.getErrorCode().name(), ex.getMessage())));
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<Object> handleDataIntegrity(DataIntegrityViolationException ex, WebRequest request) {
		log.warn("Data integrity violation", ex);
		return build(HttpStatus.CONFLICT, request,
				List.of(new ApiError(ErrorCode.DUPLICATE_RESOURCE.name(), "Request conflicts with existing data")));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Object> handleUnexpected(Exception ex, WebRequest request) {
		log.error("Unexpected error", ex);
		return build(HttpStatus.INTERNAL_SERVER_ERROR, request,
				List.of(new ApiError(ErrorCode.INTERNAL_ERROR.name(), "Unexpected error occurred")));
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<ApiError> errors = ex.getBindingResult().getFieldErrors().stream()
				.map(e -> new ApiError(ErrorCode.VALIDATION_FAILED.name(), e.getField() + ": " + e.getDefaultMessage()))
				.toList();
		return build(HttpStatus.BAD_REQUEST, request, errors);
	}

	/** Wraps every framework-raised error (bad JSON, wrong method, unknown path, ...) in the standard envelope. */
	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
			HttpStatusCode statusCode, WebRequest request) {
		String message = body instanceof ProblemDetail pd && pd.getDetail() != null ? pd.getDetail()
				: "Request could not be processed";
		ErrorCode code = statusCode.value() == 400 ? ErrorCode.MALFORMED_REQUEST : ErrorCode.REQUEST_ERROR;
		return build(HttpStatus.valueOf(statusCode.value()), request, List.of(new ApiError(code.name(), message)));
	}

	private ResponseEntity<Object> build(HttpStatus status, WebRequest request, List<ApiError> errors) {
		String instance = ((ServletWebRequest) request).getRequest().getRequestURI();
		return ResponseEntity.status(status).body(ApiResponse.failure(status, instance, errors));
	}
}
