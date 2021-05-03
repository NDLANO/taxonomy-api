package no.ndla.taxonomy.rest.v1;

import no.ndla.taxonomy.TestUtils;
import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.repositories.*;
import no.ndla.taxonomy.service.CachedUrlUpdaterService;
import no.ndla.taxonomy.service.MetadataApiService;
import no.ndla.taxonomy.service.dtos.MetadataDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("junit")
@Transactional
public abstract class RestTest {

    @Autowired
    EntityManager entityManager;

    @Autowired
    SubjectRepository subjectRepository;

    @Autowired
    TopicRepository topicRepository;

    @Autowired
    TopicResourceRepository topicResourceRepository;

    @Autowired
    SubjectTopicRepository subjectTopicRepository;

    @Autowired
    TopicSubtopicRepository topicSubtopicRepository;

    @Autowired
    ResourceResourceTypeRepository resourceResourceTypeRepository;

    @Autowired
    ResourceRepository resourceRepository;

    @Autowired
    ResourceTypeRepository resourceTypeRepository;

    @Autowired
    RelevanceRepository relevanceRepository;

    @Autowired
    protected TestUtils testUtils;

    @MockBean
    protected MetadataApiService metadataApiService;

    @Autowired
    protected CachedUrlUpdaterService cachedUrlUpdaterService;

    protected Builder builder;

    private MetadataDto createMetadataObject(URI publicId) {
        final var metadata = new MetadataDto();
        metadata.setPublicId(publicId.toString());

        // Can search for RESOURCE1 where publicId is urn:resource:1 in the test
        metadata.setGrepCodes(Set.of(publicId.getSchemeSpecificPart().replace(":", "").toUpperCase()));

        metadata.setVisible(true);

        return metadata;
    }

    @SuppressWarnings("unchecked")
    @BeforeEach
    public void restTestSetUp() {
        builder = new Builder(entityManager, cachedUrlUpdaterService);

        when(metadataApiService.getMetadataByPublicId(any(URI.class)))
                .thenAnswer(invocationOnMock ->
                        metadataApiService.getMetadataByPublicId(List.of((URI) invocationOnMock.getArgument(0))).stream().findFirst().orElseThrow());

        when(metadataApiService.getMetadataByPublicId(any(Collection.class))).thenAnswer(invocationOnMock -> {
            final var idList = (Collection<URI>) invocationOnMock.getArgument(0);

            final var returnList = new HashSet<MetadataDto>();

            for (var publicId : idList) {
                returnList.add(createMetadataObject(publicId));
            }

            return returnList;
        });
    }

    <T> T save(T entity) {
        entityManager.persist(entity);
        return entity;
    }

    Subject newSubject() {
        return save(new Subject());
    }

    Topic newTopic() {
        return save(new Topic());
    }

    Resource newResource() {
        return save(new Resource());
    }

    ResourceType newResourceType() {
        return save(new ResourceType());
    }
}
