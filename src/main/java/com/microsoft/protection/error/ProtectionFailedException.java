/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */
package com.microsoft.protection.error;

public class ProtectionFailedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ProtectionFailedException(final String message) {
        super(message);
    }

    public ProtectionFailedException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
