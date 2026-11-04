package com.bank.document_management.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bank.document_management.entity.DocumentHistory;

@Repository
public interface DocumentHistoryRepository extends JpaRepository<DocumentHistory, Long> {

	List<DocumentHistory> findByDocumentIdOrderByUpdatedDateAscIdAsc(Long documentId);
}
