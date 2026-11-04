package com.bank.document_management.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bank.document_management.entity.Customer;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

	boolean existsByIdentificationNo(String identificationNo);

	Optional<Customer> findByIdentificationNo(String identificationNo);

	boolean existsByEmail(String email);
}
