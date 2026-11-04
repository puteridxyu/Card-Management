package com.bank.document_management.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.bank.document_management.dto.response.CustomerResponse;
import com.bank.document_management.exception.DuplicateResourceException;
import com.bank.document_management.exception.ResourceNotFoundException;
import com.bank.document_management.service.CustomerService;

@WebMvcTest(CustomerController.class)
class CustomerControllerTest {

	private static final String VALID_BODY = """
			{"fullName":"Ali Bin Abu","idNo":"900101015555","emailAddress":"ali@example.com"}""";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CustomerService customerService;

	private static CustomerResponse sample() {
		return new CustomerResponse(4L, "Ali Bin Abu", "900101015555", "ali@example.com", OffsetDateTime.now());
	}

	@Test
	void register_validBody_returns201WithEnvelope() throws Exception {
		when(customerService.register(any())).thenReturn(sample());

		mockMvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/api/customers/4"))
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.status").value(201))
				.andExpect(jsonPath("$.traceId").isNotEmpty())
				.andExpect(jsonPath("$.data.customerId").value(4))
				.andExpect(jsonPath("$.data.idNo").value("900101015555"));
	}

	@Test
	void register_invalidIdNo_returns400ValidationFailed() throws Exception {
		String body = """
				{"fullName":"Ali","idNo":"900101-01-5555","emailAddress":"ali@example.com"}""";

		mockMvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.isSuccess").value(false))
				.andExpect(jsonPath("$.errors[0].errorCode").value("VALIDATION_FAILED"));
	}

	@Test
	void register_missingFields_returns400() throws Exception {
		mockMvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.length()").value(3));
	}

	@Test
	void register_malformedJson_returns400MalformedRequest() throws Exception {
		mockMvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content("{not json"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].errorCode").value("MALFORMED_REQUEST"));
	}

	@Test
	void register_duplicate_returns409() throws Exception {
		when(customerService.register(any())).thenThrow(new DuplicateResourceException("duplicate"));

		mockMvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.errors[0].errorCode").value("DUPLICATE_RESOURCE"));
	}

	@Test
	void getById_existing_returns200() throws Exception {
		when(customerService.getById(4L)).thenReturn(sample());

		mockMvc.perform(get("/api/customers/4"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.customerId").value(4));
	}

	@Test
	void getById_unknown_returns404() throws Exception {
		when(customerService.getById(99L)).thenThrow(new ResourceNotFoundException("Customer not found: 99"));

		mockMvc.perform(get("/api/customers/99"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.errors[0].errorCode").value("RESOURCE_NOT_FOUND"));
	}

	@Test
	void searchByIdNo_existing_returns200() throws Exception {
		when(customerService.searchByIdNo("900101015555")).thenReturn(sample());

		mockMvc.perform(post("/api/customers/search").contentType(MediaType.APPLICATION_JSON)
				.content("{\"idNo\":\"900101015555\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.customerId").value(4));
	}

	@Test
	void searchByIdNo_unknown_returns404() throws Exception {
		when(customerService.searchByIdNo("111111111111")).thenThrow(new ResourceNotFoundException("Customer not found"));

		mockMvc.perform(post("/api/customers/search").contentType(MediaType.APPLICATION_JSON)
				.content("{\"idNo\":\"111111111111\"}"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.errors[0].errorCode").value("RESOURCE_NOT_FOUND"));
	}

	@Test
	void searchByIdNo_invalidFormat_returns400() throws Exception {
		mockMvc.perform(post("/api/customers/search").contentType(MediaType.APPLICATION_JSON)
				.content("{\"idNo\":\"900101-01-5555\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[0].errorCode").value("VALIDATION_FAILED"));
	}

	@Test
	void unexpectedError_returnsGeneric500WithoutDetails() throws Exception {
		when(customerService.getById(1L)).thenThrow(new IllegalStateException("ORA-00942 secret table detail"));

		mockMvc.perform(get("/api/customers/1"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.errors[0].errorCode").value("INTERNAL_ERROR"))
				.andExpect(jsonPath("$.errors[0].errorMessage").value("Unexpected error occurred"));
	}
}
