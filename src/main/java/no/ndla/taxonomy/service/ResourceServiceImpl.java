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

    public ResourceServiceImpl(ResourceRepository resourceRepository, EntityConnectionService connectionService) {
        this.resourceRepository = resourceRepository;
        this.connectionService = connectionService;
    }

    @Override
    @Transactional
    public void delete(URI id) throws NotFoundServiceException {
        final var resourceToDelete = resourceRepository.findFirstByPublicId(id).orElseThrow(() -> new NotFoundServiceException("Subject was not found"));

        connectionService.replacePrimaryConnectionsFor(resourceToDelete);

        resourceRepository.delete(resourceToDelete);
        resourceRepository.flush();
    }
}
