/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */
package com.microsoft.protection.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;

import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.microsoft.protection.data.AzureStorageRepository;
import com.microsoft.protection.data.ProtectionRequestRepository;
import com.microsoft.protection.data.model.ProtectionRequest;
import com.microsoft.protection.data.model.ProtectionRequest.Status;
import com.microsoft.protection.error.ProtectionFailedException;
import com.microsoft.protection.mip.MipSdkCaller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class ProtectionHandler {

    private static final int READ_TIMEOUT_MS = 2_000;
    private static final int CONNECT_TIMEOUT_MS = 2_000;
    private final ProtectionRequestRepository protectionRequestRepository;
    private final AzureStorageRepository azureStorageRepository;
    private final AadHandler aadHandler;
    private final MipSdkCaller mipSdkCaller;
    private final ProtectionPublisher protectionPublisher;

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

    private static long copyMultipart(final MultipartFile file, final File toProtect) throws IOException {
        try (final InputStream upload = file.getInputStream()) {
            try (OutputStream temp = new FileOutputStream(toProtect)) {
                return ByteStreams.copy(upload, temp);
            }
        }
    }

    private void copyAndprotect(final ProtectionRequest request, final MultipartFile file) {

        final File myTempDir = Files.createTempDir();
        final File toProtect = new File(myTempDir, request.getFileName());

        try {
            final long size;
            if (file == null) {
                size = copyFromUrl(request, toProtect);

            } else {
                size = copyMultipart(file, toProtect);
            }

            verifySize(request, size);

            final String accessToken = aadHandler.getAccessToken()
                    .orElseThrow(() -> new ProtectionFailedException("Could not get access token from AAD"));
            final File protectedFile = mipSdkCaller.protect(request, toProtect, accessToken);
            azureStorageRepository.store(protectedFile, request.getContentType(), request.getId());
            request.setStatus(Status.COMPLETE);

        } catch (final Exception e) {
            log.error("Failed to protect " + request, e);
            request.setStatus(Status.ERROR);
            request.setStatusReason(e.getMessage());
        }
        protectionPublisher.orderComplete(protectionRequestRepository.save(request));
    }

    private static void verifySize(final ProtectionRequest request, final long size) {
        if (request.getSize() == null) {
            request.setSize(size);
        } else if (request.getSize() != size) {
            throw new ProtectionFailedException(
                    "Provide file size " + request.getSize() + " does not match with actual file size " + size);
        }
    }

    private static long copyFromUrl(final ProtectionRequest request, final File toProtect) throws IOException {

        final URLConnection connection = new URL(request.getUrl()).openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        try (InputStream in = connection.getInputStream(); OutputStream out = FileUtils.openOutputStream(toProtect)) {
            return IOUtils.copyLarge(in, out);
        }
    }

}
