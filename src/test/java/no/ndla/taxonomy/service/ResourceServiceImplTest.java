package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.Builder;
import no.ndla.taxonomy.repositories.ResourceRepository;
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
public class ResourceServiceImplTest {
    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private Builder builder;

    private ResourceServiceImpl resourceService;

    @Before
    public void setUp() {
        resourceService = new ResourceServiceImpl(resourceRepository);
    }

    @Test
    @Transactional
    public void delete() throws NotFoundServiceException {
        final var resourceId = builder.resource().getPublicId();

        assertTrue(resourceRepository.findFirstByPublicId(resourceId).isPresent());

        resourceService.delete(resourceId);

        assertFalse(resourceRepository.findFirstByPublicId(resourceId).isPresent());
    }
}