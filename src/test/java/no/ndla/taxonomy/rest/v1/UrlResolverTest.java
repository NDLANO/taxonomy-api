package no.ndla.taxonomy.rest.v1;


import no.ndla.taxonomy.service.dtos.ResolvedUrl;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class UrlResolverTest extends RestTest {

    @Test
    public void can_resolve_url_for_subject() throws Exception {
        builder.subject(s -> s.publicId("urn:subject:1").contentUri("urn:article:1").name("the subject"));

        ResolvedUrl url = resolveUrl("/subject:1");

        assertEquals("urn:subject:1", url.id.toString());
        assertEquals("the subject", url.name);
        assertEquals("urn:article:1", url.contentUri.toString());
        assertEquals(0, url.parents.size());
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

        ResolvedUrl url = resolveUrl("/subject:1/topic:1");

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

        ResolvedUrl url = resolveUrl("/subject:1/topic:1/topic:2");
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

        ResolvedUrl url = resolveUrl("/subject:1/topic:1/resource:1");
        assertEquals("urn:article:1", url.contentUri.toString());
        assertParents(url, "urn:topic:1", "urn:subject:1");
    }

    @Test
    public void ignores_multiple_or_leading_or_trailing_slashes() throws Exception {
        builder.subject(s -> s
                .publicId("urn:subject:1")
                .topic(t -> t
                        .publicId("urn:topic:1")
                        .resource(r -> r.publicId("urn:resource:1").contentUri("urn:article:1"))
                )
        );

        {
            ResolvedUrl url = resolveUrl("/subject:1/topic:1/resource:1");
            assertEquals("urn:article:1", url.contentUri.toString());
            assertParents(url, "urn:topic:1", "urn:subject:1");
            assertEquals("/subject:1/topic:1/resource:1", url.path);
        }
        {
            ResolvedUrl url = resolveUrl("subject:1/topic:1/resource:1");
            assertEquals("urn:article:1", url.contentUri.toString());
            assertParents(url, "urn:topic:1", "urn:subject:1");
            assertEquals("/subject:1/topic:1/resource:1", url.path);
        }
        {
            ResolvedUrl url = resolveUrl("/subject:1/topic:1/resource:1/");
            assertEquals("urn:article:1", url.contentUri.toString());
            assertParents(url, "urn:topic:1", "urn:subject:1");
            assertEquals("/subject:1/topic:1/resource:1", url.path);
        }
        {
            ResolvedUrl url = resolveUrl("//subject:1////topic:1/resource:1//");
            assertEquals("urn:article:1", url.contentUri.toString());
            assertParents(url, "urn:topic:1", "urn:subject:1");
            assertEquals("/subject:1/topic:1/resource:1", url.path);
        }
    }

    @Test
    public void gets_404_on_wrong_path_to_resource() throws Exception {
        builder.subject(s -> s
                .publicId("urn:subject:1")
                .topic(t -> t
                        .publicId("urn:topic:1")
                        .resource(r -> r.publicId("urn:resource:1"))
                )
        );

        testUtils.getResource("/v1/url/resolve?path=/subject:1/topic:2/resource:1", status().isNotFound());
        testUtils.getResource("/v1/url/resolve?path=/subject:1/topic:1/resource:1", status().isOk());
    }

    @Test
    public void sends_404_when_not_found() throws Exception {
        String path = "/no/such/element";
        testUtils.getResource("/v1/url/resolve?path=" + path, status().isNotFound());
    }

    private void assertParents(ResolvedUrl path, String... expected) {
        assertEquals(expected.length, path.parents.size());
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], path.parents.get(i).toString());
        }
    }

    private ResolvedUrl resolveUrl(String url) throws Exception {
        return testUtils.getObject(ResolvedUrl.class, testUtils.getResource("/v1/url/resolve?path=" + url));
    }
}
