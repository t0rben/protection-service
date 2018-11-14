/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */
package com.microsoft.protection.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.protection.controller.model.ProtectionRequestPost;
import com.microsoft.protection.data.AzureStorageRepository;
import com.microsoft.protection.data.ProtectionRequestRepository;
import com.microsoft.protection.data.model.ProtectionRequest;
import com.microsoft.protection.data.model.ProtectionRequest.Right;
import com.microsoft.protection.data.model.ProtectionRequest.Status;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:/test.yml")
public class ProtectionRequestControllerTest {

    @MockBean
    private AzureStorageRepository azureStorageRepository;

    @MockBean
    private MipHandler mipHandler;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ProtectionRequestRepository protectionRequestRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Before
    public void setup() {
        protectionRequestRepository.deleteAll();
    }

    @Test
    public void createProtectionRequest() throws Exception {
        final ProtectionRequestPost test = new ProtectionRequestPost("https://download.here/filename.pdf",
                "user@contoso.com", UUID.randomUUID().toString(), "READ", "filename.pdf");

        mvc.perform(post("/v1/protection").contentType(MediaType.APPLICATION_JSON_UTF8)
                .accept(MediaType.APPLICATION_JSON_UTF8).content(objectMapper.writeValueAsString(test)))
                .andExpect(status().isCreated()).andExpect(jsonPath("id", any(String.class)))
                .andExpect(jsonPath("correlationId", is(test.getCorrelationId())))
                .andExpect(jsonPath("url", is(test.getUrl()))).andExpect(jsonPath("user", is(test.getUser())))
                .andExpect(jsonPath("status", is(Status.PROCESSING.toString())));

        assertThat(protectionRequestRepository.count()).isEqualTo(1L);

        final ProtectionRequest stored = protectionRequestRepository.findAll().iterator().next();

        assertThat(stored.getCorrelationId()).isEqualTo(test.getCorrelationId());
        assertThat(stored.getUrl()).isEqualTo(test.getUrl());
        assertThat(stored.getUser()).isEqualTo(test.getUser());
        assertThat(stored.getFileName()).isEqualTo(test.getFileName());

        verify(mipHandler).protect(stored);

    }

    @Test
    public void createProtectionRequestWithUpload() throws Exception {
        final ProtectionRequestPost test = new ProtectionRequestPost("https://download.here/filename.pdf",
                "user@contoso.com", UUID.randomUUID().toString(), "READ", "filename.pdf");

        final MockMultipartFile mockMultipartFile = new MockMultipartFile("file", "filename.pdf", "application/pdf",
                "test data".getBytes());

        mvc.perform(multipart("/v1/protection/upload").file(mockMultipartFile).param("rights", test.getRights())
                .param("correlationId", test.getCorrelationId()).param("user", test.getUser())
                .accept(MediaType.APPLICATION_JSON_UTF8)).andExpect(status().isCreated())
                .andExpect(jsonPath("id", any(String.class)))
                .andExpect(jsonPath("correlationId", is(test.getCorrelationId())))
                .andExpect(jsonPath("url").doesNotExist()).andExpect(jsonPath("user", is(test.getUser())))
                .andExpect(jsonPath("status", is(Status.PROCESSING.toString())));

        assertThat(protectionRequestRepository.count()).isEqualTo(1L);

        final ProtectionRequest stored = protectionRequestRepository.findAll().iterator().next();

        assertThat(stored.getCorrelationId()).isEqualTo(test.getCorrelationId());
        assertThat(stored.getUrl()).isNull();
        assertThat(stored.getUser()).isEqualTo(test.getUser());
        assertThat(stored.getFileName()).isEqualTo(test.getFileName());

        verify(mipHandler).protect(stored, mockMultipartFile);

    }

    @Test
    public void getProtectionRequests() throws Exception {
        ProtectionRequest test = new ProtectionRequest();
        test.setCorrelationId(UUID.randomUUID().toString());
        test.setRights(Set.of(Right.READ));
        test.setUrl("https://download.here");
        test.setUser("user@contoso.com");

        test = protectionRequestRepository.save(test);

        mvc.perform(get("/v1/protection").accept(MediaType.APPLICATION_JSON_UTF8)).andExpect(status().isOk())
                .andExpect(jsonPath("[0].id", is(test.getId())))
                .andExpect(jsonPath("[0].correlationId", is(test.getCorrelationId())))
                .andExpect(jsonPath("[0].url", is(test.getUrl()))).andExpect(jsonPath("[0].user", is(test.getUser())))
                .andExpect(jsonPath("[0].status", is(Status.PROCESSING.toString())));

        verifyZeroInteractions(mipHandler);
    }

    @Test
    public void getProtectionRequest() throws Exception {
        ProtectionRequest test = new ProtectionRequest();
        test.setCorrelationId(UUID.randomUUID().toString());
        test.setRights(Set.of(Right.READ));
        test.setUrl("https://download.here");
        test.setUser("user@contoso.com");

        test = protectionRequestRepository.save(test);

        mvc.perform(get("/v1/protection/{id}", test.getId()).accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk()).andExpect(jsonPath("id", is(test.getId())))
                .andExpect(jsonPath("correlationId", is(test.getCorrelationId())))
                .andExpect(jsonPath("url", is(test.getUrl()))).andExpect(jsonPath("user", is(test.getUser())))
                .andExpect(jsonPath("status", is(Status.PROCESSING.toString())));

        verifyZeroInteractions(mipHandler);
    }
}
