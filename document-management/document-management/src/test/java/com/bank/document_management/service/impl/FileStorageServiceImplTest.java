package com.bank.document_management.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import com.bank.document_management.config.StorageProperties;
import com.bank.document_management.exception.InvalidFileException;
import com.bank.document_management.service.StoredFile;

class FileStorageServiceImplTest {

	private static final long MAX_SIZE = 1024;
	private static final byte[] PDF = "%PDF-1.4 sample".getBytes(StandardCharsets.US_ASCII);
	private static final byte[] PNG = { (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00 };
	private static final byte[] JPG = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00 };

	@TempDir
	Path uploadDir;

	private FileStorageServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new FileStorageServiceImpl(new StorageProperties(uploadDir.toString(), MAX_SIZE));
	}

	private static MockMultipartFile file(String name, byte[] content) {
		return new MockMultipartFile("file", name, "application/octet-stream", content);
	}

	@Test
	void store_validPdf_savesFileWithServerGeneratedName() {
		StoredFile stored = service.store(file("nric.pdf", PDF));

		assertThat(stored.contentType()).isEqualTo("application/pdf");
		assertThat(stored.originalFileName()).isEqualTo("nric.pdf");
		assertThat(stored.storedFileName()).endsWith(".pdf").isNotEqualTo("nric.pdf");
		assertThat(Files.exists(uploadDir.resolve(stored.relativePath()))).isTrue();
	}

	@Test
	void store_validPng_isAccepted() {
		assertThat(service.store(file("photo.png", PNG)).contentType()).isEqualTo("image/png");
	}

	@Test
	void store_validJpegWithJpegExtension_isAccepted() {
		assertThat(service.store(file("photo.jpeg", JPG)).contentType()).isEqualTo("image/jpeg");
	}

	@Test
	void store_textFileRenamedToPdf_isRejected() {
		MockMultipartFile fake = file("fake.pdf", "just text".getBytes(StandardCharsets.US_ASCII));

		assertThatThrownBy(() -> service.store(fake)).isInstanceOf(InvalidFileException.class)
				.hasMessageContaining("only PDF, JPG and PNG");
	}

	@Test
	void store_extensionDoesNotMatchContent_isRejected() {
		assertThatThrownBy(() -> service.store(file("nric.png", PDF))).isInstanceOf(InvalidFileException.class)
				.hasMessageContaining("extension");
	}

	@Test
	void store_doubleExtension_isRejected() {
		assertThatThrownBy(() -> service.store(file("nric.pdf.exe", PDF))).isInstanceOf(InvalidFileException.class);
	}

	@Test
	void store_emptyFile_isRejected() {
		assertThatThrownBy(() -> service.store(file("nric.pdf", new byte[0]))).isInstanceOf(InvalidFileException.class);
	}

	@Test
	void store_nullFile_isRejected() {
		assertThatThrownBy(() -> service.store(null)).isInstanceOf(InvalidFileException.class);
	}

	@Test
	void store_fileOverLimit_isRejectedWith413() {
		byte[] big = new byte[(int) MAX_SIZE + 1];
		System.arraycopy(PDF, 0, big, 0, PDF.length);

		assertThatThrownBy(() -> service.store(file("big.pdf", big)))
				.isInstanceOfSatisfying(InvalidFileException.class,
						ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.valueOf(413)));
	}

	@Test
	void store_pathTraversalInFileName_isStrippedAndStaysInsideUploadDir() {
		StoredFile stored = service.store(file("../../evil.pdf", PDF));

		assertThat(stored.originalFileName()).isEqualTo("evil.pdf");
		Path target = uploadDir.resolve(stored.relativePath()).normalize();
		assertThat(target).startsWith(uploadDir);
		assertThat(Files.exists(target)).isTrue();
	}

	@Test
	void delete_removesStoredFile() {
		StoredFile stored = service.store(file("nric.pdf", PDF));

		service.delete(stored.relativePath());

		assertThat(Files.exists(uploadDir.resolve(stored.relativePath()))).isFalse();
	}

	@Test
	void delete_pathOutsideUploadDir_isRejected() {
		assertThatThrownBy(() -> service.delete("../outside.pdf")).isInstanceOf(InvalidFileException.class);
	}
}
