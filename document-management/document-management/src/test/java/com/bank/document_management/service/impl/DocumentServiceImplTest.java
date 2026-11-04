package com.bank.document_management.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.bank.document_management.client.ThirdPartyVerificationClient;
import com.bank.document_management.dto.request.DocumentStatusUpdateRequest;
import com.bank.document_management.dto.request.DocumentUploadRequest;
import com.bank.document_management.dto.response.DocumentResponse;
import com.bank.document_management.dto.response.VerificationResponse;
import com.bank.document_management.entity.Customer;
import com.bank.document_management.entity.Document;
import com.bank.document_management.entity.DocumentHistory;
import com.bank.document_management.enums.DocumentStatus;
import com.bank.document_management.enums.VerificationResult;
import com.bank.document_management.exception.ApiException;
import com.bank.document_management.exception.InvalidFileException;
import com.bank.document_management.exception.InvalidStatusTransitionException;
import com.bank.document_management.exception.ResourceNotFoundException;
import com.bank.document_management.mapper.DocumentMapper;
import com.bank.document_management.repository.CustomerRepository;
import com.bank.document_management.repository.DocumentHistoryRepository;
import com.bank.document_management.repository.DocumentRepository;
import com.bank.document_management.service.FileStorageService;
import com.bank.document_management.service.StoredFile;

@ExtendWith(MockitoExtension.class)
class DocumentServiceImplTest {

	private static final StoredFile STORED = new StoredFile(
			"nric.pdf", "uuid-1.pdf", "2026/uuid-1.pdf", "application/pdf", 1234L);

	@Mock
	private CustomerRepository customerRepository;
	@Mock
	private DocumentRepository documentRepository;
	@Mock
	private DocumentHistoryRepository documentHistoryRepository;
	@Mock
	private FileStorageService fileStorageService;
	@Mock
	private ThirdPartyVerificationClient verificationClient;

	private DocumentServiceImpl service;
	private final MockMultipartFile file = new MockMultipartFile("file", "nric.pdf", "application/pdf", new byte[] { 1 });

	@BeforeEach
	void setUp() {
		service = new DocumentServiceImpl(customerRepository, documentRepository, documentHistoryRepository,
				fileStorageService, new DocumentMapper(), verificationClient);
		// upload() registers a transaction callback, which needs an active synchronization
		TransactionSynchronizationManager.initSynchronization();
	}

	@AfterEach
	void tearDown() {
		TransactionSynchronizationManager.clearSynchronization();
	}

	private Customer customer() {
		Customer customer = new Customer();
		customer.setId(4L);
		return customer;
	}

