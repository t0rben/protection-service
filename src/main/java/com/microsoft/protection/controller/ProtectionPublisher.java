/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */
package com.microsoft.protection.controller;

import org.springframework.cloud.stream.messaging.Source;
import org.springframework.messaging.support.MessageBuilder;

import com.microsoft.protection.data.AzureStorageRepository;
import com.microsoft.protection.data.model.ProtectionRequest;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ProtectionPublisher {

    private final Source source;
    private final AzureStorageRepository azureStorageRepository;

    public void orderComplete(final ProtectionRequest entity) {
        source.output().send(MessageBuilder
                .withPayload(EntityConverter.toProtectionRequestGet(entity, azureStorageRepository)).build());
    }

}
