package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.Builder;
import no.ndla.taxonomy.domain.Resource;
import no.ndla.taxonomy.repositories.ResourceRepository;
import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;
import no.ndla.taxonomy.service.exceptions.ServiceUnavailableException;
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
    public void delete() throws NotFoundServiceException, ServiceUnavailableException {
        final var resourceId = builder.resource().getPublicId();

        doAnswer(invocation -> {
            final var resource = (Resource) invocation.getArgument(0);

            assertEquals(resourceId, resource.getPublicId());

            return null;
        }).when(connectionService).replacePrimaryConnectionsFor(any(Resource.class));

        resourceService.delete(resourceId);

        verify(connectionService).replacePrimaryConnectionsFor(any(Resource.class));
        verify(metadataApiService).deleteMetadataByPublicId(resourceId);
    }
}