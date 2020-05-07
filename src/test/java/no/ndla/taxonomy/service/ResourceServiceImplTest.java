package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.Builder;
import no.ndla.taxonomy.repositories.ResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@DirtiesContext
public class ResourceServiceImplTest {
    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private Builder builder;

    private EntityConnectionService connectionService;
    private ResourceServiceImpl resourceService;
    private MetadataApiService metadataApiService;

    @BeforeEach
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