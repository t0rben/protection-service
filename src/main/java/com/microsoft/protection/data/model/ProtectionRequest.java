/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */
package com.microsoft.protection.data.model;

import java.util.Set;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.URL;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.util.CollectionUtils;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Document(collection = "protectionrequests")
@Getter
@Setter
@ToString
public class ProtectionRequest extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @NotBlank
    @Email
    private String user;

    @NotNull
    private Status status = Status.PROCESSING;

    @NotBlank
    private String fileName;

    @Size(max = 1024)
    private String statusReason;

    @Size(max = 256)
    private String correlationId;

    @URL(regexp = "^(http|https)")
    private String url;

    private Set<Right> rights = Set.of(Right.READ);

    // FIXME: implement
    // private Date validUntil

    public void setRights(final Set<Right> rights) {
        if (!CollectionUtils.isEmpty(rights)) {
            this.rights = rights;
        }
    }

    public void setFileName(final String fileName) {
        this.fileName = fileName.replace(' ', '_');
    }

    public enum Right {
        READ;
    }

    public enum Status {
        PROCESSING,

        COMPLETE,

        ERROR;
    }
}
