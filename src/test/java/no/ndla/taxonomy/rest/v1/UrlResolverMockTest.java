/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.rest.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.ndla.taxonomy.TestUtils;
import no.ndla.taxonomy.service.UrlResolverService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.net.URI;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
/*
 Test controller only
 */
public class UrlResolverMockTest {
    @MockBean
    UrlResolverService urlResolverService;

    @MockBean
    JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestUtils testUtils;

    @Test
    public void resolveOldUrl404WhenNotImported() throws Exception {
        String oldUrl = "no/such/path";

        given(this.urlResolverService.resolveOldUrl(oldUrl)).willReturn(Optional.empty());
        ResultActions result = mvc.perform(
                get("/v1/url/mapping?url=" + oldUrl)
                        .accept(APPLICATION_JSON_UTF8));

        result.andExpect(status().isNotFound());
    }

    @Test
    public void resolveOldUrlExpectNewPathWhenImported() throws Exception {
        String oldUrl = "ndla.no/nb/node/183926?fag=127013";
        String newPath = "subject:11/topic:1:183926";

        given(this.urlResolverService.resolveOldUrl(oldUrl)).willReturn(Optional.of(newPath));
        ResultActions result = mvc.perform(
                get("/v1/url/mapping?url=" + oldUrl)
                        .accept(APPLICATION_JSON_UTF8));

        result.andExpect(status().isOk());
        UrlResolver.ResolvedOldUrl resolvedOldUrl = testUtils.getObject(UrlResolver.ResolvedOldUrl.class, result.andReturn().getResponse());
        assertEquals(newPath, resolvedOldUrl.path);
    }

    @Test
    public void putOldUrl() throws Exception {
        String oldUrl = "ndla.no/nb/node/183926?fag=127013";
        URI nodeId = new URI("urn:topic:1:183926");
        URI subjectId = new URI("urn:subject:11");

        ResultActions result = mvc.perform(
                put("/v1/url/mapping")
                        .content(new ObjectMapper().writeValueAsString(new UrlResolver.UrlMapping(oldUrl, nodeId, subjectId)))
                        .contentType(MediaType.APPLICATION_JSON_UTF8));

        result.andExpect(status().isNoContent());
        verify(this.urlResolverService, times(1)).putUrlMapping(oldUrl, nodeId, subjectId);
    }

    @Test
    public void putOldUrlBadParameters() throws Exception {
        UrlResolver.UrlMapping urlMapping = new UrlResolver.UrlMapping();
        urlMapping.url = "ndla.no/nb/node/183926?fag=127013";
        urlMapping.nodeId = "b a d";
        urlMapping.subjectId = "b a d";

        ResultActions result = mvc.perform(
                put("/v1/url/mapping")
                        .content(new ObjectMapper().writeValueAsString(urlMapping))
                        .contentType(MediaType.APPLICATION_JSON_UTF8));

        result.andExpect(status().isBadRequest());
    }

    @Test
    public void putBadOldUrl() throws Exception {
        String oldUrl = "ndla.no/nb/node/183926?fag=127013";
        URI nodeId = new URI("urn:topic:1:183926");
        URI subjectId = new URI("urn:subject:11");

        doThrow(new UrlResolverService.NodeIdNotFoundExeption("")).when(this.urlResolverService).putUrlMapping(any(), any(), any());

        ResultActions result = mvc.perform(
                put("/v1/url/mapping")
                        .content(new ObjectMapper().writeValueAsString(new UrlResolver.UrlMapping(oldUrl, nodeId, subjectId)))
                        .contentType(MediaType.APPLICATION_JSON_UTF8));

        result.andExpect(status().isNotFound());
    }

}
