/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */
package com.microsoft.protection.controller.model;

import org.springframework.hateoas.ResourceSupport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor(access = AccessLevel.PUBLIC)
@NoArgsConstructor
@Setter
@Getter
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProtectionRequestPost extends ResourceSupport {

    @JsonProperty
    private String url;

    @JsonProperty(required = true)
    private String user;

    @JsonProperty
    private String correlationId;

    @JsonProperty
    private String rights;

    @JsonProperty
    private String fileName;

    @JsonProperty
    private String contentType;

    @JsonProperty
    private Long size;

    // TODO implement
    // @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy
    // hh:mm:ss")
    // private String validUntil;
}
