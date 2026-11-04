package com.bank.document_management.dto.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;

import com.bank.document_management.util.DateTimeUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Standard response envelope (RFC 9457 style) used for every success and error response.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
		String type,
		String title,
		int status,
		String instance,
		@JsonProperty("isSuccess") boolean isSuccess,
		OffsetDateTime timestamp,
		String traceId,
		List<ApiError> errors,
		T data) {

	public static <T> ApiResponse<T> success(HttpStatus status, String instance, T data) {
		return new ApiResponse<>(typeOf(status), "Success", status.value(), instance, true,
				DateTimeUtil.now(), currentTraceId(), List.of(), data);
	}

	public static ApiResponse<Void> failure(HttpStatus status, String instance, List<ApiError> errors) {
		return new ApiResponse<>(typeOf(status), status.getReasonPhrase(), status.value(), instance, false,
				DateTimeUtil.now(), currentTraceId(), errors, null);
	}

	private static String currentTraceId() {
		String traceId = MDC.get("traceId");
		return traceId != null ? traceId : UUID.randomUUID().toString();
	}

	private static String typeOf(HttpStatus status) {
		String section = switch (status.value()) {
			case 200 -> "6.3.1";
			case 201 -> "6.3.2";
			case 400 -> "6.5.1";
			case 404 -> "6.5.4";
			case 405 -> "6.5.5";
			case 409 -> "6.5.8";
			case 413 -> "6.5.11";
			case 415 -> "6.5.13";
			default -> status.is5xxServerError() ? "6.6.1" : "6.5.1";
		};
		return "https://tools.ietf.org/html/rfc7231#section-" + section;
	}
}
