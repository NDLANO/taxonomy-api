package no.ndla.taxonomy.service;

import no.ndla.taxonomy.repositories.ResourceRepository;
import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;

@Service
@Transactional(readOnly = true)
public class ResourceServiceImpl implements ResourceService {
    private final ResourceRepository resourceRepository;
    private final EntityConnectionService connectionService;
    private final MetadataApiService metadataApiService;

    public ResourceServiceImpl(ResourceRepository resourceRepository, EntityConnectionService connectionService,
                               MetadataApiService metadataApiService) {
        this.resourceRepository = resourceRepository;
        this.connectionService = connectionService;
        this.metadataApiService = metadataApiService;
    }

    @Override
    @Transactional
    public void delete(URI id) {
        final var resourceToDelete = resourceRepository.findFirstByPublicId(id).orElseThrow(() -> new NotFoundServiceException("Subject was not found"));

        // ATM resources can not have any children, but still implements the interface that could have children
        connectionService.disconnectAllChildren(resourceToDelete);

        resourceRepository.delete(resourceToDelete);
        resourceRepository.flush();

        metadataApiService.deleteMetadataByPublicId(id);
    }
}
