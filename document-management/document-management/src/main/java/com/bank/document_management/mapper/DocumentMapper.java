package com.bank.document_management.mapper;

import org.springframework.stereotype.Component;

import com.bank.document_management.dto.response.DocumentHistoryResponse;
import com.bank.document_management.dto.response.DocumentResponse;
import com.bank.document_management.entity.Customer;
import com.bank.document_management.entity.Document;
import com.bank.document_management.entity.DocumentHistory;
import com.bank.document_management.enums.DocumentStatus;
import com.bank.document_management.service.StoredFile;
import com.bank.document_management.util.DateTimeUtil;

@Component
public class DocumentMapper {

	public Document toEntity(Customer customer, String documentType, StoredFile file) {
		Document document = new Document();
		document.setCustomer(customer);
		document.setDocumentType(documentType);
		document.setFileName(file.originalFileName());
		document.setStoredFileName(file.storedFileName());
		document.setFilePath(file.relativePath());
		document.setContentType(file.contentType());
		document.setFileSize(file.size());
		document.setVerificationStatus(DocumentStatus.PENDING);
		return document;
	}

	public DocumentHistoryResponse toHistoryResponse(DocumentHistory history) {
		return new DocumentHistoryResponse(
				history.getId(),
				history.getPreviousStatus(),
				history.getNewStatus(),
				history.getRemarks(),
				history.getUpdatedBy(),
				DateTimeUtil.toOffset(history.getUpdatedDate()));
	}

	public DocumentResponse toResponse(Document document) {
		return new DocumentResponse(
				document.getId(),
				document.getCustomer().getId(),
				document.getDocumentType(),
				document.getFileName(),
				document.getContentType(),
				document.getFileSize(),
				document.getVerificationStatus(),
				DateTimeUtil.toOffset(document.getUploadedDate()),
				DateTimeUtil.toOffset(document.getUpdatedDate()));
	}
}
