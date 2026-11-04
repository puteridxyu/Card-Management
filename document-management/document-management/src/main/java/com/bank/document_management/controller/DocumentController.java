package com.bank.document_management.controller;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.bank.document_management.dto.request.DocumentStatusUpdateRequest;
import com.bank.document_management.dto.request.DocumentUploadRequest;
import com.bank.document_management.dto.response.ApiResponse;
import com.bank.document_management.dto.response.DocumentHistoryResponse;
import com.bank.document_management.dto.response.DocumentResponse;
import com.bank.document_management.dto.response.PageResponse;
import com.bank.document_management.dto.response.VerificationResponse;
import com.bank.document_management.enums.DocumentStatus;
import com.bank.document_management.service.DocumentContent;
import com.bank.document_management.service.DocumentService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

	private final DocumentService documentService;

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ApiResponse<DocumentResponse>> upload(
			@Valid @RequestPart("data") DocumentUploadRequest data,
			@RequestPart("file") MultipartFile file,
			HttpServletRequest httpRequest) {
		DocumentResponse document = documentService.upload(data, file);
		URI location = URI.create(httpRequest.getRequestURI() + "/" + document.documentId());
		return ResponseEntity.created(location)
				.body(ApiResponse.success(HttpStatus.CREATED, httpRequest.getRequestURI(), document));
	}

	@GetMapping("/{documentId}")
	public ResponseEntity<ApiResponse<DocumentResponse>> getById(
			@PathVariable Long documentId, HttpServletRequest httpRequest) {
		return ok(httpRequest, documentService.getById(documentId));
	}

	@GetMapping("/{documentId}/content")
	public ResponseEntity<Resource> download(@PathVariable Long documentId) {
		DocumentContent content = documentService.getContent(documentId);
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(content.contentType()))
				.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
						.filename(content.fileName(), StandardCharsets.UTF_8).build().toString())
				.header("X-Content-Type-Options", "nosniff")
				.body(content.resource());
	}

	@GetMapping
	public ResponseEntity<ApiResponse<PageResponse<DocumentResponse>>> list(
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(required = false) DocumentStatus status,
			@RequestParam(required = false) Long customerId,
			HttpServletRequest httpRequest) {
		return ok(httpRequest, documentService.list(page, status, customerId));
	}

	@PatchMapping("/{documentId}/status")
	public ResponseEntity<ApiResponse<DocumentResponse>> updateStatus(
			@PathVariable Long documentId,
			@Valid @RequestBody DocumentStatusUpdateRequest request,
			HttpServletRequest httpRequest) {
		return ok(httpRequest, documentService.updateStatus(documentId, request));
	}

	@GetMapping("/{documentId}/history")
	public ResponseEntity<ApiResponse<List<DocumentHistoryResponse>>> history(
			@PathVariable Long documentId, HttpServletRequest httpRequest) {
		return ok(httpRequest, documentService.getHistory(documentId));
	}

	@PostMapping("/{documentId}/verifications")
	public ResponseEntity<ApiResponse<VerificationResponse>> verify(
			@PathVariable Long documentId, HttpServletRequest httpRequest) {
		return ok(httpRequest, documentService.verify(documentId));
	}

	private static <T> ResponseEntity<ApiResponse<T>> ok(HttpServletRequest httpRequest, T data) {
		return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, httpRequest.getRequestURI(), data));
	}
}
