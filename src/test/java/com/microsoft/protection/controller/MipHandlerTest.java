/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */
package com.microsoft.protection.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Optional;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import com.google.common.io.Files;
import com.microsoft.protection.data.AzureStorageRepository;
import com.microsoft.protection.data.model.ProtectionRequest;
import com.microsoft.protection.mip.MipSdkCaller;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:/test.yml")
public class MipHandlerTest extends AbstractTest {
    @MockBean
    private AzureStorageRepository azureStorageRepository;

    @MockBean
    private AadHandler aadHandler;

    @MockBean
    private MipSdkCaller mipSdkCaller;

    @Autowired
    private MipHandler mipHandler;

    @LocalServerPort
    private int randomServerPort;

    private ProtectionRequest test;
    private MockMultipartFile mockMultipartFile;
    private File testProtectFile;
    private byte[] fileContent;

    @Before
    public void setup() {

        fileContent = "test data".getBytes();

        mockMultipartFile = new MockMultipartFile("file", "filename.pdf", "application/pdf", fileContent);

        testProtectFile = new File("protected.pdf");

        test = storeTestRequest();
        test.setUrl("http://localhost:" + randomServerPort + "/test.pdf");
        test = protectionRequestRepository.save(test);
    }

    @Test
    public void protectWithMultipartFile() throws Exception {

        final String testAccessToken = UUID.randomUUID().toString();
        when(aadHandler.getAccessToken()).thenReturn(Optional.of(testAccessToken));
        when(mipSdkCaller.protect(eq(test), any(File.class), eq(testAccessToken))).thenReturn(testProtectFile);

        // test
        mipHandler.protect(test, mockMultipartFile);

        // verify
        verify(aadHandler, timeout(2_000)).getAccessToken();
        verify(azureStorageRepository, timeout(2_000)).store(testProtectFile, "application/pdf", test.getId());

        final ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
        verify(mipSdkCaller, timeout(2_000)).protect(eq(test), fileCaptor.capture(), eq(testAccessToken));
        assertThat(Files.toByteArray(fileCaptor.getValue())).isEqualTo(fileContent);

    }

    @Test
    public void protectProtectionRequest() throws Exception {

        final String testAccessToken = UUID.randomUUID().toString();
        when(aadHandler.getAccessToken()).thenReturn(Optional.of(testAccessToken));
        when(mipSdkCaller.protect(eq(test), any(File.class), eq(testAccessToken))).thenReturn(testProtectFile);

        // test
        mipHandler.protect(test);

        // verify
        verify(aadHandler, timeout(2_000)).getAccessToken();
        verify(azureStorageRepository, timeout(2_000)).store(testProtectFile, "application/pdf", test.getId());

        final ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
        verify(mipSdkCaller, timeout(2_000)).protect(eq(test), fileCaptor.capture(), eq(testAccessToken));
        assertThat(Files.toByteArray(fileCaptor.getValue())).isEqualTo(fileContent);
    }

}