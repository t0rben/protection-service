/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */
package com.microsoft.protection;

import javax.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@ConfigurationProperties("com.microsoft.protection")
@Getter
@Setter
public class ProtectionServiceProperties {
    @NotBlank
    private String user;

    @NotBlank
    private String protectionBaseurl = "https://aadrm.com";

    private AAD aad = new AAD();

    @Getter
    @Setter
    public static class AAD {
        // FIXME document
        @NotBlank
        private String tenant;

        @NotBlank
        private String clientId;

        @NotBlank
        private String clientSecret;

        @NotBlank
        private String authorityHost = "https://login.microsoftonline.com/";
    }
}
