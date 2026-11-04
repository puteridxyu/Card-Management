package com.bank.document_management.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.bank.document_management.config.StorageProperties;
import com.bank.document_management.exception.FileStorageException;
import com.bank.document_management.exception.InvalidFileException;
import com.bank.document_management.exception.ResourceNotFoundException;
import com.bank.document_management.service.FileStorageService;
import com.bank.document_management.service.StoredFile;
import com.bank.document_management.util.DateTimeUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

	private static final int HEADER_LENGTH = 8;
	private static final int MAX_FILE_NAME_LENGTH = 255;

	private final StorageProperties properties;

	private enum FileKind {
		PDF("application/pdf", "pdf", Set.of("pdf"), new int[] { 0x25, 0x50, 0x44, 0x46, 0x2D }),
		JPEG("image/jpeg", "jpg", Set.of("jpg", "jpeg"), new int[] { 0xFF, 0xD8, 0xFF }),
		PNG("image/png", "png", Set.of("png"), new int[] { 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A });

		private final String contentType;
		private final String extension;
		private final Set<String> allowedExtensions;
		private final int[] signature;

		FileKind(String contentType, String extension, Set<String> allowedExtensions, int[] signature) {
			this.contentType = contentType;
			this.extension = extension;
			this.allowedExtensions = allowedExtensions;
			this.signature = signature;
		}

		boolean matches(byte[] header) {
			if (header.length < signature.length) {
				return false;
			}
			for (int i = 0; i < signature.length; i++) {
				if ((header[i] & 0xFF) != signature[i]) {
					return false;
				}
			}
			return true;
		}
	}

	@Override
	public StoredFile store(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new InvalidFileException("file is required and must not be empty");
		}
		if (file.getSize() > properties.maxFileSizeBytes()) {
			throw new InvalidFileException(HttpStatus.valueOf(413),
					"file exceeds the maximum size of " + properties.maxFileSizeBytes() + " bytes");
		}

		String originalName = sanitizeFileName(file.getOriginalFilename());
		FileKind kind = detectKind(file);
		String extension = StringUtils.getFilenameExtension(originalName);
		if (extension == null || !kind.allowedExtensions.contains(extension.toLowerCase(Locale.ROOT))) {
			throw new InvalidFileException("file extension does not match the file content");
		}

		String storedName = UUID.randomUUID() + "." + kind.extension;
		String relativePath = DateTimeUtil.now().getYear() + "/" + storedName;
		Path target = resolve(relativePath);

		try (InputStream in = file.getInputStream()) {
			Files.createDirectories(target.getParent());
			Files.copy(in, target);
		} catch (IOException e) {
			deleteQuietly(target);
			throw new FileStorageException("Unable to store the file", e);
		}
		log.info("Stored file {}", storedName);
		return new StoredFile(originalName, storedName, relativePath, kind.contentType, file.getSize());
	}

	@Override
	public Resource load(String relativePath) {
		Path target = resolve(relativePath);
		if (!Files.isRegularFile(target) || !Files.isReadable(target)) {
			throw new ResourceNotFoundException("Stored file not found");
		}
		return new FileSystemResource(target);
	}

	@Override
	public void delete(String relativePath) {
		deleteQuietly(resolve(relativePath));
	}

	private FileKind detectKind(MultipartFile file) {
		byte[] header;
		try (InputStream in = file.getInputStream()) {
			header = in.readNBytes(HEADER_LENGTH);
		} catch (IOException e) {
			throw new FileStorageException("Unable to read the uploaded file", e);
		}
		return Arrays.stream(FileKind.values())
				.filter(kind -> kind.matches(header))
				.findFirst()
				.orElseThrow(() -> new InvalidFileException("only PDF, JPG and PNG files are allowed"));
	}

	/** Strips any client-supplied path and control characters; the result is only used as display metadata. */
	private String sanitizeFileName(String rawName) {
		String base = rawName == null ? null : StringUtils.getFilename(StringUtils.cleanPath(rawName));
		String name = base == null ? "" : base.replaceAll("\\p{Cntrl}", "");
		if (!StringUtils.hasText(name)) {
			throw new InvalidFileException("file name is required");
		}
		return name.length() > MAX_FILE_NAME_LENGTH ? name.substring(name.length() - MAX_FILE_NAME_LENGTH) : name;
	}

	private Path root() {
		return Paths.get(properties.uploadDir()).toAbsolutePath().normalize();
	}

	private Path resolve(String relativePath) {
		Path root = root();
		Path target = root.resolve(relativePath).normalize();
		if (!target.startsWith(root)) {
			throw new InvalidFileException("invalid file path");
		}
		return target;
	}

	private void deleteQuietly(Path path) {
		try {
			Files.deleteIfExists(path);
		} catch (IOException e) {
			log.warn("Unable to delete file {}", path.getFileName(), e);
		}
	}
}
