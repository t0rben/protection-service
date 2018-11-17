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

import javax.naming.ServiceUnavailableException;

import org.springframework.web.util.UriComponentsBuilder;

import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.aad.adal4j.ClientCredential;
import com.microsoft.protection.ProtectionServiceProperties;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AadHandler {

    private volatile String accessToken;
    private volatile long accessTokenExpiresAfter;

    private final ThreadPoolExecutor threadPoolExecutor;
    private final ProtectionServiceProperties protectionServiceProperties;

    public Optional<String> getAccessToken() throws MalformedURLException, InterruptedException, ExecutionException,
            TimeoutException, ServiceUnavailableException {
        if (accessToken != null && System.currentTimeMillis() < (accessTokenExpiresAfter - 10_000)) {
            return Optional.of(accessToken);
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

        return Optional.ofNullable(accessToken);
    }
}
