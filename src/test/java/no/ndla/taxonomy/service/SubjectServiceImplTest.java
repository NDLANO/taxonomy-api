package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.Builder;
import no.ndla.taxonomy.repositories.SubjectRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@SpringBootTest
@RunWith(SpringRunner.class)
public class SubjectServiceImplTest {
    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private Builder builder;

    private SubjectServiceImpl subjectService;

    private MetadataApiService metadataApiService;

    @Before
    public void setUp() {
        metadataApiService = mock(MetadataApiService.class);

        subjectService = new SubjectServiceImpl(subjectRepository, metadataApiService);
    }

    @Test
    @Transactional
    public void delete() {
        final var subjectId = builder.subject().getPublicId();

        assertTrue(subjectRepository.findFirstByPublicId(subjectId).isPresent());

        subjectService.delete(subjectId);

        assertFalse(subjectRepository.findFirstByPublicId(subjectId).isPresent());

        verify(metadataApiService).deleteMetadataByPublicId(subjectId);
    }
}