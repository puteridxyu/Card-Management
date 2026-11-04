package com.bank.document_management.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.bank.document_management.dto.response.DocumentResponse;
import com.bank.document_management.enums.DocumentStatus;
import com.bank.document_management.exception.InvalidFileException;
import com.bank.document_management.exception.ResourceNotFoundException;
import com.bank.document_management.service.DocumentService;

@WebMvcTest(DocumentController.class)
class DocumentControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private DocumentService documentService;

	private static MockMultipartFile data(String json) {
		return new MockMultipartFile("data", "", MediaType.APPLICATION_JSON_VALUE, json.getBytes());
	}

	private static MockMultipartFile pdf() {
		return new MockMultipartFile("file", "nric.pdf", "application/pdf", "%PDF-1.4".getBytes());
	}

	private static DocumentResponse sample() {
		OffsetDateTime now = OffsetDateTime.now();
		return new DocumentResponse(29L, 4L, "NRIC", "nric.pdf", "application/pdf", 1234L,
				DocumentStatus.PENDING, now, now);
	}

	@Test
	void upload_valid_returns201WithLocation() throws Exception {
		when(documentService.upload(any(), any())).thenReturn(sample());

		mockMvc.perform(multipart("/api/documents").file(data("{\"customerId\":4,\"documentType\":\"NRIC\"}")).file(pdf()))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/api/documents/29"))
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.data.documentId").value(29))
				.andExpect(jsonPath("$.data.status").value("PENDING"))
				.andExpect(jsonPath("$.data.filePath").doesNotExist())
				.andExpect(jsonPath("$.data.storedFileName").doesNotExist());
	}

	@Test
	void upload_invalidDocumentType_returns400ValidationFailed() throws Exception {
		mockMvc.perform(multipart("/api/documents").file(data("{\"customerId\":4,\"documentType\":\"PASSPORT\"}")).file(pdf()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].errorCode").value("VALIDATION_FAILED"));
	}

	@Test
	void upload_missingFilePart_returns400() throws Exception {
		mockMvc.perform(multipart("/api/documents").file(data("{\"customerId\":4,\"documentType\":\"NRIC\"}")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.isSuccess").value(false));
	}

	@Test
	void upload_unknownCustomer_returns404() throws Exception {
		when(documentService.upload(any(), any())).thenThrow(new ResourceNotFoundException("Customer not found: 99"));

		mockMvc.perform(multipart("/api/documents").file(data("{\"customerId\":99,\"documentType\":\"NRIC\"}")).file(pdf()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.errors[0].errorCode").value("RESOURCE_NOT_FOUND"));
	}

	@Test
	void upload_invalidFile_returns400InvalidFile() throws Exception {
		when(documentService.upload(any(), any()))
				.thenThrow(new InvalidFileException("only PDF, JPG and PNG files are allowed"));

		mockMvc.perform(multipart("/api/documents").file(data("{\"customerId\":4,\"documentType\":\"NRIC\"}")).file(pdf()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].errorCode").value("INVALID_FILE"));
	}

	@Test
	void upload_fileTooLarge_returns413() throws Exception {
		when(documentService.upload(any(), any()))
				.thenThrow(new InvalidFileException(HttpStatus.valueOf(413), "file exceeds the maximum size"));

		mockMvc.perform(multipart("/api/documents").file(data("{\"customerId\":4,\"documentType\":\"NRIC\"}")).file(pdf()))
				.andExpect(status().isContentTooLarge());
	}

	@Test
	void upload_nonMultipartRequest_returns415() throws Exception {
		mockMvc.perform(post("/api/documents")
				.contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isUnsupportedMediaType())
				.andExpect(jsonPath("$.isSuccess").value(false));
	}
}
