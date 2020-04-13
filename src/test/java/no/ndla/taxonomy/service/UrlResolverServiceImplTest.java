package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.Builder;
import no.ndla.taxonomy.domain.UrlMapping;
import no.ndla.taxonomy.repositories.ResourceRepository;
import no.ndla.taxonomy.repositories.SubjectRepository;
import no.ndla.taxonomy.repositories.TopicRepository;
import no.ndla.taxonomy.repositories.UrlMappingRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.net.URI;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

@SpringBootTest
@RunWith(SpringRunner.class)
/*
 * Test Service and database together.
 */
public class UrlResolverServiceImplTest {
    @Autowired
    private Builder builder;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private SubjectRepository subjectRepository;
    @Autowired
    private TopicRepository topicRepository;
    @Autowired
    private ResourceRepository resourceRepository;
    @Autowired
    private UrlMappingRepository urlMappingRepository;
    @Autowired
    private OldUrlCanonifier oldUrlCanonifier;

    private UrlResolverServiceImpl urlResolverService;

    @Before
    public void restTestSetUp() {
        urlResolverService = new UrlResolverServiceImpl(subjectRepository, topicRepository, resourceRepository, urlMappingRepository, oldUrlCanonifier);
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

        String path = urlResolverService.resolveOldUrl(oldUrl).orElseThrow();

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

        assertFalse(urlResolverService.resolveOldUrl(oldUrl).isPresent());
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

        String path = urlResolverService.resolveOldUrl("ndla.no/nb/node/183926?fag=127013").orElseThrow();

        assertEquals("/subject:11/topic:1:183926", path);
    }

    @Test
    @Transactional
    public void resolveOldUrlWhenNoSubjectImportedToPrimaryPath() {
        String nodeId = "urn:topic:1:183926";
        builder.subject(s -> s
                .publicId("urn:subject:2")
                .topic(t -> t
                        .publicId(nodeId)
                )
        );
        String oldUrl = "ndla.no/node/183926?fag=127013";
        UrlMapping urlMapping = builder.urlMapping(c -> c.oldUrl(oldUrl).public_id(nodeId));
        entityManager.persist(urlMapping);
        entityManager.flush();

        String path = urlResolverService.resolveOldUrl(oldUrl).orElseThrow();

        assertEquals("/subject:2/topic:1:183926", path);
    }

    @Test
    @Transactional
    public void resolveOldUrlWhenNoSubjectImportedOrQueriedToPrimaryPath() {
        String nodeId = "urn:topic:1:183926";
        builder.subject(s -> s
                .publicId("urn:subject:2")
                .topic(t -> t
                        .publicId(nodeId)
                )
        );
        String oldUrl = "ndla.no/node/183926";
        UrlMapping urlMapping = builder.urlMapping(c -> c.oldUrl(oldUrl).public_id(nodeId));
        entityManager.persist(urlMapping);
        entityManager.flush();

        String path = urlResolverService.resolveOldUrl(oldUrl).orElseThrow();

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

        String path = urlResolverService.resolveOldUrl("ndla.no/node/183926").orElseThrow();

        assertEquals("/subject:11/topic:1:183926", path);
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
        String oldUrl = "ndla.no/node/183926?fag=127013";
        UrlMapping urlMapping = builder.urlMapping(c -> c.oldUrl(oldUrl).public_id("urn:topic:1:183926").subject_id("urn:subject:11"));
        entityManager.persist(urlMapping);
        entityManager.flush();

        String path = urlResolverService.resolveOldUrl(oldUrl).orElseThrow();

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

        String path = urlResolverService.resolveOldUrl(oldUrl).orElseThrow();
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

        String path = urlResolverService.resolveOldUrl(oldUrl).orElseThrow();
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

        String path = urlResolverService.resolveOldUrl("ndla.no/nb/node/183926").orElseThrow();
        assertEquals("/subject:12/topic:1:183926", path);
    }

