/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */
package com.microsoft.protection.controller;

import java.net.MalformedURLException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.web.util.UriComponentsBuilder;

import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.aad.adal4j.ClientCredential;
import com.microsoft.protection.ProtectionServiceProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class AadHandler {

    private static final int BUFFER_MILLISECONDS = 10_000;
    private volatile String accessToken;
    private volatile long accessTokenExpiresAfter;

    private final ThreadPoolExecutor threadPoolExecutor;
    private final ProtectionServiceProperties protectionServiceProperties;

    public Optional<String> getAccessToken() {
        if (accessToken != null && System.currentTimeMillis() < (accessTokenExpiresAfter - BUFFER_MILLISECONDS)) {
            return Optional.of(accessToken);
        }

        try {
            final String authority = UriComponentsBuilder
                    .fromHttpUrl(protectionServiceProperties.getAad().getAuthorityHost())
                    .path(protectionServiceProperties.getAad().getTenant()).build().toUriString();

            final AuthenticationContext context = new AuthenticationContext(authority, false, threadPoolExecutor);

            final AuthenticationResult response = context
                    .acquireToken(protectionServiceProperties.getProtectionBaseurl(),
                            new ClientCredential(protectionServiceProperties.getAad().getClientId(),
                                    protectionServiceProperties.getAad().getClientSecret()),
                            null)
                    .get(5, TimeUnit.SECONDS);

            accessToken = response.getAccessToken();
            accessTokenExpiresAfter = TimeUnit.MILLISECONDS.convert(response.getExpiresAfter(), TimeUnit.SECONDS);
        } catch (MalformedURLException | ExecutionException | TimeoutException e) {
            log.error("Failed to call AAD!", e);
            return Optional.empty();
        } catch (final InterruptedException e) {
            log.warn("Interrupted!", e);
            // Restore interrupted state...
            Thread.currentThread().interrupt();
        }

        return Optional.ofNullable(accessToken);
    }
}
