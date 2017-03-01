package no.ndla.taxonomy.service.rest.v1;


import no.ndla.taxonomy.service.domain.UrlCacher;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class UrlCacherTest extends RestTest {

    @Autowired
    private UrlCacher urlCacher;

    @Test
    @Ignore
    public void can_add_new_subject_to_cache() throws Exception {

        urlCacher.add("urn:subject:1");

    }
}
