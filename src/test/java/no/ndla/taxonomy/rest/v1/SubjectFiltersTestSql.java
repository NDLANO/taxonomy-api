package no.ndla.taxonomy.rest.v1;

import no.ndla.taxonomy.rest.v1.dtos.topics.ResourceIndexDocument;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static no.ndla.taxonomy.TestUtils.*;
import static org.junit.Assert.assertEquals;

public class SubjectFiltersTestSql extends RestTest {

    @Test
    public void can_get_resources_belonging_to_a_filter_and_resource_type_for_a_subject() throws Exception {
        executeSqlScript("classpath:resource_with_filter_and_type_test_setup.sql", false);

        MockHttpServletResponse response = getResource("/v1/subjects/urn:subject:1/resources?filter=urn:filter:1&type=urn:resourcetype:video");
        ResourceIndexDocument[] result = getObject(ResourceIndexDocument[].class, response);

        assertEquals(1, result.length);
        assertAnyTrue(result, r -> "R:1".equals(r.name));
    }

}
