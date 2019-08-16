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
import java.net.URISyntaxException;
import java.util.Set;

import static org.junit.Assert.*;

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
        final String oldUrl = "ndla.no/node/183926?fag=127013";
        UrlMapping urlMapping = builder.urlMapping(c -> c.oldUrl(oldUrl).public_id(nodeId).subject_id(subjectId));
        entityManager.persist(urlMapping);
        entityManager.flush();

        String path = urlResolverService.resolveUrl(oldUrl);

        assertEquals("/subject:11/topic:1:183926", path);
    }


    @Test()
    @Transactional
    public void queryForNonExistingNodeShouldNotMatchSimilarNodeId() {
        String oldUrl = "ndla.no/node/54";
        String otherSubjectId = "urn:subject:1";
        String otherTopicId = "urn:topic:1:54321";
        String otherTopicUrl = "ndla.no/node/54321";

        //create another topic and mapping that should NOT match the query for the url above
        builder.subject(s -> s
                .publicId(otherSubjectId)
                .topic(t -> t
                        .publicId("urn:topic:1:54321")
                )
        );
        entityManager.persist(builder.urlMapping(c -> c.oldUrl(otherTopicUrl).public_id(otherTopicId).subject_id(otherSubjectId)));
        entityManager.flush();

        String path = urlResolverService.resolveUrl(oldUrl);
        assertNull(path);
    }


    @Test
    @Transactional
    public void resolveOldUrlWithLanguage() {
        final String subjectId = "urn:subject:11";
        final String nodeId = "urn:topic:1:183926";
        builder.subject(s -> s
                .publicId(subjectId)
                .topic(t -> t
                        .publicId(nodeId)
                )
        );
        final String oldUrl = "ndla.no/node/183926?fag=127013";
        UrlMapping urlMapping = builder.urlMapping(c -> c.oldUrl(oldUrl).public_id(nodeId).subject_id(subjectId));
        entityManager.persist(urlMapping);
        entityManager.flush();

        String path = urlResolverService.resolveUrl("ndla.no/nb/node/183926?fag=127013");

        assertEquals("/subject:11/topic:1:183926", path);
    }

    @Test
    @Transactional
    public void resolveOldUrlWhenNoSubjectImportedToPrimaryPath() {
        String nodeId = "urn:topic:1:183926";
        builder.subject(s -> s
                .publicId("urn:subject:2")
                .topic(true, t -> t
                        .publicId(nodeId)
                )
        );
        String oldUrl = "ndla.no/node/183926?fag=127013";
        UrlMapping urlMapping = builder.urlMapping(c -> c.oldUrl(oldUrl).public_id(nodeId));
        entityManager.persist(urlMapping);
        entityManager.flush();

        String path = urlResolverService.resolveUrl(oldUrl);

        assertEquals("/subject:2/topic:1:183926", path);
    }

    @Test
    @Transactional
    public void resolveOldUrlWhenNoSubjectImportedOrQueriedToPrimaryPath() {
        String nodeId = "urn:topic:1:183926";
        builder.subject(s -> s
                .publicId("urn:subject:2")
                .topic(true, t -> t
                        .publicId(nodeId)
                )
        );
        String oldUrl = "ndla.no/node/183926";
        UrlMapping urlMapping = builder.urlMapping(c -> c.oldUrl(oldUrl).public_id(nodeId));
        entityManager.persist(urlMapping);
        entityManager.flush();

        String path = urlResolverService.resolveUrl(oldUrl);

        assertEquals("/subject:2/topic:1:183926", path);
    }

    @Test
    @Transactional
    public void resolveOldUrlWhenSubjectImportedButNotQueriedToPrimaryPath() {
        final String subjectId = "urn:subject:11";
        String nodeId = "urn:topic:1:183926";
        builder.subject(s -> s
                .publicId(subjectId)
                .topic(t -> t
                        .publicId(nodeId)
                )
        );
        String oldUrl = "ndla.no/node/183926?fag=127013";
        UrlMapping urlMapping = builder.urlMapping(c -> c.oldUrl(oldUrl).public_id(nodeId).subject_id(subjectId));
        entityManager.persist(urlMapping);
        entityManager.flush();

        String path = urlResolverService.resolveUrl("ndla.no/node/183926");

        assertEquals("/subject:11/topic:1:183926", path);
    }

    @Test
    @Transactional
    public void resolveOldUrlBadSubjectPrimaryPath() {
        builder.subject(s -> s
                .publicId("urn:subject:2")
                .topic(true, t -> t
                        .publicId("urn:topic:1:183926")
                )
        );
        String oldUrl = "ndla.no/node/183926?fag=127013";
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
    public void putOldUrlWithNoPaths() throws Exception {
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
        try {
            urlResolverService.putUrlMapping(oldUrl, URI.create("urn:topic:1:283926"), URI.create(subjectId));
            fail("Expected NodeIdNotFoundExeption");
        } catch (UrlResolverService.NodeIdNotFoundExeption ignored) {

        }
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


    @Test
    @Transactional
    public void getResolvablePathEntitiesFromPublicId() throws URISyntaxException {
        // Pretty sure this should be illegal; adding different objects with the same publicId, but still supported

        final var subject = builder.subject();
        subject.setPublicId(new URI("urn:test:1"));

        final var topic = builder.topic();
        topic.setPublicId(new URI("urn:test:1"));

        final var resource = builder.resource();
        resource.setPublicId(new URI("urn:test:1"));

        final var resource2 = builder.resource();
        resource2.setPublicId(new URI("urn:test:2"));

        entityManager.flush();

        final var urn1Set = urlResolverService.getResolvablePathEntitiesFromPublicId(new URI("urn:test:1"));
        final var urn2Set = urlResolverService.getResolvablePathEntitiesFromPublicId(new URI("urn:test:2"));

        assertEquals(3, urn1Set.size());
        assertEquals(1, urn2Set.size());

        assertTrue(urn1Set.containsAll(Set.of(subject, topic, resource)));
        assertTrue(urn2Set.contains(resource2));
    }
}