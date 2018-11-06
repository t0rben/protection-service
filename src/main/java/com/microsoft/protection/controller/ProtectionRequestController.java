/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */
package com.microsoft.protection.controller;

import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.google.common.base.Joiner;
import com.microsoft.protection.controller.model.ProtectionRequestGet;
import com.microsoft.protection.controller.model.ProtectionRequestPost;
import com.microsoft.protection.data.ProtectionRequestRepository;
import com.microsoft.protection.data.model.ProtectionRequest;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/protection")
public class ProtectionRequestController {
    private static final Joiner JOINER = Joiner.on(",").skipNulls();

    private final ProtectionRequestRepository protectionRequestRepository;

    private final MipHandler mipHandler;

    @GetMapping
    public Flux<ProtectionRequest> getAllRequests() {
        return protectionRequestRepository.findAll();
    }

    @PostMapping("/upload")
    public Mono<ResponseEntity<ProtectionRequestGet>> createRequestWithUpload(
            @RequestParam("file") final MultipartFile file,
            @RequestParam(name = "rights", required = false) final String rights,
            @RequestParam(name = "correlationId", required = false) final String correlationId,
            @RequestParam(name = "user") final String user) {
        final ProtectionRequest toCreate = new ProtectionRequest();
        toCreate.setCorrelationId(correlationId);
        toCreate.setRightsAsString(rights);
        toCreate.setUser(user);
        toCreate.setFileName(file.getOriginalFilename());

        return protectionRequestRepository.save(toCreate).doOnNext(stored -> mipHandler.protect(stored, file))
                .map(entity -> ResponseEntity.ok(toProtectionRequestGet(entity)));
    }

    @PostMapping
    public Mono<ResponseEntity<ProtectionRequestGet>> createRequest(
            @Valid @RequestBody final ProtectionRequestPost request) {
        final ProtectionRequest toCreate = new ProtectionRequest();
        toCreate.setCorrelationId(request.getCorrelationId());
        toCreate.setRightsAsString(request.getRights());
        toCreate.setUrl(request.getUrl());
        toCreate.setUser(request.getUser());
        toCreate.setFileName(request.getFileName());

        return protectionRequestRepository.save(toCreate).doOnNext(mipHandler::protect)
                .map(entity -> ResponseEntity.ok(toProtectionRequestGet(entity)));
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<ProtectionRequestGet>> getRequest(@PathVariable final String id) {
        return protectionRequestRepository.findById(id).map(entity -> ResponseEntity.ok(toProtectionRequestGet(entity)))
                .defaultIfEmpty(ResponseEntity.status(404).body(null));
    }

    private static ProtectionRequestGet toProtectionRequestGet(final ProtectionRequest entity) {
        return new ProtectionRequestGet(entity.getUrl(), entity.getUser(), entity.getCorrelationId(),
                JOINER.join(entity.getRights()), entity.getId(), entity.getStatus().toString(),
                entity.getStatusReason(), entity.getFileName());
    }
}
