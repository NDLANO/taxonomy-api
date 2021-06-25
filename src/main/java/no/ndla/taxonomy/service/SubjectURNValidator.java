package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.DomainEntity;

import java.net.URI;

public class SubjectURNValidator extends URNValidator {
    @Override
    public void validate(URI id, DomainEntity entity) {
        if ("Topic".equalsIgnoreCase(entity.getClass().getSimpleName())) {
            validate(id, "Subject");
        } else {
            super.validate(id, entity);
        }
    }
}
