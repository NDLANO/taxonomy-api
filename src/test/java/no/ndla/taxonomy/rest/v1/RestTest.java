package no.ndla.taxonomy.rest.v1;

import no.ndla.taxonomy.TestUtils;
import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.repositories.*;
import no.ndla.taxonomy.service.CachedUrlUpdaterService;
import no.ndla.taxonomy.service.MetadataApiService;
import no.ndla.taxonomy.service.MetadataEntityWrapperService;
import no.ndla.taxonomy.service.MetadataWrappedEntity;
import no.ndla.taxonomy.service.dtos.MetadataDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("junit")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
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
    FilterRepository filterRepository;

    @Autowired
    RelevanceRepository relevanceRepository;

    @Autowired
    ResourceFilterRepository resourceFilterRepository;

    @Autowired
    TopicFilterRepository topicFilterRepository;

    @Autowired
    protected TestUtils testUtils;

    @MockBean
    protected MetadataApiService metadataApiService;
    @MockBean
    protected MetadataEntityWrapperService metadataEntityWrapperService;

    @Autowired
    protected CachedUrlUpdaterService cachedUrlUpdaterService;

    protected Builder builder;

    private MetadataDto createMetadataObject(URI publicId, DomainEntity entity) {
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

        when(metadataEntityWrapperService.wrapEntity(any(DomainEntity.class), anyBoolean()))
                .thenAnswer(invocationOnMock ->
                        metadataEntityWrapperService.wrapEntities(List.of((DomainEntity) invocationOnMock.getArgument(0)), invocationOnMock.getArgument(1)).get(0));

        when(metadataEntityWrapperService.wrapEntities(any(List.class), anyBoolean()))
                .thenAnswer(invocationOnMock ->
                        metadataEntityWrapperService.wrapEntities(invocationOnMock.getArgument(0), invocationOnMock.getArgument(1), DomainEntity::getPublicId));

        when(metadataEntityWrapperService.wrapEntities(any(List.class), anyBoolean(), any(Function.class))).thenAnswer(invocationOnMock -> {
            final var entities = (List<DomainEntity>) invocationOnMock.getArgument(0);
            final var includeMetadata = (boolean) invocationOnMock.getArgument(1);
            final var idCallback = (Function<DomainEntity, URI>) invocationOnMock.getArgument(2);

            final var returnList = new ArrayList<MetadataWrappedEntity<DomainEntity>>();

            for (var entity : entities) {
                final var wrapped = mock(MetadataWrappedEntity.class);
                when(wrapped.getEntity()).thenReturn(entity);

                if (includeMetadata) {
                    when(wrapped.getMetadata()).thenAnswer((inv) -> Optional.of(createMetadataObject(idCallback.apply(entity), entity)));
                }

                returnList.add(wrapped);
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
