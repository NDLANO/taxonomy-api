package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.Builder;
import no.ndla.taxonomy.domain.UrlMapping;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.net.URI;

import static org.junit.Assert.assertEquals;

@SpringBootTest
@RunWith(SpringRunner.class)
/*
 * Test Service and database together.
 */
public class UrlResolverServiceTest {
    private Builder builder;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private UrlResolverService urlResolverService;

    @Before
    public void restTestSetUp() {
        builder = new Builder(entityManager);
    }

    @Test
    @Transactional
    public void resolveOldUrl() {
        final String subjectId = "urn:subject:11";
        final String nodeId = "urn:topic:1:183926";
        builder.subject(s -> s
                .publicId(subjectId)
                .topic(t -> t
                        .publicId(nodeId)
                )
        );
        final String oldUrl = "ndla.no/nb/node/183926?fag=127013";
        UrlMapping urlMapping = builder.urlMapping(c -> c.oldUrl(oldUrl).public_id(nodeId).subject_id(subjectId));
        entityManager.persist(urlMapping);
        entityManager.flush();

        String path = urlResolverService.resolveUrl(oldUrl);

        assertEquals("/subject:11/topic:1:183926", path);
    }

    @Test
    @Transactional
    public void resolveOldUrlNoSubjectPrimaryPath() {
        String nodeId = "urn:topic:1:183926";
        builder.subject(s -> s
                .publicId("urn:subject:2")
                .topic(t -> t
                        .publicId(nodeId)
                )
        );
        String oldUrl = "ndla.no/nb/node/183926?fag=127013";
        UrlMapping urlMapping = builder.urlMapping(c -> c.oldUrl(oldUrl).public_id(nodeId));
        entityManager.persist(urlMapping);
        entityManager.flush();

        String path = urlResolverService.resolveUrl(oldUrl);

        assertEquals("/subject:2/topic:1:183926", path);
    }

    @Test
    @Transactional
    public void resolveOldUrlBadSubjectPrimaryPath() {
        builder.subject(s -> s
                .publicId("urn:subject:2")
                .topic(t -> t
                        .publicId("urn:topic:1:183926")
                )
        );
        String oldUrl = "ndla.no/nb/node/183926?fag=127013";
        UrlMapping urlMapping = builder.urlMapping(c -> c.oldUrl(oldUrl).public_id("urn:topic:1:183926").subject_id("urn:subject:11"));
        entityManager.persist(urlMapping);
        entityManager.flush();

        String path = urlResolverService.resolveUrl(oldUrl);

        assertEquals("/subject:2/topic:1:183926", path);
    }

    @Test(expected = Exception.class)
    @Transactional
    public void putOldUrlForNonexistentResource() throws Exception {
        urlResolverService.putUrlMapping("abc", URI.create("urn:topic:1:12"), URI.create("urn:subject:12"));
    }

    @Test
    @Transactional
    public void putOldUrl() throws Exception {
        final String subjectId = "urn:subject:12";
        final String nodeId = "urn:topic:1:183926";
        builder.subject(s -> s
                .publicId(subjectId)
                .topic(t -> t
                        .publicId(nodeId)
                )
        );
        entityManager.flush();

        final String oldUrl = "ndla.no/nb/node/183926?fag=127013";
        urlResolverService.putUrlMapping(oldUrl, URI.create(nodeId), URI.create(subjectId));
        entityManager.flush();

        String path = urlResolverService.resolveUrl(oldUrl);
        assertEquals("/subject:12/topic:1:183926", path);
    }

    @Test
    @Transactional
    public void putOldUrlTwice() throws Exception {
        final String subjectId = "urn:subject:12";
        final String nodeId = "urn:topic:1:183926";
        builder.subject(s -> s
                .publicId(subjectId)
                .topic(t -> t
                        .publicId(nodeId)
                )
        );
        entityManager.flush();

        final String oldUrl = "ndla.no/nb/node/183926?fag=127013";
        urlResolverService.putUrlMapping(oldUrl, URI.create(nodeId), URI.create(subjectId));
        urlResolverService.putUrlMapping(oldUrl, URI.create(nodeId), URI.create(subjectId));
        entityManager.flush();

        String path = urlResolverService.resolveUrl(oldUrl);
        assertEquals("/subject:12/topic:1:183926", path);
    }

    @Test
    @Transactional
    public void putOldUrlWithSubjectQueryWithoutSubject() throws Exception {
        final String subjectId = "urn:subject:12";
        final String nodeId = "urn:topic:1:183926";
        builder.subject(s -> s
                .publicId(subjectId)
                .topic(t -> t
                        .publicId(nodeId)
                )
        );
        entityManager.flush();

        final String oldUrl = "ndla.no/nb/node/183926?fag=127013";
        urlResolverService.putUrlMapping(oldUrl, URI.create(nodeId), URI.create(subjectId));

        entityManager.flush();

        String path = urlResolverService.resolveUrl("ndla.no/nb/node/183926");
        assertEquals("/subject:12/topic:1:183926", path);
    }


}