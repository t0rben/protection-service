/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */
package com.microsoft.protection.error;

public class FileStorageFailedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public FileStorageFailedException(final String message) {
        super(message);
    }

    public FileStorageFailedException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
