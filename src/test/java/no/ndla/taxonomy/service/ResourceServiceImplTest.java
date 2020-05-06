package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.Builder;
import no.ndla.taxonomy.repositories.ResourceRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@SpringBootTest
@RunWith(SpringRunner.class)
public class ResourceServiceImplTest {
    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private Builder builder;

    private EntityConnectionService connectionService;
    private ResourceServiceImpl resourceService;
    private MetadataApiService metadataApiService;

    @Before
    public void setUp() {
        connectionService = mock(EntityConnectionService.class);
        metadataApiService = mock(MetadataApiService.class);

        resourceService = new ResourceServiceImpl(resourceRepository, connectionService, metadataApiService);
    }

    @Test
    @Transactional
    public void delete() {
        final var createdResource = builder.resource();

        resourceService.delete(createdResource.getPublicId());

        verify(connectionService).disconnectAllChildren(createdResource);
        verify(metadataApiService).deleteMetadataByPublicId(createdResource.getPublicId());
    }
}