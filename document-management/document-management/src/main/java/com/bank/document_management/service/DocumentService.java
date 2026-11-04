package com.bank.document_management.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.bank.document_management.dto.request.DocumentStatusUpdateRequest;
import com.bank.document_management.dto.request.DocumentUploadRequest;
import com.bank.document_management.dto.response.DocumentHistoryResponse;
import com.bank.document_management.dto.response.DocumentResponse;
import com.bank.document_management.dto.response.PageResponse;
import com.bank.document_management.dto.response.VerificationResponse;
import com.bank.document_management.enums.DocumentStatus;

public interface DocumentService {

	DocumentResponse upload(DocumentUploadRequest request, MultipartFile file);

	DocumentResponse getById(Long documentId);

	DocumentContent getContent(Long documentId);

	/** {@code page} is 1-based; the page size is fixed at 10. Both filters are optional. */
	PageResponse<DocumentResponse> list(int page, DocumentStatus status, Long customerId);

	DocumentResponse updateStatus(Long documentId, DocumentStatusUpdateRequest request);

	List<DocumentHistoryResponse> getHistory(Long documentId);

	VerificationResponse verify(Long documentId);
}
