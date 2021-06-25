package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.Topic;

import java.net.URI;

public interface DomainEntityHelperService {
    Topic getSubjectByPublicId(URI publicId);

    Topic getTopicByPublicId(URI publicId);
}
