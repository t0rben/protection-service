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

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;

import com.microsoft.applicationinsights.core.dependencies.googlecommon.util.concurrent.ThreadFactoryBuilder;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
@EnableConfigurationProperties(ProtectionServiceProperties.class)
public class ProtectionServiceConfiguration {

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
        return new ThreadPoolExecutor(2, 10, 60, TimeUnit.SECONDS, blockingQueue,
                new ThreadFactoryBuilder().setNameFormat("executor-pool-%d").build(), new PoolSizeExceededPolicy());
    }

    private static class PoolSizeExceededPolicy extends CallerRunsPolicy {
        @Override
        public void rejectedExecution(final Runnable r, final ThreadPoolExecutor executor) {
            log.warn("Caller has to run on its own, reached limit of queue size {}", executor.getQueue().size());
            super.rejectedExecution(r, executor);
        }
    }

}
