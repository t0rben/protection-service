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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.naming.ServiceUnavailableException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;
import org.zeroturnaround.exec.InvalidExitValueException;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;

import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.aad.adal4j.ClientCredential;
import com.microsoft.protection.ProtectionServiceProperties;
import com.microsoft.protection.data.ProtectionRequestRepository;
import com.microsoft.protection.data.model.ProtectionRequest;
import com.microsoft.protection.data.model.ProtectionRequest.Status;

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

    // FIXME reduce duplicate code
    @Async
    void protect(final ProtectionRequest request, final MultipartFile file) {

        final File myTempDir = Files.createTempDir();
        final File toProtect = new File(myTempDir, request.getFileName());

        try (final InputStream upload = file.getInputStream()) {
            try (OutputStream temp = new FileOutputStream(toProtect)) {
                ByteStreams.copy(upload, temp);
            }

            protect(request, toProtect);
            request.setStatus(Status.COMPLETE);
        } catch (final Exception e) {
            log.error("Failed to protect " + request, e);
            request.setStatus(Status.ERROR);
            request.setStatusReason(e.getMessage());
        }

        protectionRequestRepository.save(request).block();
    }

    @Async
    void protect(final ProtectionRequest request) {
        Assert.hasLength(request.getUrl(), "URL must not be empty");

        final File myTempDir = Files.createTempDir();
        final File toProtect = new File(myTempDir, request.getFileName());

        try {
            FileUtils.copyURLToFile(new URL(request.getUrl()), toProtect, 2_000, 2_000);
            protect(request, toProtect);
            request.setStatus(Status.COMPLETE);
        } catch (final Exception e) {
            log.error("Failed to protect " + request, e);
            request.setStatus(Status.ERROR);
            request.setStatusReason(e.getMessage());
        }

        protectionRequestRepository.save(request).block();
    }

    private File protect(final ProtectionRequest request, final File toProtect) throws ServiceUnavailableException,
            InterruptedException, ExecutionException, TimeoutException, InvalidExitValueException, IOException {
        final StopWatch watch = new StopWatch();
        watch.start();
        getAccessToken();

        final String sdkCall = buildMipSdkCall(request, toProtect);

        final String output = new ProcessExecutor().commandSplit(sdkCall).destroyOnExit()
                .redirectError(Slf4jStream.ofCaller().asError()).readOutput(true).timeout(60, TimeUnit.MINUTES)
                .execute().outputUTF8();

        if (!StringUtils.hasText(output)) {
            throw new ProtectionFailedException("Failed to protect file: no result from MIP SDK.");
        }

        final String[] items = output.split(" ");
        final File protectedFile = new File(items[items.length - 1].trim());

        if (!protectedFile.getAbsoluteFile().exists()) {
            throw new ProtectionFailedException(
                    "MIP SDK has no created a protected file (returned with with message: " + output + ")");
        }

        FileUtils.forceDelete(toProtect);
        if (!protectedFile.renameTo(toProtect.getAbsoluteFile())) {
            throw new ProtectionFailedException("Failed to rename " + protectedFile + " to " + toProtect);
        }

        watch.stop();
        log.info("Completed protection of : {} in {} ms", protectedFile.getAbsolutePath(), watch.getTime());

        return protectedFile;

    }

    private String buildMipSdkCall(final ProtectionRequest request, final File toProtect) {
        final StringBuilder sdkCall = new StringBuilder();
        sdkCall.append(protectionServiceProperties.getFileApiCli());
        sdkCall.append(" ");

        sdkCall.append("--username ");
        sdkCall.append(protectionServiceProperties.getUser());
        sdkCall.append(" ");
        // FIXME from request
        sdkCall.append("--rights READ ");

        sdkCall.append("--protect ");
        sdkCall.append(request.getUser());
        sdkCall.append(" ");

        sdkCall.append("--clientid ");
        sdkCall.append(protectionServiceProperties.getAad().getClientId());
        sdkCall.append(" ");

        sdkCall.append("--protectiontoken ");
        sdkCall.append(accessToken);
        sdkCall.append(" ");

        // FIXME properties
        sdkCall.append("--protectionbaseurl ");
        sdkCall.append(protectionServiceProperties.getProtectionBaseurl());
        sdkCall.append(" ");

        sdkCall.append("--file ");
        sdkCall.append(toProtect.getAbsolutePath());
        sdkCall.append(" ");

        return sdkCall.toString();
    }

    private void getAccessToken() throws MalformedURLException, InterruptedException, ExecutionException,
            TimeoutException, ServiceUnavailableException {
        if (accessToken != null && System.currentTimeMillis() > (accessTokenExpiresAfter - 10_000)) {
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
