package no.ndla.taxonomy.service.repositories;


import no.ndla.taxonomy.service.domain.NotFoundException;
import no.ndla.taxonomy.service.domain.Subject;
import org.springframework.data.repository.CrudRepository;

import java.net.URI;

public interface SubjectRepository extends CrudRepository<Subject, URI> {
    Subject findById(URI id);

    default Subject getById(URI id) {
        Subject subject = findById(id);
        if (null == subject) throw new NotFoundException("subject", id);
        return subject;
    }

    default Subject getById(String id) {
        return getById(URI.create(id));
    }

    public void deleteById(URI id);
}
