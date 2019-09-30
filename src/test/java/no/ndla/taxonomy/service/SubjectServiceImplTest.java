package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.Builder;
import no.ndla.taxonomy.repositories.SubjectRepository;
import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

@SpringBootTest
@RunWith(SpringRunner.class)
public class SubjectServiceImplTest {
    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private Builder builder;

    private SubjectServiceImpl subjectService;

    @Before
    public void setUp() {
        subjectService = new SubjectServiceImpl(subjectRepository);
    }

    @Test
    @Transactional
    public void delete() throws NotFoundServiceException {
        final var subjectId = builder.subject().getPublicId();

        assertTrue(subjectRepository.findFirstByPublicId(subjectId).isPresent());

        subjectService.delete(subjectId);

        assertFalse(subjectRepository.findFirstByPublicId(subjectId).isPresent());
    }
}