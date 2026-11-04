package com.bank.document_management.mapper;

import org.springframework.stereotype.Component;

import com.bank.document_management.dto.request.CustomerRequest;
import com.bank.document_management.dto.response.CustomerResponse;
import com.bank.document_management.entity.Customer;
import com.bank.document_management.util.DateTimeUtil;

@Component
public class CustomerMapper {

	public Customer toEntity(CustomerRequest request) {
		Customer customer = new Customer();
		customer.setFullName(request.fullName());
		customer.setIdentificationNo(request.idNo());
		customer.setEmail(request.emailAddress());
		return customer;
	}

	public CustomerResponse toResponse(Customer customer) {
		return new CustomerResponse(
				customer.getId(),
				customer.getFullName(),
				customer.getIdentificationNo(),
				customer.getEmail(),
				DateTimeUtil.toOffset(customer.getCreatedDate()));
	}
}
