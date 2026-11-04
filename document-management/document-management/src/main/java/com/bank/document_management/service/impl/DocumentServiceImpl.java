package com.bank.document_management.service.impl;

import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import com.bank.document_management.client.ThirdPartyVerificationClient;
import com.bank.document_management.dto.request.DocumentStatusUpdateRequest;
import com.bank.document_management.dto.request.DocumentUploadRequest;
import com.bank.document_management.dto.response.DocumentHistoryResponse;
import com.bank.document_management.dto.response.DocumentResponse;
import com.bank.document_management.dto.response.PageResponse;
import com.bank.document_management.dto.response.VerificationResponse;
import com.bank.document_management.entity.Customer;
import com.bank.document_management.entity.Document;
import com.bank.document_management.entity.DocumentHistory;
import com.bank.document_management.enums.DocumentStatus;
import com.bank.document_management.enums.VerificationResult;
import com.bank.document_management.exception.ApiException;
import com.bank.document_management.exception.ErrorCode;
import com.bank.document_management.exception.InvalidStatusTransitionException;
import com.bank.document_management.exception.ResourceNotFoundException;
import com.bank.document_management.mapper.DocumentMapper;
import com.bank.document_management.repository.CustomerRepository;
import com.bank.document_management.repository.DocumentHistoryRepository;
import com.bank.document_management.repository.DocumentRepository;
import com.bank.document_management.service.DocumentContent;
import com.bank.document_management.service.DocumentService;
import com.bank.document_management.service.FileStorageService;
import com.bank.document_management.service.StoredFile;
import com.bank.document_management.util.DateTimeUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

	private static final String CUSTOMER_ACTOR = "CUSTOMER";
	private static final String OFFICER_ACTOR = "OFFICER";
	private static final int PAGE_SIZE = 10;

	private final CustomerRepository customerRepository;
	private final DocumentRepository documentRepository;
	private final DocumentHistoryRepository documentHistoryRepository;
	private final FileStorageService fileStorageService;
	private final DocumentMapper documentMapper;
	private final ThirdPartyVerificationClient verificationClient;

	@Override
	@Transactional
	public DocumentResponse upload(DocumentUploadRequest request, MultipartFile file) {
		Customer customer = customerRepository.findById(request.customerId())
				.orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + request.customerId()));

		StoredFile stored = fileStorageService.store(file);
		deleteFileIfTransactionRollsBack(stored);

		Document document = documentRepository.save(documentMapper.toEntity(customer, request.documentType(), stored));

		DocumentHistory history = new DocumentHistory();
		history.setDocument(document);
		history.setPreviousStatus(null);
		history.setNewStatus(DocumentStatus.PENDING);
		history.setUpdatedBy(CUSTOMER_ACTOR);
		documentHistoryRepository.save(history);

		return documentMapper.toResponse(document);
	}

	@Override
	@Transactional(readOnly = true)
	public DocumentResponse getById(Long documentId) {
		return documentMapper.toResponse(findDocument(documentId));
	}

	@Override
	@Transactional(readOnly = true)
	public DocumentContent getContent(Long documentId) {
		Document document = findDocument(documentId);
		Resource resource = fileStorageService.load(document.getFilePath());
		return new DocumentContent(resource, document.getFileName(), document.getContentType());
	}

	@Override
	@Transactional(readOnly = true)
	public PageResponse<DocumentResponse> list(int page, DocumentStatus status, Long customerId) {
		if (page < 1) {
			throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED,
					"page: must be greater than or equal to 1");
		}
		Specification<Document> spec = (root, query, cb) -> cb.conjunction();
		if (status != null) {
			spec = spec.and((root, query, cb) -> cb.equal(root.get("verificationStatus"), status));
		}
		if (customerId != null) {
			spec = spec.and((root, query, cb) -> cb.equal(root.get("customer").get("id"), customerId));
		}
		Sort sort = Sort.by(Sort.Direction.DESC, "uploadedDate").and(Sort.by(Sort.Direction.DESC, "id"));
		Page<DocumentResponse> result = documentRepository
				.findAll(spec, PageRequest.of(page - 1, PAGE_SIZE, sort))
				.map(documentMapper::toResponse);
		return PageResponse.of(result);
	}

	@Override
	@Transactional
	public DocumentResponse updateStatus(Long documentId, DocumentStatusUpdateRequest request) {
		DocumentStatus newStatus = DocumentStatus.valueOf(request.status());
		if (newStatus == DocumentStatus.REJECTED && request.remarks() == null) {
			throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED,
					"remarks: is required when status is REJECTED");
		}

		Document document = documentRepository.findByIdForUpdate(documentId)
				.orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));
		DocumentStatus previous = document.getVerificationStatus();
		if (previous != DocumentStatus.PENDING) {
			throw new InvalidStatusTransitionException(
					"Document is already " + previous + "; only PENDING documents can be approved or rejected");
		}

		document.setVerificationStatus(newStatus);
		documentRepository.saveAndFlush(document);

		DocumentHistory history = new DocumentHistory();
		history.setDocument(document);
		history.setPreviousStatus(previous);
		history.setNewStatus(newStatus);
		history.setRemarks(request.remarks());
		history.setUpdatedBy(OFFICER_ACTOR);
		documentHistoryRepository.save(history);

		return documentMapper.toResponse(document);
	}

	@Override
	@Transactional(readOnly = true)
	public List<DocumentHistoryResponse> getHistory(Long documentId) {
		findDocument(documentId);
		return documentHistoryRepository.findByDocumentIdOrderByUpdatedDateAscIdAsc(documentId).stream()
				.map(documentMapper::toHistoryResponse)
				.toList();
	}

	/** Not transactional on purpose: no DB connection is held while waiting for the 3rd-party API. */
	@Override
	public VerificationResponse verify(Long documentId) {
		Document document = findDocument(documentId);
		VerificationResult result = verificationClient.verify(
				document.getId(), document.getDocumentType(), document.getFileName());
		return new VerificationResponse(document.getId(), result, DateTimeUtil.now());
	}

	private Document findDocument(Long documentId) {
		return documentRepository.findById(documentId)
				.orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));
	}

	/** File writes are not covered by the DB transaction, so remove the file if the transaction fails. */
	private void deleteFileIfTransactionRollsBack(StoredFile stored) {
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCompletion(int status) {
				if (status != STATUS_COMMITTED) {
					fileStorageService.delete(stored.relativePath());
				}
			}
		});
	}
}
