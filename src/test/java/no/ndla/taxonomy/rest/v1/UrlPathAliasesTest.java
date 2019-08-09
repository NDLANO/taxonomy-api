package no.ndla.taxonomy.rest.v1;

import no.ndla.taxonomy.domain.PathAlias;
import no.ndla.taxonomy.service.PathAliasService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = UrlPathAliases.class)
public class UrlPathAliasesTest {
    @MockBean
    private PathAliasService pathAliasService;
    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testPathAliasNotFound() throws Exception {
        given(pathAliasService.pathAliasForPath("topic:1/resource:1")).willReturn(Optional.empty());
        mockMvc.perform(get("http://localhost/v1/url/alias?path=topic:1/resource:1"))
                .andExpect(status().isNotFound());
        verify(pathAliasService, times(1)).pathAliasForPath("topic:1/resource:1");
    }

    @Test
    public void testPathAlias() throws Exception {
        var pathAlias = new PathAlias() {{
            id = 1;
            alias = "the/alias";
            originalPath = "topic:1/resource:1";
            root = "topic:1";
            leaf = "resource:1";
            created = Instant.ofEpochSecond(1000000000L);
            //replacedBy = null;
        }};
        given(pathAliasService.pathAliasForPath("topic:1/resource:1")).willReturn(Optional.of(pathAlias));
        mockMvc.perform(get("http://localhost/v1/url/alias?path=topic:1/resource:1"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"id\": 1, \"alias\": \"the/alias\", \"originalPath\": \"topic:1/resource:1\", \"root\": \"topic:1\", \"leaf\": \"resource:1\", \"created\": \"2001-09-09T01:46:40Z\", \"replacedBy\": null}", true));
        verify(pathAliasService, times(1)).pathAliasForPath("topic:1/resource:1");
    }

    @Test
    public void testAliasResolver() throws Exception {
        var pathAlias = new PathAlias() {{
            id = 1;
            alias = "the/alias";
            originalPath = "topic:1/resource:1";
            root = "topic:1";
            leaf = "resource:1";
            created = Instant.ofEpochSecond(1000000000L);
            //replacedBy = null;
        }};
        given(pathAliasService.resolvePath("the/alias")).willReturn(Optional.of(pathAlias));
        mockMvc.perform(get("http://localhost/v1/url/alias/resolve?alias=the/alias"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"id\": 1, \"alias\": \"the/alias\", \"originalPath\": \"topic:1/resource:1\", \"root\": \"topic:1\", \"leaf\": \"resource:1\", \"created\": \"2001-09-09T01:46:40Z\", \"replacedBy\": null}", true));
        verify(pathAliasService, times(1)).resolvePath("the/alias");
    }

    @Test
    public void testAliasResolverWithReplace() throws Exception {
        var pathAlias = new PathAlias() {{
            id = 1;
            alias = "the/alias";
            originalPath = "topic:1/resource:1";
            root = "topic:1";
            leaf = "resource:1";
            created = Instant.ofEpochSecond(1000000000L);
            //replacedBy = null;
        }};
        given(pathAliasService.resolvePath("old/alias")).willThrow(new PathAliasService.PathAliasReplacedException(pathAlias));
        mockMvc.perform(get("http://localhost/v1/url/alias/resolve?alias=old/alias"))
                .andExpect(status().is(301))
                .andExpect(header().string("Location", "the/alias"))
                .andExpect(content().json("{\"id\": 1, \"alias\": \"the/alias\", \"originalPath\": \"topic:1/resource:1\", \"root\": \"topic:1\", \"leaf\": \"resource:1\", \"created\": \"2001-09-09T01:46:40Z\", \"replacedBy\": null}", true));
        verify(pathAliasService, times(1)).resolvePath("old/alias");
    }
}
