/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */
package com.microsoft.protection.data;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import com.microsoft.protection.data.model.ProtectionRequest;

@Repository
public interface ProtectionRequestRepository extends PagingAndSortingRepository<ProtectionRequest, String> {

}