    @Test
    @Transactional
    public void resolveEntitiesFromPath() {
        builder.subject(s -> s
                .publicId("urn:subject:1")
                .topic(t -> t
                        .publicId("urn:topic:1")
                        .resource("resource", resourceBuilder -> {
                            resourceBuilder.publicId("urn:resource:1")
                                    .name("Resource Name")
                                    .contentUri(URI.create("urn:test:1"));
                        })
                )
        );

        builder.subject(s -> s
                .publicId("urn:subject:2")
                .topic(t -> t
                        .publicId("urn:topic:2")
                        .resource("resource")
                )
        );

        builder.subject(s -> s
                .publicId("urn:subject:3")
                .topic(t -> {
                            t.publicId("urn:topic:3");
                            t.isContext(true);
                            t.resource("resource");
                        }
                )
        );

        // Four paths exists to the same resource:
        // /subject:1/topic:1/resource:1
        // /subject:2/topic:2/resource:2
        // /subject:3/topic:3/resource:2
        // /topic:3/resource:2

        {
            final var resolvedUrl = urlResolverService.resolveUrl("/subject:1/topic:1/resource:1").orElseThrow();
            assertEquals(2, resolvedUrl.parents.size());

            final var parentIdList = resolvedUrl.parents
                    .stream()
                    .map(URI::getSchemeSpecificPart)
                    .collect(Collectors.toList());

            assertEquals("resource:1", resolvedUrl.id.getSchemeSpecificPart());
            assertEquals("Resource Name", resolvedUrl.name);
            assertEquals(URI.create("urn:test:1"), resolvedUrl.contentUri);
            assertEquals("/subject:1/topic:1/resource:1", resolvedUrl.path);

            assertEquals("topic:1", parentIdList.get(0));
            assertEquals("subject:1", parentIdList.get(1));
        }

        {
            final var resolvedUrl = urlResolverService.resolveUrl("/subject:2/topic:2/resource:1").orElseThrow();
            assertEquals(2, resolvedUrl.parents.size());

            final var parentIdList = resolvedUrl.parents
                    .stream()
                    .map(URI::getSchemeSpecificPart)
                    .collect(Collectors.toList());

            assertEquals("resource:1", resolvedUrl.id.getSchemeSpecificPart());
            assertEquals("Resource Name", resolvedUrl.name);
            assertEquals(URI.create("urn:test:1"), resolvedUrl.contentUri);
            assertEquals("/subject:2/topic:2/resource:1", resolvedUrl.path);

            assertEquals("topic:2", parentIdList.get(0));
            assertEquals("subject:2", parentIdList.get(1));
        }

        {
            final var resolvedUrl = urlResolverService.resolveUrl("/subject:2/topic:2").orElseThrow();
            assertEquals(1, resolvedUrl.parents.size());

            final var parentIdList = resolvedUrl.parents
                    .stream()
                    .map(URI::getSchemeSpecificPart)
                    .collect(Collectors.toList());

            assertEquals("topic:2", resolvedUrl.id.getSchemeSpecificPart());
            assertEquals("/subject:2/topic:2", resolvedUrl.path);

            assertEquals("subject:2", parentIdList.get(0));
        }

        {
            final var resolvedUrl = urlResolverService.resolveUrl("/subject:2").orElseThrow();
            assertEquals(0, resolvedUrl.parents.size());
            assertEquals("subject:2", resolvedUrl.id.getSchemeSpecificPart());
            assertEquals("/subject:2", resolvedUrl.path);
        }

        {
            final var resolvedUrl = urlResolverService.resolveUrl("/subject:2/").orElseThrow();
            assertEquals(0, resolvedUrl.parents.size());
            assertEquals("subject:2", resolvedUrl.id.getSchemeSpecificPart());
            assertEquals("/subject:2", resolvedUrl.path);
        }
        {
            final var resolvedUrl = urlResolverService.resolveUrl("subject:2").orElseThrow();
            assertEquals(0, resolvedUrl.parents.size());
            assertEquals("subject:2", resolvedUrl.id.getSchemeSpecificPart());
            assertEquals("/subject:2", resolvedUrl.path);
        }

        // Test with a non-valid path
        assertFalse(urlResolverService.resolveUrl("/subject:2/resource:1").isPresent());

        // Test with a non-context
        assertFalse(urlResolverService.resolveUrl("/topic:1/resource:1").isPresent());
        assertFalse(urlResolverService.resolveUrl("/topic:2/resource:1").isPresent());

        // Since topic3 is a context in itself, it would be valid to use it as root
        {
            final var resolvedUrl = urlResolverService.resolveUrl("/topic:3/resource:1").orElseThrow();
            assertEquals(1, resolvedUrl.parents.size());

            final var parentIdList = resolvedUrl.parents
                    .stream()
                    .map(URI::getSchemeSpecificPart)
                    .collect(Collectors.toList());

            assertEquals("resource:1", resolvedUrl.id.getSchemeSpecificPart());
            assertEquals("Resource Name", resolvedUrl.name);
            assertEquals(URI.create("urn:test:1"), resolvedUrl.contentUri);
            assertEquals("/topic:3/resource:1", resolvedUrl.path);

            assertEquals("topic:3", parentIdList.get(0));
        }

        // Going via subject:3 is also valid
        {
            final var resolvedUrl = urlResolverService.resolveUrl("/subject:3/topic:3/resource:1").orElseThrow();
            assertEquals(2, resolvedUrl.parents.size());

            final var parentIdList = resolvedUrl.parents
                    .stream()
                    .map(URI::getSchemeSpecificPart)
                    .collect(Collectors.toList());

            assertEquals("resource:1", resolvedUrl.id.getSchemeSpecificPart());
            assertEquals("Resource Name", resolvedUrl.name);
            assertEquals(URI.create("urn:test:1"), resolvedUrl.contentUri);
            assertEquals("/subject:3/topic:3/resource:1", resolvedUrl.path);

            assertEquals("topic:3", parentIdList.get(0));
            assertEquals("subject:3", parentIdList.get(1));
        }

        // Additional slashes should make no difference
        {
            final var resolvedUrl = urlResolverService.resolveUrl("////subject:3///topic:3//////resource:1///").orElseThrow();
            assertEquals(2, resolvedUrl.parents.size());

            final var parentIdList = resolvedUrl.parents
                    .stream()
                    .map(URI::getSchemeSpecificPart)
                    .collect(Collectors.toList());

            assertEquals("resource:1", resolvedUrl.id.getSchemeSpecificPart());
            assertEquals("Resource Name", resolvedUrl.name);
            assertEquals(URI.create("urn:test:1"), resolvedUrl.contentUri);
            assertEquals("/subject:3/topic:3/resource:1", resolvedUrl.path);

            assertEquals("topic:3", parentIdList.get(0));
            assertEquals("subject:3", parentIdList.get(1));
        }

        // No leading slash should make no difference
        {
            final var resolvedUrl = urlResolverService.resolveUrl("subject:3///topic:3//////resource:1///").orElseThrow();
            assertEquals(2, resolvedUrl.parents.size());

            final var parentIdList = resolvedUrl.parents
                    .stream()
                    .map(URI::getSchemeSpecificPart)
                    .collect(Collectors.toList());

            assertEquals("resource:1", resolvedUrl.id.getSchemeSpecificPart());
            assertEquals("Resource Name", resolvedUrl.name);
            assertEquals(URI.create("urn:test:1"), resolvedUrl.contentUri);
            assertEquals("/subject:3/topic:3/resource:1", resolvedUrl.path);

            assertEquals("topic:3", parentIdList.get(0));
            assertEquals("subject:3", parentIdList.get(1));
        }
    }
}