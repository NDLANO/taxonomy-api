package no.ndla.taxonomy.service;

import no.ndla.taxonomy.repositories.SubjectRepository;
import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;

@Service
@Transactional(readOnly = true)
public class SubjectServiceImpl implements SubjectService {
    private final SubjectRepository subjectRepository;
    private final MetadataApiService metadataApiService;
    private final EntityConnectionService entityConnectionService;

    public SubjectServiceImpl(SubjectRepository subjectRepository, MetadataApiService metadataApiService,
                              EntityConnectionService entityConnectionService) {
        this.subjectRepository = subjectRepository;
        this.metadataApiService = metadataApiService;
        this.entityConnectionService = entityConnectionService;
    }

    @Override
    @Transactional
    public void delete(URI publicId) {
        final var subjectToDelete = subjectRepository.findFirstByPublicId(publicId).orElseThrow(() -> new NotFoundServiceException("Subject was not found"));

        entityConnectionService.disconnectAllChildren(subjectToDelete);

        subjectRepository.delete(subjectToDelete);
        subjectRepository.flush();

        metadataApiService.deleteMetadataByPublicId(publicId);
    }
}
