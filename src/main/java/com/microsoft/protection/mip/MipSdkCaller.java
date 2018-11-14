/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */
package com.microsoft.protection.mip;

import java.io.File;

import com.microsoft.protection.data.model.ProtectionRequest;

public interface MipSdkCaller {
    File protect(ProtectionRequest request, File toProtect, String accessToken);
}