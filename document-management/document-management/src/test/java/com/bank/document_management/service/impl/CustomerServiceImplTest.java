package com.bank.document_management.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bank.document_management.dto.request.CustomerRequest;
import com.bank.document_management.dto.response.CustomerResponse;
import com.bank.document_management.entity.Customer;
import com.bank.document_management.exception.DuplicateResourceException;
import com.bank.document_management.exception.ResourceNotFoundException;
import com.bank.document_management.mapper.CustomerMapper;
import com.bank.document_management.repository.CustomerRepository;

@ExtendWith(MockitoExtension.class)
class CustomerServiceImplTest {

	private static final String ID_NO = "900101015555";
	private static final String EMAIL = "ali@example.com";

	@Mock
	private CustomerRepository customerRepository;

	private CustomerServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new CustomerServiceImpl(customerRepository, new CustomerMapper());
	}

	@Test
	void register_savesCustomerAndReturnsResponse() {
		CustomerRequest request = new CustomerRequest("Ali Bin Abu", ID_NO, "Ali@Example.com");
		when(customerRepository.existsByIdentificationNo(ID_NO)).thenReturn(false);
		when(customerRepository.existsByEmail(EMAIL)).thenReturn(false);
		when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> {
			Customer saved = invocation.getArgument(0);
			saved.setId(4L);
			saved.setCreatedDate(LocalDateTime.of(2026, 11, 4, 11, 48));
			return saved;
		});

		CustomerResponse response = service.register(request);

		assertThat(response.customerId()).isEqualTo(4L);
		assertThat(response.fullName()).isEqualTo("Ali Bin Abu");
		assertThat(response.idNo()).isEqualTo(ID_NO);
		assertThat(response.emailAddress()).isEqualTo(EMAIL);
		assertThat(response.createdDate()).isNotNull();
	}

	@Test
	void register_duplicateIdNo_throwsAndDoesNotSave() {
		when(customerRepository.existsByIdentificationNo(ID_NO)).thenReturn(true);

		assertThatThrownBy(() -> service.register(new CustomerRequest("Ali", ID_NO, EMAIL)))
				.isInstanceOf(DuplicateResourceException.class);

		verify(customerRepository, never()).save(any());
	}

	@Test
	void register_duplicateEmail_throwsAndDoesNotSave() {
		when(customerRepository.existsByIdentificationNo(ID_NO)).thenReturn(false);
		when(customerRepository.existsByEmail(EMAIL)).thenReturn(true);

		assertThatThrownBy(() -> service.register(new CustomerRequest("Ali", ID_NO, EMAIL)))
				.isInstanceOf(DuplicateResourceException.class);

		verify(customerRepository, never()).save(any());
	}

	@Test
	void getById_existingCustomer_returnsResponse() {
		Customer customer = new Customer();
		customer.setId(4L);
		customer.setFullName("Ali Bin Abu");
		customer.setIdentificationNo(ID_NO);
		customer.setEmail(EMAIL);
		when(customerRepository.findById(4L)).thenReturn(Optional.of(customer));

		CustomerResponse response = service.getById(4L);

		assertThat(response.customerId()).isEqualTo(4L);
		assertThat(response.emailAddress()).isEqualTo(EMAIL);
	}

	@Test
	void getById_unknownCustomer_throwsNotFound() {
		when(customerRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getById(99L)).isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void searchByIdNo_existingCustomer_returnsResponse() {
		Customer customer = new Customer();
		customer.setId(4L);
		customer.setFullName("Ali Bin Abu");
		customer.setIdentificationNo(ID_NO);
		customer.setEmail(EMAIL);
		when(customerRepository.findByIdentificationNo(ID_NO)).thenReturn(Optional.of(customer));

		CustomerResponse response = service.searchByIdNo(ID_NO);

		assertThat(response.customerId()).isEqualTo(4L);
		assertThat(response.idNo()).isEqualTo(ID_NO);
	}

	@Test
	void searchByIdNo_unknownIdNo_throwsNotFoundWithoutEchoingInput() {
		when(customerRepository.findByIdentificationNo(ID_NO)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.searchByIdNo(ID_NO))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageNotContaining(ID_NO);
	}
}