	private void stubSuccessfulUpload() {
		when(customerRepository.findById(4L)).thenReturn(Optional.of(customer()));
		when(fileStorageService.store(file)).thenReturn(STORED);
		when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
			Document saved = invocation.getArgument(0);
			saved.setId(29L);
			saved.setUploadedDate(LocalDateTime.of(2026, 11, 4, 11, 48));
			saved.setUpdatedDate(LocalDateTime.of(2026, 11, 4, 11, 48));
			return saved;
		});
	}

	@Test
	void upload_validRequest_savesPendingDocumentAndHistory() {
		stubSuccessfulUpload();

		DocumentResponse response = service.upload(new DocumentUploadRequest(4L, "nric"), file);

		assertThat(response.documentId()).isEqualTo(29L);
		assertThat(response.customerId()).isEqualTo(4L);
		assertThat(response.documentType()).isEqualTo("NRIC");
		assertThat(response.fileName()).isEqualTo("nric.pdf");
		assertThat(response.status()).isEqualTo(DocumentStatus.PENDING);

		ArgumentCaptor<DocumentHistory> history = ArgumentCaptor.forClass(DocumentHistory.class);
		verify(documentHistoryRepository).save(history.capture());
		assertThat(history.getValue().getPreviousStatus()).isNull();
		assertThat(history.getValue().getNewStatus()).isEqualTo(DocumentStatus.PENDING);
		assertThat(history.getValue().getUpdatedBy()).isEqualTo("CUSTOMER");
	}

	@Test
	void upload_unknownCustomer_throwsNotFoundAndStoresNothing() {
		when(customerRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.upload(new DocumentUploadRequest(99L, "NRIC"), file))
				.isInstanceOf(ResourceNotFoundException.class);

		verifyNoInteractions(fileStorageService, documentRepository, documentHistoryRepository);
	}

	@Test
	void upload_invalidFile_throwsAndSavesNothing() {
		when(customerRepository.findById(4L)).thenReturn(Optional.of(customer()));
		when(fileStorageService.store(file)).thenThrow(new InvalidFileException("only PDF, JPG and PNG files are allowed"));

		assertThatThrownBy(() -> service.upload(new DocumentUploadRequest(4L, "NRIC"), file))
				.isInstanceOf(InvalidFileException.class);

		verify(documentRepository, never()).save(any());
		verify(documentHistoryRepository, never()).save(any());
	}

	@Test
	void upload_transactionRollsBack_deletesStoredFile() {
		stubSuccessfulUpload();
		service.upload(new DocumentUploadRequest(4L, "NRIC"), file);

		TransactionSynchronizationManager.getSynchronizations()
				.forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

		verify(fileStorageService).delete(STORED.relativePath());
	}

	@Test
	void upload_transactionCommits_keepsStoredFile() {
		stubSuccessfulUpload();
		service.upload(new DocumentUploadRequest(4L, "NRIC"), file);

		TransactionSynchronizationManager.getSynchronizations()
				.forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));

		verify(fileStorageService, never()).delete(any());
	}

	private Document document(DocumentStatus status) {
		Document document = new Document();
		document.setId(7L);
		document.setCustomer(customer());
		document.setDocumentType("NRIC");
		document.setFileName("nric.pdf");
		document.setFilePath("2026/uuid-1.pdf");
		document.setContentType("application/pdf");
		document.setFileSize(1234L);
		document.setVerificationStatus(status);
		document.setUploadedDate(LocalDateTime.of(2026, 11, 4, 11, 48));
		document.setUpdatedDate(LocalDateTime.of(2026, 11, 4, 11, 48));
		return document;
	}

	@Test
	void getById_unknownDocument_throwsNotFound() {
		when(documentRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getById(99L)).isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void updateStatus_pendingDocument_approvesAndWritesHistory() {
		when(documentRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(document(DocumentStatus.PENDING)));

		DocumentResponse response = service.updateStatus(7L, new DocumentStatusUpdateRequest("approved", null));

		assertThat(response.status()).isEqualTo(DocumentStatus.APPROVED);
		ArgumentCaptor<DocumentHistory> history = ArgumentCaptor.forClass(DocumentHistory.class);
		verify(documentHistoryRepository).save(history.capture());
		assertThat(history.getValue().getPreviousStatus()).isEqualTo(DocumentStatus.PENDING);
		assertThat(history.getValue().getNewStatus()).isEqualTo(DocumentStatus.APPROVED);
		assertThat(history.getValue().getUpdatedBy()).isEqualTo("OFFICER");
	}

	@Test
	void updateStatus_rejectWithoutRemarks_throwsValidationErrorAndTouchesNothing() {
		assertThatThrownBy(() -> service.updateStatus(7L, new DocumentStatusUpdateRequest("REJECTED", " ")))
				.isInstanceOf(ApiException.class)
				.hasMessageContaining("remarks");

		verifyNoInteractions(documentRepository, documentHistoryRepository);
	}

	@Test
	void updateStatus_alreadyDecided_throwsConflict() {
		when(documentRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(document(DocumentStatus.APPROVED)));

		assertThatThrownBy(() -> service.updateStatus(7L, new DocumentStatusUpdateRequest("REJECTED", "blurry")))
				.isInstanceOf(InvalidStatusTransitionException.class);

		verify(documentHistoryRepository, never()).save(any());
	}

	@Test
	void list_pageBelowOne_throwsValidationError() {
		assertThatThrownBy(() -> service.list(0, null, null)).isInstanceOf(ApiException.class);
	}

	@Test
	void verify_callsThirdPartyAndReturnsItsResult() {
		when(documentRepository.findById(7L)).thenReturn(Optional.of(document(DocumentStatus.PENDING)));
		when(verificationClient.verify(7L, "NRIC", "nric.pdf")).thenReturn(VerificationResult.UNABLE_TO_VERIFY);

		VerificationResponse response = service.verify(7L);

		assertThat(response.documentId()).isEqualTo(7L);
		assertThat(response.result()).isEqualTo(VerificationResult.UNABLE_TO_VERIFY);
	}
}
