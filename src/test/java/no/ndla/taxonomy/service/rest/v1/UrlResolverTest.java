package no.ndla.taxonomy.service.rest.v1;


import no.ndla.taxonomy.service.domain.Resource;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static no.ndla.taxonomy.service.TestUtils.getObject;
import static no.ndla.taxonomy.service.TestUtils.getResource;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class UrlResolverTest extends RestTest {
    @Test
    public void can_resolve_url_for_subject() throws Exception {
        builder.subject(s -> s.publicId("urn:subject:1").contentUri("urn:article:1").name("the subject"));

        UrlResolver.ResolvedUrl url = resolveUrl("/subject:1");

        assertEquals("urn:subject:1", url.id.toString());
        assertEquals("the subject", url.name);
        assertEquals("urn:article:1", url.contentUri.toString());
        assertEquals(0, url.parents.length);
    }

    @Test
    public void can_resolve_url_for_topic() throws Exception {
        builder.subject(s -> s
                .publicId("urn:subject:1")
                .topic(t -> t
                        .publicId("urn:topic:1")
                        .name("the topic")
                        .contentUri("urn:article:1"))
        );

        UrlResolver.ResolvedUrl url = resolveUrl("/subject:1/topic:1");

        assertEquals("urn:topic:1", url.id.toString());
        assertEquals("the topic", url.name);
        assertEquals("urn:article:1", url.contentUri.toString());
        assertParents(url, "urn:subject:1");
    }

    @Test
    public void can_resolve_url_for_subtopic() throws Exception {
        builder.subject(s -> s
                .publicId("urn:subject:1")
                .topic(t -> t
                        .publicId("urn:topic:1")
                        .subtopic(st -> st.publicId("urn:topic:2").contentUri("urn:article:1"))
                )
        );

        UrlResolver.ResolvedUrl url = resolveUrl("/subject:1/topic:1/topic:2");
        assertEquals("urn:article:1", url.contentUri.toString());
        assertParents(url, "urn:topic:1", "urn:subject:1");
    }


    @Test
    public void can_resolve_url_for_resource() throws Exception {
        builder.subject(s -> s
                .publicId("urn:subject:1")
                .topic(t -> t
                        .publicId("urn:topic:1")
                        .resource(r -> r.publicId("urn:resource:1").contentUri("urn:article:1"))
                )
        );

        UrlResolver.ResolvedUrl url = resolveUrl("/subject:1/topic:1/resource:1");
        assertEquals("urn:article:1", url.contentUri.toString());
        assertParents(url, "urn:topic:1", "urn:subject:1");
    }


    @Test
    public void is_redirected_to_new_url() throws Exception {
        builder.subject(s -> s
                .publicId("urn:subject:1")
                .topic(t -> t
                        .publicId("urn:topic:1")
                        .resource(r -> r.publicId("urn:resource:1"))
                )
        );

        String redirect = resolveUrlAndExpectRedirect("/subject:1/topic:2/resource:1");
        assertEquals("/subject:1/topic:1/resource:1", redirect);
    }

    @Test
    public void is_redirected_to_closest_url() throws Exception {
        Resource resource = builder.resource(r -> r.publicId("urn:resource:1"));
        builder.subject(s -> s
                .publicId("urn:subject:1")
                .topic("topic1", t -> t
                        .publicId("urn:topic:1")
                        .resource(resource)
                )
        );

        builder.subject(s -> s
                .publicId("urn:subject:2")
                .topic("topic1")
        );

        String redirect = resolveUrlAndExpectRedirect("/subject:2/topic:2/resource:1");
        assertEquals("/subject:2/topic:1/resource:1", redirect);
    }

    @Test
    public void sends_404_when_not_found() throws Exception {
        String url = "/no/such/element";
        getResource("/v1/url/resolve?url=" + url, status().isNotFound());
    }

    private void assertParents(UrlResolver.ResolvedUrl url, String... expected) {
        assertEquals(expected.length, url.parents.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], url.parents[i].toString());
        }
    }

    private String resolveUrlAndExpectRedirect(String url) throws Exception {
        MockHttpServletResponse response = getResource("/v1/url/resolve?url=" + url, status().is3xxRedirection());
        return response.getHeader("Location");
    }

    private UrlResolver.ResolvedUrl resolveUrl(String url) throws Exception {
        return getObject(UrlResolver.ResolvedUrl.class, getResource("/v1/url/resolve?url=" + url));
    }
}
