/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */
package com.microsoft.protection.controller;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.naming.ServiceUnavailableException;

import org.apache.commons.io.FileUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.aad.adal4j.ClientCredential;
import com.microsoft.protection.ProtectionServiceProperties;
import com.microsoft.protection.data.AzureStorageRepository;
import com.microsoft.protection.data.ProtectionRequestRepository;
import com.microsoft.protection.data.model.ProtectionRequest;
import com.microsoft.protection.data.model.ProtectionRequest.Status;
import com.microsoft.protection.error.ProtectionFailedException;
import com.microsoft.protection.mip.MipSdkCaller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class MipHandler {
    private volatile String accessToken;
    private volatile long accessTokenExpiresAfter;

    private final ProtectionRequestRepository protectionRequestRepository;
    private final ThreadPoolExecutor threadPoolExecutor;
    private final ProtectionServiceProperties protectionServiceProperties;
    private final AzureStorageRepository azureStorageRepository;

    private final MipSdkCaller mipSdkCaller;

    // TODO create scheduler to pick up unfinished requests, use lock service to
    // synchronize
    @Async
    void protect(final ProtectionRequest request, final MultipartFile file) {
        Assert.notNull(file, "File must not be null!");

        copyAndprotect(request, file);
    }

    @Async
    void protect(final ProtectionRequest request) {
        Assert.hasLength(request.getUrl(), "URL must not be empty");

        copyAndprotect(request, null);
    }

    private void copyMultipart(final MultipartFile file, final File toProtect)
            throws IOException, FileNotFoundException {
        try (final InputStream upload = file.getInputStream()) {
            try (OutputStream temp = new FileOutputStream(toProtect)) {
                ByteStreams.copy(upload, temp);
            }
        }
    }

    private void copyAndprotect(final ProtectionRequest request, final MultipartFile file) {

        final File myTempDir = Files.createTempDir();
        final File toProtect = new File(myTempDir, request.getFileName());

        try {

            if (file == null) {
                copyFromUrl(request, toProtect);
            } else {
                copyMultipart(file, toProtect);
            }

            getAccessToken();
            final File protectedFile = mipSdkCaller.protect(request, toProtect, accessToken);
            azureStorageRepository.store(protectedFile, file.getContentType(), request.getId());
            request.setStatus(Status.COMPLETE);
        } catch (final Exception e) {
            log.error("Failed to protect " + request, e);
            request.setStatus(Status.ERROR);
            request.setStatusReason(e.getMessage());
        }

        protectionRequestRepository.save(request);
    }

    private void copyFromUrl(final ProtectionRequest request, final File toProtect)
            throws IOException, MalformedURLException {
        FileUtils.copyURLToFile(new URL(request.getUrl()), toProtect, 2_000, 2_000);
    }

    private void getAccessToken() throws MalformedURLException, InterruptedException, ExecutionException,
            TimeoutException, ServiceUnavailableException {
        if (accessToken != null && System.currentTimeMillis() < (accessTokenExpiresAfter - 10_000)) {
            return;
        }

        final String authority = UriComponentsBuilder
                .fromHttpUrl(protectionServiceProperties.getAad().getAuthorityHost())
                .path(protectionServiceProperties.getAad().getTenant()).build().toUriString();

        final AuthenticationContext context = new AuthenticationContext(authority, false, threadPoolExecutor);

        final AuthenticationResult response = context.acquireToken(protectionServiceProperties.getProtectionBaseurl(),
                new ClientCredential(protectionServiceProperties.getAad().getClientId(),
                        protectionServiceProperties.getAad().getClientSecret()),
                null).get(5, TimeUnit.SECONDS);
        accessToken = response.getAccessToken();
        accessTokenExpiresAfter = response.getExpiresAfter() * 1000;

        if (accessToken == null) {
            throw new ProtectionFailedException("authentication result was null");
        }
    }

}
