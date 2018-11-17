/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */
package com.microsoft.protection;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.hateoas.config.EnableHypermediaSupport.HypermediaType;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
@EnableMongoRepositories
@EnableHypermediaSupport(type = { HypermediaType.HAL })
public class ProtectionServiceApplication {

    public static void main(final String[] args) {
        SpringApplication.run(ProtectionServiceApplication.class, args);
    }
}
