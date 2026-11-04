package com.bank.document_management.service;

import com.bank.document_management.dto.request.CustomerRequest;
import com.bank.document_management.dto.response.CustomerResponse;

public interface CustomerService {

	CustomerResponse register(CustomerRequest request);

	CustomerResponse getById(Long customerId);

	CustomerResponse searchByIdNo(String idNo);
}
