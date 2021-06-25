package no.ndla.taxonomy.service;

import no.ndla.taxonomy.repositories.TopicRepository;
import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;

@Service
@Transactional(readOnly = true)
public class SubjectServiceImpl implements SubjectService {
    private final TopicRepository topicRepository;
    private final MetadataApiService metadataApiService;
    private final EntityConnectionService entityConnectionService;

    public SubjectServiceImpl(TopicRepository topicRepository, MetadataApiService metadataApiService,
                              EntityConnectionService entityConnectionService) {
        this.topicRepository = topicRepository;
        this.metadataApiService = metadataApiService;
        this.entityConnectionService = entityConnectionService;
    }

    @Override
    @Transactional
    public void delete(URI publicId) {
        final var subjectToDelete = topicRepository.findFirstByPublicId(publicId).orElseThrow(() -> new NotFoundServiceException("Subject was not found"));

        entityConnectionService.disconnectAllChildren(subjectToDelete);

        topicRepository.delete(subjectToDelete);
        topicRepository.flush();

        metadataApiService.deleteMetadataByPublicId(publicId);
    }
}
