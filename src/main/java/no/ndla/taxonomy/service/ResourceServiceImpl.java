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

    public ResourceServiceImpl(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    @Override
    @Transactional
    public void delete(URI id) throws NotFoundServiceException {
        final var resourceToDelete = resourceRepository.findFirstByPublicId(id).orElseThrow(() -> new NotFoundServiceException("Subject was not found"));

        resourceRepository.delete(resourceToDelete);
        resourceRepository.flush();
    }
}
