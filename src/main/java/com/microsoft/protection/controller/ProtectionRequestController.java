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
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.microsoft.azure.storage.StorageException;
import com.microsoft.protection.controller.model.ProtectionRequestGet;
import com.microsoft.protection.controller.model.ProtectionRequestPost;
import com.microsoft.protection.controller.model.ResponseList;
import com.microsoft.protection.data.AzureStorageRepository;
import com.microsoft.protection.data.ProtectionRequestRepository;
import com.microsoft.protection.data.model.ProtectionRequest;
import com.microsoft.protection.data.model.ProtectionRequest.Status;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/protection")
@Slf4j
public class ProtectionRequestController {
    private static final int LIMIT = 500;

    private final ProtectionRequestRepository protectionRequestRepository;

    private final MipHandler mipHandler;

    private final AzureStorageRepository azureStorageRepository;

    // TODO limit and filter by parameter
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<ProtectionRequestGet> getAllRequests() {
        return new ResponseList<>(protectionRequestRepository.findAll(PageRequest.of(0, LIMIT)).stream()
                .map(this::toProtectionRequestGet).collect(Collectors.toList()));
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
        toCreate.setContentType(file.getContentType());
        toCreate.setSize(file.getSize());

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
        toCreate.setContentType(request.getContentType());
        toCreate.setSize(request.getSize());

        final ProtectionRequest stored = protectionRequestRepository.save(toCreate);
        mipHandler.protect(stored);

        return toProtectionRequestGet(stored);
    }

    @GetMapping(value = "/{id}", produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity<ProtectionRequestGet> getRequest(@PathVariable final String id) {
        return protectionRequestRepository.findById(id).map(entity -> ResponseEntity.ok(toProtectionRequestGet(entity)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteRequest(@PathVariable final String id) {

        final ProtectionRequest entity = protectionRequestRepository.findById(id).orElseThrow();
        try {
            azureStorageRepository.delete(id, entity.getFileName());
        } catch (StorageException | URISyntaxException e) {
            log.error("Could not delete file {}", entity, e);
        }

        // TODO miphandler -> invalidate
        protectionRequestRepository.delete(entity);

    }

    private ProtectionRequestGet toProtectionRequestGet(final ProtectionRequest entity) {

        final ProtectionRequestGet response = new ProtectionRequestGet(entity.getUrl(), entity.getUser(),
                entity.getCorrelationId(), entity.getRightsAsString(), entity.getId(), entity.getStatus().toString(),
                entity.getStatusReason(), entity.getFileName(), entity.getContentType(), entity.getSize());

        if (Status.COMPLETE == entity.getStatus()) {
            try {
                response.add(new Link(azureStorageRepository.getUri(entity.getId(), entity.getFileName()).toString(),
                        "download"));
            } catch (StorageException | URISyntaxException e) {
                log.error("Could not generate download link", e);
            }
        }

        response.add(linkTo(methodOn(ProtectionRequestController.class).getRequest(entity.getId())).withSelfRel());

        return response;
    }
}
