/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */
package com.microsoft.protection.data;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.springframework.stereotype.Component;

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

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class AzureStorageRepository {
    // TODO handle valid until by means of auto delete entry

    private final CloudBlobClient blobClient;
    private final ProtectionServiceProperties properties;

    AzureStorageRepository(final CloudStorageAccount storageAccount, final ProtectionServiceProperties properties) {
        this.blobClient = storageAccount.createCloudBlobClient();
        this.properties = properties;
    }

    private CloudBlobContainer getContainer() throws URISyntaxException, StorageException {
        final CloudBlobContainer container = blobClient.getContainerReference(properties.getStorageContainerName());
        container.createIfNotExists(BlobContainerPublicAccessType.CONTAINER, new BlobRequestOptions(),
                new OperationContext());

        return container;
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

    private CloudBlockBlob getBlob(final String id, final String fileName) throws URISyntaxException, StorageException {
        final CloudBlobContainer container = getContainer();
        final CloudBlobDirectory directory = container.getDirectoryReference(id);
        return directory.getBlockBlobReference(fileName);
    }

    public void delete(final String id, final String fileName) throws StorageException, URISyntaxException {
        final CloudBlockBlob blob = getBlob(id, fileName);

        log.info("Deleting Azure Storage blob from container {} and id {}", blob.getContainer().getName(), id);
        blob.delete();
    }

    public URI getUri(final String id, final String fileName) throws StorageException, URISyntaxException {
        final CloudBlockBlob blob = getBlob(id, fileName);

        log.info("Deleting Azure Storage blob from container {} and id {}", blob.getContainer().getName(), id);
        return blob.getUri();
    }

}
