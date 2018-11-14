/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */
package com.microsoft.protection.mip;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.zeroturnaround.exec.InvalidExitValueException;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;

import com.microsoft.protection.ProtectionServiceProperties;
import com.microsoft.protection.data.model.ProtectionRequest;
import com.microsoft.protection.error.ProtectionFailedException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class FileSampleMipSdkCaller implements MipSdkCaller {
    private final ProtectionServiceProperties protectionServiceProperties;

    @Override
    public File protect(final ProtectionRequest request, final File toProtect, final String accessToken) {
        final StopWatch watch = new StopWatch();
        watch.start();

        final String sdkCall = buildMipSdkCall(request, toProtect, accessToken);

        String output;
        try {
            output = new ProcessExecutor().commandSplit(sdkCall).destroyOnExit()
                    .redirectError(Slf4jStream.ofCaller().asError()).readOutput(true).timeout(60, TimeUnit.MINUTES)
                    .execute().outputUTF8();
        } catch (InvalidExitValueException | IOException | InterruptedException | TimeoutException e) {
            throw new ProtectionFailedException("Failed to protect " + toProtect, e);
        }

        if (!StringUtils.hasText(output)) {
            throw new ProtectionFailedException("Failed to protect file: no result from MIP SDK.");
        }

        final String[] items = output.split(" ");
        final File protectedFile = new File(items[items.length - 1].trim());

        if (!protectedFile.getAbsoluteFile().exists()) {
            throw new ProtectionFailedException(
                    "MIP SDK has no created a protected file (returned with with message: " + output + ")");
        }

        try {
            FileUtils.forceDelete(toProtect);
        } catch (final IOException e) {
            throw new ProtectionFailedException("Failed to delete " + toProtect, e);
        }

        if (!protectedFile.renameTo(toProtect.getAbsoluteFile())) {
            throw new ProtectionFailedException("Failed to rename " + protectedFile + " to " + toProtect);
        }

        watch.stop();
        log.info("Completed protection of : {} in {} ms", protectedFile.getAbsolutePath(), watch.getTime());

        return toProtect;

    }

    private String buildMipSdkCall(final ProtectionRequest request, final File toProtect, final String accessToken) {
        final StringBuilder sdkCall = new StringBuilder();
        sdkCall.append(protectionServiceProperties.getFileApiCli());
        sdkCall.append(" ");

        sdkCall.append("--username ");
        sdkCall.append(protectionServiceProperties.getUser());
        sdkCall.append(" ");

        sdkCall.append("--rights ");
        sdkCall.append(request.getRightsAsString());
        sdkCall.append(" ");

        sdkCall.append("--protect ");
        sdkCall.append(request.getUser());
        sdkCall.append(" ");

        sdkCall.append("--clientid ");
        sdkCall.append(protectionServiceProperties.getAad().getClientId());
        sdkCall.append(" ");

        sdkCall.append("--protectiontoken ");
        sdkCall.append(accessToken);
        sdkCall.append(" ");

        sdkCall.append("--protectionbaseurl ");
        sdkCall.append(protectionServiceProperties.getProtectionBaseurl());
        sdkCall.append(" ");

        sdkCall.append("--file ");
        sdkCall.append(toProtect.getAbsolutePath());
        sdkCall.append(" ");

        return sdkCall.toString();
    }

}
