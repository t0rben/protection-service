/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */
package com.microsoft.protection.controller;

import java.util.Set;
import java.util.UUID;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import com.microsoft.protection.ProtectionServiceConfiguration;
import com.microsoft.protection.data.ProtectionRequestRepository;
import com.microsoft.protection.data.model.ProtectionRequest;
import com.microsoft.protection.data.model.ProtectionRequest.Right;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = { ProtectionServiceConfiguration.class })
@TestPropertySource(locations = "classpath:/test.yml")
public abstract class AbstractTest {

    @Autowired
    protected ProtectionRequestRepository protectionRequestRepository;

    @Before
    public void cleanup() {
        protectionRequestRepository.deleteAll();
    }

    protected ProtectionRequest storeTestRequest() {
        final ProtectionRequest testStored = new ProtectionRequest();
        testStored.setCorrelationId(UUID.randomUUID().toString());
        testStored.setRights(Set.of(Right.READ));
        testStored.setUrl("https://download.here");
        testStored.setUser("user@contoso.com");
        testStored.setFileName("filename.pdf");
        testStored.setContentType("application/pdf");
        testStored.setSize(9L);

        return protectionRequestRepository.save(testStored);
    }

}
