package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.Builder;
import no.ndla.taxonomy.repositories.TopicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ExtendWith(SpringExtension.class)
public class SubjectServiceImplTest {
    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private Builder builder;

    private SubjectServiceImpl subjectService;

    private MetadataApiService metadataApiService;

    @BeforeEach
    public void setUp() {
        metadataApiService = mock(MetadataApiService.class);
        EntityConnectionService entityConnectionService = mock(EntityConnectionService.class);

        subjectService = new SubjectServiceImpl(topicRepository, metadataApiService, entityConnectionService);
    }

    @Test
    @Transactional
    public void delete() {
        final var subjectId = builder.subject().getPublicId();

        assertTrue(topicRepository.findFirstByPublicId(subjectId).isPresent());

        subjectService.delete(subjectId);

        assertFalse(topicRepository.findFirstByPublicId(subjectId).isPresent());

        verify(metadataApiService).deleteMetadataByPublicId(subjectId);
    }
}