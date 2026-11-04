package com.bank.document_management.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bank.document_management.dto.request.CustomerRequest;
import com.bank.document_management.dto.response.CustomerResponse;
import com.bank.document_management.entity.Customer;
import com.bank.document_management.exception.DuplicateResourceException;
import com.bank.document_management.exception.ResourceNotFoundException;
import com.bank.document_management.mapper.CustomerMapper;
import com.bank.document_management.repository.CustomerRepository;
import com.bank.document_management.service.CustomerService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

	private final CustomerRepository customerRepository;
	private final CustomerMapper customerMapper;

	@Override
	@Transactional
	public CustomerResponse register(CustomerRequest request) {
		if (customerRepository.existsByIdentificationNo(request.idNo())) {
			throw new DuplicateResourceException("Customer with the given idNo already exists");
		}
		if (customerRepository.existsByEmail(request.emailAddress())) {
			throw new DuplicateResourceException("Customer with the given emailAddress already exists");
		}
		Customer saved = customerRepository.save(customerMapper.toEntity(request));
		return customerMapper.toResponse(saved);
	}

	@Override
	@Transactional(readOnly = true)
	public CustomerResponse getById(Long customerId) {
		return customerRepository.findById(customerId)
				.map(customerMapper::toResponse)
				.orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerId));
	}

	@Override
	@Transactional(readOnly = true)
	public CustomerResponse searchByIdNo(String idNo) {
		return customerRepository.findByIdentificationNo(idNo)
				.map(customerMapper::toResponse)
				.orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
	}
}
