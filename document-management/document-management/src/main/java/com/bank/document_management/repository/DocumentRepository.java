package com.bank.document_management.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.bank.document_management.entity.Document;
import com.bank.document_management.enums.DocumentStatus;

import jakarta.persistence.LockModeType;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long>, JpaSpecificationExecutor<Document> {

	/** Row lock so two officers cannot approve/reject the same document at once. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select d from Document d where d.id = :id")
	Optional<Document> findByIdForUpdate(@Param("id") Long id);

	Page<Document> findByVerificationStatus(DocumentStatus status, Pageable pageable);

	Page<Document> findByCustomerId(Long customerId, Pageable pageable);
}
