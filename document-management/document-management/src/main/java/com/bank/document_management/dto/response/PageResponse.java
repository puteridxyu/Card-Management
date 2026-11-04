package com.bank.document_management.dto.response;

import java.util.List;

import org.springframework.data.domain.Page;

/** {@code page} is 1-based. */
public record PageResponse<T>(
		List<T> content,
		int page,
		int size,
		long totalElements,
		int totalPages) {

	public static <T> PageResponse<T> of(Page<T> page) {
		return new PageResponse<>(page.getContent(), page.getNumber() + 1, page.getSize(),
				page.getTotalElements(), page.getTotalPages());
	}
}
