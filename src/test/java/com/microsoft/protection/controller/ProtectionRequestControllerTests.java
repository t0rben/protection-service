/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */
package com.microsoft.protection.controller;

import java.util.Set;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.microsoft.protection.controller.model.ProtectionRequestPost;
import com.microsoft.protection.data.ProtectionRequestRepository;
import com.microsoft.protection.data.model.ProtectionRequest;
import com.microsoft.protection.data.model.ProtectionRequest.Right;
import com.microsoft.protection.data.model.ProtectionRequest.Status;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:/test.yml")
@Slf4j
public class ProtectionRequestControllerTests {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ProtectionRequestRepository protectionRequestRepository;

    @Test
    public void createProtectionRequest() {
        final ProtectionRequestPost test = new ProtectionRequestPost("htts://download.here/filename.pdf",
                "user@contoso.com", UUID.randomUUID().toString(), "READ", "filename.pdf");

        webTestClient.post().uri("/v1/protection").contentType(MediaType.APPLICATION_JSON_UTF8)
                .accept(MediaType.APPLICATION_JSON_UTF8).body(Mono.just(test), ProtectionRequestPost.class).exchange()
                .expectStatus().isOk().expectHeader().contentType(MediaType.APPLICATION_JSON_UTF8).expectBody()
                .jsonPath("$.id").isNotEmpty().jsonPath("$.correlationId").isEqualTo(test.getCorrelationId())
                .jsonPath("$.url").isEqualTo(test.getUrl()).jsonPath("$.user").isEqualTo(test.getUser())
                .jsonPath("$.status").isEqualTo(Status.PROCESSING.toString());
    }

    @Test
    public void getProtectionRequest() {
        final ProtectionRequest test = new ProtectionRequest();
        test.setCorrelationId(UUID.randomUUID().toString());
        test.setRights(Set.of(Right.READ));
        test.setUrl("htts://download.here");
        test.setUser("user@contoso.com");

        protectionRequestRepository.save(test);

        webTestClient.get().uri("/v1/protection").accept(MediaType.APPLICATION_JSON_UTF8).exchange().expectStatus()
                .isOk().expectHeader().contentType(MediaType.APPLICATION_JSON_UTF8).expectBody().jsonPath("[0].id")
                .isNotEmpty().jsonPath("[0].correlationId").isEqualTo(test.getCorrelationId()).jsonPath("[0].url")
                .isEqualTo(test.getUrl()).jsonPath("[0].user").isEqualTo(test.getUser()).jsonPath("[0].status")
                .isEqualTo(Status.PROCESSING.toString());

    }
}
