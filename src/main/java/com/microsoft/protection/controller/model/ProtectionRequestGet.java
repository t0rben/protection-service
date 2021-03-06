/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */
package com.microsoft.protection.controller.model;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Setter
@Getter
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProtectionRequestGet extends ProtectionRequestPost {

    @JsonProperty(value = "id")
    private String entityId;

    @JsonProperty
    private String status;

    @JsonProperty
    private String statusReason;

    public ProtectionRequestGet(final String url, final String user, final String correlationId,
            final String permissions, final String entityId, final String status, final String statusReason,
            final String fileName, final String contentType, final Long size, final Date validUntil) {
        super(url, user, correlationId, permissions, fileName, contentType, size, validUntil);
        this.entityId = entityId;
        this.status = status;
        this.statusReason = statusReason;
    }

}
