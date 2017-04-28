package no.ndla.taxonomy.service.rest.v1;

import no.ndla.taxonomy.service.domain.Filter;
import no.ndla.taxonomy.service.domain.Relevance;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.URI;

import static no.ndla.taxonomy.service.TestUtils.assertAnyTrue;
import static no.ndla.taxonomy.service.TestUtils.getObject;
import static no.ndla.taxonomy.service.TestUtils.getResource;
import static org.junit.Assert.assertEquals;

public class SubjectFiltersTest extends RestTest{



    @Test
    public void can_get_filters_for_subject() throws Exception {
        builder.subject(s -> s
                .name("Byggfag")
                .publicId("urn:subject:1")
                .filter(f -> f.name("Tømrer"))
                .filter(f -> f.name("Rørlegger"))
        );

        MockHttpServletResponse response = getResource("/v1/subjects/urn:subject:1/filters");
        Subjects.FilterIndexDocument[] filters = getObject(Subjects.FilterIndexDocument[].class, response);

        assertEquals(2, filters.length);
        assertAnyTrue(filters, f -> f.name.equals("Tømrer"));
        assertAnyTrue(filters, f -> f.name.equals("Rørlegger"));
    }

    @Test
    public void can_get_resources_belonging_to_a_filter_for_a_subject() throws Exception {
        Filter vg1 = builder.filter(f -> f.publicId("urn:filter:vg1"));
        Filter vg2 = builder.filter(f -> f.publicId("urn:filter:vg2"));
        Relevance core = builder.relevance(r -> r.publicId("urn:relevance:core"));

        URI subjectId = builder.subject(s -> s
                .name("subject")
                .topic(t -> t
                        .name("a")
                        .subtopic(sub -> sub.name("subtopic").resource(r -> r.name("a lecture in a subtopic").filter(vg1, core)))
                        .resource(r -> r.name("an assignment").filter(vg1, core))
                        .resource(r -> r.name("a lecture").filter(vg2, core))
                )
        ).getPublicId();

        MockHttpServletResponse response = getResource("/v1/subjects/" + subjectId + "/resources?filter=" + vg1.getPublicId());
        Topics.ResourceIndexDocument[] result = getObject(Topics.ResourceIndexDocument[].class, response);

        assertEquals(2, result.length);
        assertAnyTrue(result, r -> "a lecture in a subtopic".equals(r.name));
        assertAnyTrue(result, r -> "an assignment".equals(r.name));
    }
}
