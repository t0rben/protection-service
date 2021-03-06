/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */
package com.microsoft.protection.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.util.UUID;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.protection.controller.model.ProtectionRequestPost;
import com.microsoft.protection.data.model.ProtectionRequest;
import com.microsoft.protection.data.model.ProtectionRequest.Status;

@AutoConfigureMockMvc
public class ProtectionRequestControllerTest extends AbstractTest {

    @MockBean
    private ProtectionHandler mipHandler;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    private final ProtectionRequestPost testPost = new ProtectionRequestPost("https://download.here/filename.pdf",
            "user@contoso.com", UUID.randomUUID().toString(), "READ", "filename.pdf", "application/pdf", 123L, null);

    @Test
    public void testCreateProtectionRequest() throws Exception {

        mvc.perform(post("/v1/protection").contentType(MediaType.APPLICATION_JSON_UTF8)
                .accept(MediaType.APPLICATION_JSON_UTF8).content(objectMapper.writeValueAsString(testPost)))
                .andExpect(status().isCreated()).andExpect(jsonPath("id", any(String.class)))
                .andExpect(jsonPath("correlationId", is(testPost.getCorrelationId())))
                .andExpect(jsonPath("url", is(testPost.getUrl()))).andExpect(jsonPath("user", is(testPost.getUser())))
                .andExpect(jsonPath("contentType", is(testPost.getContentType())))
                .andExpect(jsonPath("size", is(testPost.getSize().intValue())))
                .andExpect(jsonPath("status", is(Status.PROCESSING.toString())));

        assertThat(protectionRequestRepository.count()).isEqualTo(1L);

        final ProtectionRequest stored = protectionRequestRepository.findAll().iterator().next();

        assertThat(stored.getCorrelationId()).isEqualTo(testPost.getCorrelationId());
        assertThat(stored.getUrl()).isEqualTo(testPost.getUrl());
        assertThat(stored.getUser()).isEqualTo(testPost.getUser());
        assertThat(stored.getFileName()).isEqualTo(testPost.getFileName());
        assertThat(stored.getContentType()).isEqualTo(testPost.getContentType());
        assertThat(stored.getSize()).isEqualTo(testPost.getSize());

        verify(mipHandler).protect(stored);

    }

    @Test
    public void testCreateProtectionRequestWithUpload() throws Exception {
        final MockMultipartFile mockMultipartFile = new MockMultipartFile("file", "filename.pdf", "application/pdf",
                "test data".getBytes());

        mvc.perform(multipart("/v1/protection/upload").file(mockMultipartFile).param("rights", testPost.getRights())
                .param("correlationId", testPost.getCorrelationId()).param("user", testPost.getUser())
                .accept(MediaType.APPLICATION_JSON_UTF8)).andExpect(status().isCreated())
                .andExpect(jsonPath("id", any(String.class)))
                .andExpect(jsonPath("correlationId", is(testPost.getCorrelationId())))
                .andExpect(jsonPath("contentType", is(testPost.getContentType())))
                .andExpect(jsonPath("size", is("test data".getBytes().length)))
                .andExpect(jsonPath("url").doesNotExist()).andExpect(jsonPath("user", is(testPost.getUser())))
                .andExpect(jsonPath("status", is(Status.PROCESSING.toString())));

        assertThat(protectionRequestRepository.count()).isEqualTo(1L);

        final ProtectionRequest stored = protectionRequestRepository.findAll().iterator().next();

        assertThat(stored.getCorrelationId()).isEqualTo(testPost.getCorrelationId());
        assertThat(stored.getUrl()).isNull();
        assertThat(stored.getUser()).isEqualTo(testPost.getUser());
        assertThat(stored.getFileName()).isEqualTo(testPost.getFileName());
        assertThat(stored.getContentType()).isEqualTo(testPost.getContentType());
        assertThat(stored.getSize()).isEqualTo("test data".getBytes().length);

        verify(mipHandler).protect(stored, mockMultipartFile);

    }

    @Test
    public void testGetProtectionRequests() throws Exception {
        ProtectionRequest testStored = storeTestRequest();
        testStored.setStatus(Status.COMPLETE);
        testStored = protectionRequestRepository.save(testStored);
        final URI download = new URI("https://download.from/here/" + testStored.getFileName());

        when(azureStorageRepository.getUri(Mockito.eq(testStored.getId()), eq(testStored.getFileName())))
                .thenReturn(download);

        mvc.perform(get("/v1/protection").accept(MediaTypes.HAL_JSON_UTF8_VALUE)).andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON_UTF8_VALUE))
                .andExpect(jsonPath("[0].id", is(testStored.getId())))
                .andExpect(jsonPath("[0].correlationId", is(testStored.getCorrelationId())))
                .andExpect(jsonPath("[0].url", is(testStored.getUrl())))
                .andExpect(jsonPath("[0].user", is(testStored.getUser())))
                .andExpect(jsonPath("[0].status", is(Status.COMPLETE.toString())))
                .andExpect(jsonPath("[0].contentType", is(testStored.getContentType())))
                .andExpect(jsonPath("[0].size", is(testStored.getSize().intValue())))
                .andExpect(jsonPath("[0]._links.download.href", is(download.toString())));

        mvc.perform(get("/v1/protection/{id}", testStored.getId()).accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk()).andExpect(jsonPath("id", is(testStored.getId())))
                .andExpect(jsonPath("correlationId", is(testStored.getCorrelationId())))
                .andExpect(jsonPath("url", is(testStored.getUrl())))
                .andExpect(jsonPath("user", is(testStored.getUser())))
                .andExpect(jsonPath("status", is(Status.COMPLETE.toString())))
                .andExpect(jsonPath("contentType", is(testStored.getContentType())))
                .andExpect(jsonPath("size", is(testStored.getSize().intValue())))
                .andExpect(jsonPath("_links.download.href", is(download.toString())));

        verifyZeroInteractions(mipHandler);
        verify(azureStorageRepository, times(2)).getUri(testStored.getId(), testStored.getFileName());
    }

    @Test
    public void testDeleteProtectionRequest() throws Exception {
        final ProtectionRequest testStored = storeTestRequest();
        mvc.perform(delete("/v1/protection/{id}", testStored.getId())).andExpect(status().isOk());

        assertThat(protectionRequestRepository.count()).isEqualTo(0L);

        verifyZeroInteractions(mipHandler);
    }
}
