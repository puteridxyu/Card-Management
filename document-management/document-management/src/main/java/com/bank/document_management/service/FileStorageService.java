package com.bank.document_management.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

	/** Validates the file content (PDF, JPG, PNG) and size, then writes it under the upload directory. */
	StoredFile store(MultipartFile file);

	/** Returns a stored file for download; throws ResourceNotFoundException if it is missing on disk. */
	Resource load(String relativePath);

	/** Removes a previously stored file; never throws. */
	void delete(String relativePath);
}
