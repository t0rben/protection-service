/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */
package com.microsoft.protection.data;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.OperationContext;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.BlobContainerPublicAccessType;
import com.microsoft.azure.storage.blob.BlobRequestOptions;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlobDirectory;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.protection.ProtectionServiceProperties;
import com.microsoft.protection.error.FileStorageFailedException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AzureStorageRepository {
    // TODO handle valid until by means of auto delete entry

    private final CloudBlobClient blobClient;
    private final ProtectionServiceProperties properties;

    public AzureStorageRepository(final CloudStorageAccount storageAccount,
            final ProtectionServiceProperties properties) {
        this.blobClient = storageAccount.createCloudBlobClient();
        this.properties = properties;
    }

    private CloudBlobContainer getContainer() {
        try {
            final CloudBlobContainer container = blobClient.getContainerReference(properties.getStorageContainerName());
            container.createIfNotExists(BlobContainerPublicAccessType.CONTAINER, new BlobRequestOptions(),
                    new OperationContext());
            return container;
        } catch (final StorageException | URISyntaxException e) {
            throw new FileStorageFailedException("Failed to retrieve container", e);
        }

    }

    public void store(final File file, final String contentType, final String id)
            throws IOException, URISyntaxException, StorageException {

        final CloudBlockBlob blob = getBlob(id, file.getName());

        log.info("Storing file {} with length {} to Azure Storage container {}", id, file.length(),
                properties.getStorageContainerName());

        if (blob.exists()) {
            log.warn("Artifact {} already exists on Azure Storage container {}, don't need to upload twice", id,
                    properties.getStorageContainerName());
            return;
        }

        // Creating blob and uploading file to it
        blob.getProperties().setContentType(contentType);
        blob.uploadFromFile(file.getPath());

        log.debug("Artifact {} stored on Azure Storage container {} with  server side Etag {}", id,
                blob.getContainer().getName(), blob.getProperties().getEtag());
    }

    private CloudBlockBlob getBlob(final String id, final String fileName) {
        final CloudBlobContainer container = getContainer();
        CloudBlobDirectory directory;
        try {
            directory = container.getDirectoryReference(id);
            return directory.getBlockBlobReference(fileName);
        } catch (final StorageException | URISyntaxException e) {
            throw new FileStorageFailedException("Failed to terieve blob!", e);
        }
    }

    public void delete(final String id, final String fileName) {
        final CloudBlockBlob blob = getBlob(id, fileName);

        log.info("Deleting Azure Storage blob from directory {} and id {}", fileName, id);
        try {
            blob.delete();
        } catch (final StorageException e) {
            throw new FileStorageFailedException("Failed to delete blob", e);
        }
    }

    public URI getUri(final String id, final String fileName) {
        final CloudBlockBlob blob = getBlob(id, fileName);

        log.info("Deleting Azure Storage blob from directory {} and id {}", fileName, id);
        return blob.getUri();
    }

}
