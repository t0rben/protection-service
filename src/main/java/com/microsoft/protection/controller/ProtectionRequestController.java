/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */
package com.microsoft.protection.controller;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.springframework.data.domain.PageRequest;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.google.common.base.Joiner;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.protection.controller.model.ProtectionRequestGet;
import com.microsoft.protection.controller.model.ProtectionRequestPost;
import com.microsoft.protection.data.AzureStorageRepository;
import com.microsoft.protection.data.ProtectionRequestRepository;
import com.microsoft.protection.data.model.ProtectionRequest;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/protection")
public class ProtectionRequestController {
    private static final Joiner JOINER = Joiner.on(",").skipNulls();

    private final ProtectionRequestRepository protectionRequestRepository;

    private final MipHandler mipHandler;

    private final AzureStorageRepository azureStorageRepository;

    // TODO limit and filter by parameter
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<ProtectionRequestGet> getAllRequests() {
        return protectionRequestRepository.findAll(PageRequest.of(0, 500)).stream()
                .map(entity -> toProtectionRequestGet(entity)).collect(Collectors.toList());
    }

    @PostMapping("/upload")
    @ResponseStatus(HttpStatus.CREATED)
    public ProtectionRequestGet createRequestWithUpload(@RequestParam("file") final MultipartFile file,
            @RequestParam(name = "rights", required = false) final String rights,
            @RequestParam(name = "correlationId", required = false) final String correlationId,
            @RequestParam(name = "user") final String user) {
        final ProtectionRequest toCreate = new ProtectionRequest();
        toCreate.setCorrelationId(correlationId);
        toCreate.setRightsAsString(rights);
        toCreate.setUser(user);
        toCreate.setFileName(file.getOriginalFilename());

        final ProtectionRequest stored = protectionRequestRepository.save(toCreate);
        mipHandler.protect(stored, file);

        return toProtectionRequestGet(stored);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProtectionRequestGet createRequest(@Valid @RequestBody final ProtectionRequestPost request) {
        final ProtectionRequest toCreate = new ProtectionRequest();
        toCreate.setCorrelationId(request.getCorrelationId());
        toCreate.setRightsAsString(request.getRights());
        toCreate.setUrl(request.getUrl());
        toCreate.setUser(request.getUser());
        toCreate.setFileName(request.getFileName());

        final ProtectionRequest stored = protectionRequestRepository.save(toCreate);
        mipHandler.protect(stored);

        return toProtectionRequestGet(stored);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProtectionRequestGet> getRequest(@PathVariable final String id) {
        return protectionRequestRepository.findById(id).map(entity -> ResponseEntity.ok(toProtectionRequestGet(entity)))
                .orElse(ResponseEntity.status(404).body(null));
    }

    private ProtectionRequestGet toProtectionRequestGet(final ProtectionRequest entity) {

        final ProtectionRequestGet response = new ProtectionRequestGet(entity.getUrl(), entity.getUser(),
                entity.getCorrelationId(), JOINER.join(entity.getRights()), entity.getId(),
                entity.getStatus().toString(), entity.getStatusReason(), entity.getFileName());

        if (ProtectionRequest.Status.COMPLETE.equals(entity.getStatus())) {
            try {
                response.add(new Link(azureStorageRepository.getUri(entity.getId(), entity.getFileName()).toString(),
                        "download"));
            } catch (StorageException | URISyntaxException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        response.add(linkTo(methodOn(ProtectionRequestController.class).getRequest(entity.getId())).withSelfRel());

        return response;
    }
}
