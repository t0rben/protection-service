/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */
package com.microsoft.protection;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.hateoas.config.EnableHypermediaSupport.HypermediaType;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;

import com.microsoft.applicationinsights.core.dependencies.googlecommon.util.concurrent.ThreadFactoryBuilder;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.protection.controller.AadHandler;
import com.microsoft.protection.controller.MipHandler;
import com.microsoft.protection.controller.ProtectionRequestController;
import com.microsoft.protection.data.AzureStorageRepository;
import com.microsoft.protection.data.ProtectionRequestRepository;
import com.microsoft.protection.mip.FileSampleMipSdkCaller;
import com.microsoft.protection.mip.MipSdkCaller;

import lombok.extern.slf4j.Slf4j;

@EnableAutoConfiguration
@Configuration
@Slf4j
@EnableConfigurationProperties(ProtectionServiceProperties.class)
@EnableAsync
@EnableMongoRepositories({ "com.microsoft.protection.data" })
@EnableHypermediaSupport(type = { HypermediaType.HAL })
public class ProtectionServiceConfiguration {

    private static final int MAX_POOL_SIZE = 10;
    private static final int KEEP_ALIVE_SECONDS = 60;
    private static final int CORE_POOL_SIZE = 2;

    @Bean
    ProtectionRequestController protectionRequestController(
            final ProtectionRequestRepository protectionRequestRepository, final MipHandler mipHandler,
            final AzureStorageRepository azureStorageRepository) {
        return new ProtectionRequestController(protectionRequestRepository, mipHandler, azureStorageRepository);
    }

    @Bean
    MipHandler mipHandler(final ProtectionRequestRepository protectionRequestRepository,
            final AzureStorageRepository azureStorageRepository, final AadHandler aadHandler,
            final MipSdkCaller mipSdkCaller) {
        return new MipHandler(protectionRequestRepository, azureStorageRepository, aadHandler, mipSdkCaller);
    }

    @Bean
    MipSdkCaller mipSdkCaller(final ProtectionServiceProperties properties) {
        return new FileSampleMipSdkCaller(properties);
    }

    @Bean
    AzureStorageRepository azureStorageRepository(final CloudStorageAccount storageAccount,
            final ProtectionServiceProperties properties) {
        return new AzureStorageRepository(storageAccount, properties);
    }

    @Bean
    AadHandler aadHandler(final ThreadPoolExecutor threadPoolExecutor,
            final ProtectionServiceProperties protectionServiceProperties) {
        return new AadHandler(threadPoolExecutor, protectionServiceProperties);
    }

    @Bean
    TaskExecutor taskExecutor() {
        return new ConcurrentTaskExecutor(threadPoolExecutor());
    }

    /**
     * @return central ThreadPoolExecutor for general purpose multi threaded
     *         operations. Tries an orderly shutdown when destroyed.
     */
    @Bean
    ThreadPoolExecutor threadPoolExecutor() {
        final BlockingQueue<Runnable> blockingQueue = new ArrayBlockingQueue<>(100);
        return new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_SECONDS, TimeUnit.SECONDS,
                blockingQueue, new ThreadFactoryBuilder().setNameFormat("executor-pool-%d").build(),
                new PoolSizeExceededPolicy());
    }

    private static class PoolSizeExceededPolicy extends CallerRunsPolicy {
        @Override
        public void rejectedExecution(final Runnable r, final ThreadPoolExecutor executor) {
            log.warn("Caller has to run on its own, reached limit of queue size {}", executor.getQueue().size());
            super.rejectedExecution(r, executor);
        }
    }

}
