package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.Builder;
import no.ndla.taxonomy.domain.Subject;
import no.ndla.taxonomy.repositories.SubjectRepository;
import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@RunWith(SpringRunner.class)
public class SubjectServiceImplTest {
    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private Builder builder;

    private EntityConnectionService connectionService;
    private SubjectServiceImpl subjectService;

    @Before
    public void setUp() {
        connectionService = mock(EntityConnectionService.class);

        subjectService = new SubjectServiceImpl(subjectRepository, connectionService);
    }

    @Test
    @Transactional
    public void delete() throws NotFoundServiceException {
        final var subjectId = builder.subject().getPublicId();

        doAnswer(invocation -> {
            final var subject = (Subject) invocation.getArgument(0);

            assertEquals(subjectId, subject.getPublicId());

            return null;
        }).when(connectionService).replacePrimaryConnectionsFor(any(Subject.class));

        subjectService.delete(subjectId);

        verify(connectionService).replacePrimaryConnectionsFor(any(Subject.class));
    }
}