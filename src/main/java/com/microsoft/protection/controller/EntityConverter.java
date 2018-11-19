/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */
package com.microsoft.protection.controller;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import org.springframework.hateoas.Link;

import com.microsoft.protection.controller.model.ProtectionRequestGet;
import com.microsoft.protection.data.AzureStorageRepository;
import com.microsoft.protection.data.model.ProtectionRequest;
import com.microsoft.protection.data.model.ProtectionRequest.Status;

public class EntityConverter {
    static ProtectionRequestGet toProtectionRequestGetWithSelfLink(final ProtectionRequest entity,
            final AzureStorageRepository azureStorageRepository) {

        final ProtectionRequestGet response = toProtectionRequestGet(entity, azureStorageRepository);

        response.add(linkTo(methodOn(ProtectionRequestController.class).getRequest(entity.getId())).withSelfRel());

        return response;
    }

    static ProtectionRequestGet toProtectionRequestGet(final ProtectionRequest entity,
            final AzureStorageRepository azureStorageRepository) {

        final ProtectionRequestGet response = new ProtectionRequestGet(entity.getUrl(), entity.getUser(),
                entity.getCorrelationId(), entity.getRightsAsString(), entity.getId(), entity.getStatus().toString(),
                entity.getStatusReason(), entity.getFileName(), entity.getContentType(), entity.getSize(),
                entity.getValidUntil());

        if (Status.COMPLETE == entity.getStatus()) {
            response.add(new Link(azureStorageRepository.getUri(entity.getId(), entity.getFileName()).toString(),
                    "download"));
        }

        return response;
    }
}
