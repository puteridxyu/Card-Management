package com.bank.document_management.controller;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bank.document_management.dto.request.CustomerRequest;
import com.bank.document_management.dto.request.CustomerSearchRequest;
import com.bank.document_management.dto.response.ApiResponse;
import com.bank.document_management.dto.response.CustomerResponse;
import com.bank.document_management.service.CustomerService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

	private final CustomerService customerService;

	@PostMapping
	public ResponseEntity<ApiResponse<CustomerResponse>> register(
			@Valid @RequestBody CustomerRequest request, HttpServletRequest httpRequest) {
		CustomerResponse customer = customerService.register(request);
		URI location = URI.create(httpRequest.getRequestURI() + "/" + customer.customerId());
		return ResponseEntity.created(location)
				.body(ApiResponse.success(HttpStatus.CREATED, httpRequest.getRequestURI(), customer));
	}

	@GetMapping("/{customerId}")
	public ResponseEntity<ApiResponse<CustomerResponse>> getById(
			@PathVariable Long customerId, HttpServletRequest httpRequest) {
		CustomerResponse customer = customerService.getById(customerId);
		return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, httpRequest.getRequestURI(), customer));
	}

	@PostMapping("/search")
	public ResponseEntity<ApiResponse<CustomerResponse>> searchByIdNo(
			@Valid @RequestBody CustomerSearchRequest request, HttpServletRequest httpRequest) {
		CustomerResponse customer = customerService.searchByIdNo(request.idNo());
		return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, httpRequest.getRequestURI(), customer));
	}
}
