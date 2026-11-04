package com.bank.document_management.service;

import org.springframework.core.io.Resource;

public record DocumentContent(Resource resource, String fileName, String contentType) {
}
