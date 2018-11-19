/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */
package com.microsoft.protection.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.cloud.stream.test.binder.MessageCollector;
import org.springframework.messaging.Message;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.protection.controller.model.ProtectionRequestGet;
import com.microsoft.protection.data.model.ProtectionRequest;

public class ProtectionPublisherTest extends AbstractTest {

    @Autowired
    private Source source;

    @Autowired
    private MessageCollector messageCollector;

    @Autowired
    private ProtectionPublisher protectionPublisher;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testOrderComplete() throws JsonParseException, JsonMappingException, IOException {
        final ProtectionRequest test = storeTestRequest();

        protectionPublisher.orderComplete(test);

        final Message<String> received = (Message<String>) messageCollector.forChannel(source.output()).poll();

        final ProtectionRequestGet payload = objectMapper.readValue(received.getPayload(), ProtectionRequestGet.class);

        assertThat(payload.getCorrelationId()).isEqualTo(test.getCorrelationId());
        assertThat(payload.getUrl()).isEqualTo(test.getUrl());
        assertThat(payload.getUser()).isEqualTo(test.getUser());
        assertThat(payload.getFileName()).isEqualTo(test.getFileName());
        assertThat(payload.getContentType()).isEqualTo(test.getContentType());
        assertThat(payload.getSize()).isEqualTo(test.getSize());
        assertThat(payload.getRights()).isEqualTo(test.getRightsAsString());

    }

}
