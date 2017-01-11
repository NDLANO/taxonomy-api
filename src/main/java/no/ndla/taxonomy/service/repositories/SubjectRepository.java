package no.ndla.taxonomy.service.repositories;


import no.ndla.taxonomy.service.domain.NotFoundException;
import no.ndla.taxonomy.service.domain.Subject;
import org.springframework.data.repository.CrudRepository;

import java.net.URI;

public interface SubjectRepository extends CrudRepository<Subject, Integer> {
    Subject findByPublicId(URI id);

    default Subject getByPublicId(URI id) {
        Subject subject = findByPublicId(id);
        if (null == subject) throw new NotFoundException("subject", id);
        return subject;
    }

    default Subject getByPublicId(String id) {
        return getByPublicId(URI.create(id));
    }

    void deleteByPublicId(URI id);
}
