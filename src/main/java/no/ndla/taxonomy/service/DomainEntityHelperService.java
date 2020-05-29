package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.Subject;
import no.ndla.taxonomy.domain.Topic;

import java.net.URI;

public interface DomainEntityHelperService {
    Subject getSubjectByPublicId(URI publicId);

    Topic getTopicByPublicId(URI publicId);
}
