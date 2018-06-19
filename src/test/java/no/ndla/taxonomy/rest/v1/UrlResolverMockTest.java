package no.ndla.taxonomy.rest.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.ndla.taxonomy.service.UrlResolverService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.net.URI;

import static no.ndla.taxonomy.TestUtils.getObject;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Mockito.times;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@RunWith(SpringRunner.class)
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

    @Test
    public void resolveOldUrl404WhenNotImported() throws Exception {
        String oldUrl = "no/such/path";

        given(this.urlResolverService.resolveOldUrl(oldUrl)).willReturn(null);
        ResultActions result = mvc.perform(
                get("/v1/url/pathMap?oldUrl=" + oldUrl)
                        .accept(APPLICATION_JSON_UTF8));

        result.andExpect(status().isNotFound());
    }

    @Test
    public void resolveOldUrlExpectNewPathWhenImported() throws Exception {
        String oldUrl = "ndla.no/nb/node/183926?fag=127013";
        String newPath = "subject:11/topic:1:183926";

        given(this.urlResolverService.resolveOldUrl(oldUrl)).willReturn(newPath);
        ResultActions result = mvc.perform(
                get("/v1/url/pathMap?oldUrl=" + oldUrl)
                        .accept(APPLICATION_JSON_UTF8));

        result.andExpect(status().isOk());
        UrlResolver.ResolvedOldUrl resolvedOldUrl = getObject(UrlResolver.ResolvedOldUrl.class, result.andReturn().getResponse());
        assertEquals(newPath, resolvedOldUrl.path);
    }

    @Test
    public void putOldUrl() throws Exception {
        String oldUrl = "ndla.no/nb/node/183926?fag=127013";
        URI nodeId = new URI("urn:topic:1:183926");
        URI subjectId = new URI("urn:subject:11");

        ResultActions result = mvc.perform(
                put("/v1/url/pathMap")
                        .content(new ObjectMapper().writeValueAsString(new UrlResolver.OldUrlMapping(oldUrl, nodeId, subjectId)))
                        .contentType(MediaType.APPLICATION_JSON_UTF8));

        result.andExpect(status().isNoContent());
        verify(this.urlResolverService, times(1)).putPath(oldUrl, nodeId, subjectId);
    }

    @Test
    public void putOldUrlBadParameters() throws Exception {
        UrlResolver.OldUrlMapping oldUrlMapping = new UrlResolver.OldUrlMapping();
        oldUrlMapping.oldUrl = "ndla.no/nb/node/183926?fag=127013";
        oldUrlMapping.nodeId = "b a d";
        oldUrlMapping.subjectId = "b a d";

        ResultActions result = mvc.perform(
                put("/v1/url/pathMap")
                        .content(new ObjectMapper().writeValueAsString(oldUrlMapping))
                        .contentType(MediaType.APPLICATION_JSON_UTF8));

        result.andExpect(status().isBadRequest());
    }

    @Test
    public void putBadOldUrl() throws Exception {
        String oldUrl = "ndla.no/nb/node/183926?fag=127013";
        URI nodeId = new URI("urn:topic:1:183926");
        URI subjectId = new URI("urn:subject:11");
        given(this.urlResolverService.putPath(any(), any(), any())).willThrow(new Exception());

        ResultActions result = mvc.perform(
                put("/v1/url/pathMap")
                        .content(new ObjectMapper().writeValueAsString(new UrlResolver.OldUrlMapping(oldUrl, nodeId, subjectId)))
                        .contentType(MediaType.APPLICATION_JSON_UTF8));

        result.andExpect(status().isNotFound());
    }

}
