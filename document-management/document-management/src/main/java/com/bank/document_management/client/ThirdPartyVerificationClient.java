package com.bank.document_management.client;

import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.bank.document_management.config.VerificationProperties;
import com.bank.document_management.dto.external.ThirdPartyVerifyRequest;
import com.bank.document_management.dto.external.ThirdPartyVerifyResponse;
import com.bank.document_management.enums.VerificationResult;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ThirdPartyVerificationClient {

	private final RestClient restClient;

	public ThirdPartyVerificationClient(VerificationProperties properties) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(properties.timeout());
		requestFactory.setReadTimeout(properties.timeout());
		this.restClient = RestClient.builder()
				.baseUrl(properties.baseUrl())
				.requestFactory(requestFactory)
				.build();
	}

	/** Never throws: a failed or unusable 3rd-party call is reported as UNABLE_TO_VERIFY. */
	public VerificationResult verify(Long documentId, String documentType, String fileName) {
		try {
			ThirdPartyVerifyResponse response = restClient.post()
					.uri("/verify")
					.contentType(MediaType.APPLICATION_JSON)
					.body(new ThirdPartyVerifyRequest(documentId, documentType, fileName))
					.retrieve()
					.body(ThirdPartyVerifyResponse.class);
			return response != null && response.verified()
					? VerificationResult.VERIFIED
					: VerificationResult.UNABLE_TO_VERIFY;
		} catch (RestClientException e) {
			log.warn("3rd-party verification failed for document {}: {}", documentId, e.getMessage());
			return VerificationResult.UNABLE_TO_VERIFY;
		}
	}
}
