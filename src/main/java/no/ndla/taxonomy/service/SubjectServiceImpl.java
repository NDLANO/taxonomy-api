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
    private final EntityConnectionService connectionService;

    public SubjectServiceImpl(SubjectRepository subjectRepository, EntityConnectionService connectionService) {
        this.subjectRepository = subjectRepository;
        this.connectionService = connectionService;
    }

    @Override
    @Transactional
    public void delete(URI publicId) throws NotFoundServiceException {
        final var subjectToDelete = subjectRepository.findFirstByPublicId(publicId).orElseThrow(() -> new NotFoundServiceException("Subject was not found"));

        connectionService.replacePrimaryConnectionsFor(subjectToDelete);

        subjectRepository.delete(subjectToDelete);
        subjectRepository.flush();
    }
}
