package com.bank.document_management.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.bank.document_management.enums.DocumentStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "DOCUMENT")
@Getter
@Setter
@NoArgsConstructor
public class Document {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "document_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "customer_id", nullable = false)
	private Customer customer;

	@Column(name = "document_type", nullable = false, length = 30)
	private String documentType;

	@Column(name = "file_name", nullable = false, length = 255)
	private String fileName;

	@Column(name = "stored_file_name", nullable = false, length = 100, unique = true)
	private String storedFileName;

	@Column(name = "file_path", nullable = false, length = 500)
	private String filePath;

	@Column(name = "content_type", nullable = false, length = 100)
	private String contentType;

	@Column(name = "file_size", nullable = false)
	private Long fileSize;

	@Enumerated(EnumType.STRING)
	@Column(name = "verification_status", nullable = false, length = 20)
	private DocumentStatus verificationStatus = DocumentStatus.PENDING;

	@CreationTimestamp
	@Column(name = "uploaded_date", nullable = false, updatable = false)
	private LocalDateTime uploadedDate;

	@UpdateTimestamp
	@Column(name = "updated_date", nullable = false)
	private LocalDateTime updatedDate;
}
