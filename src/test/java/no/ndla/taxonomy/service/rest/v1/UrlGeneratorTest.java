package no.ndla.taxonomy.service.rest.v1;


import org.junit.Test;

import static no.ndla.taxonomy.service.TestUtils.getObject;
import static no.ndla.taxonomy.service.TestUtils.getResource;
import static org.junit.Assert.assertEquals;

public class UrlGeneratorTest extends RestTest {
    @Test
    public void can_generate_url_for_subject() throws Exception {
        builder.subject(s -> s.publicId("urn:subject:1"));

        UrlGenerator.UrlResult url = generateUrl("urn:subject:1");
        assertEquals("/subject:1", url.path);
    }

    @Test
    public void can_generate_url_for_topic() throws Exception {
        builder.subject(s -> s
                .publicId("urn:subject:1")
                .topic(t -> t.publicId("urn:topic:1"))
        );

        UrlGenerator.UrlResult url = generateUrl("urn:topic:1");
        assertEquals("/subject:1/topic:1", url.path);
    }

    @Test
    public void can_generate_url_for_subtopic() throws Exception {
        builder.subject(s -> s
                .publicId("urn:subject:1")
                .topic(t -> t
                        .publicId("urn:topic:1")
                        .subtopic(st -> st.publicId("urn:topic:2"))
                )
        );

        UrlGenerator.UrlResult url = generateUrl("urn:topic:2");
        assertEquals("/subject:1/topic:1/topic:2", url.path);
    }

    @Test
    public void can_generate_url_for_resource() throws Exception {
        builder.subject(s -> s
                .publicId("urn:subject:1")
                .topic(t -> t
                        .publicId("urn:topic:1")
                        .resource(r -> r.publicId("urn:resource:1"))
                )
        );

        UrlGenerator.UrlResult url = generateUrl("urn:resource:1");
        assertEquals("/subject:1/topic:1/resource:1", url.path);
    }

    private UrlGenerator.UrlResult generateUrl(String id) throws Exception {
        return getObject(UrlGenerator.UrlResult.class, getResource("/v1/url/generate?id=" + id));
    }

}
