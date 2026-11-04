package com.bank.document_management.controller;

import java.util.Locale;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bank.document_management.dto.external.ThirdPartyVerifyRequest;
import com.bank.document_management.dto.external.ThirdPartyVerifyResponse;

/**
 * Stand-in for the external verification provider so the flow can be tested locally.
 * Rule: a file name containing "fake" or "invalid" cannot be verified; anything else is verified.
 * Point {@code app.verification.base-url} at the real provider to replace it.
 */
@RestController
@RequestMapping("/mock/third-party")
public class MockVerificationController {

	@PostMapping("/verify")
	public ThirdPartyVerifyResponse verify(@RequestBody ThirdPartyVerifyRequest request) {
		String name = request.fileName() == null ? "" : request.fileName().toLowerCase(Locale.ROOT);
		boolean verified = !(name.contains("fake") || name.contains("invalid"));
		return new ThirdPartyVerifyResponse(verified);
	}
}
