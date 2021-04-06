package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.DomainEntity;

import java.net.URI;

public class NodeURNValidator extends URNValidator {
    @Override
    public void validate(URI id, DomainEntity entity) {
        super.validate(id);
    }

    @Override
    public void validate(URI id, String entityName) {
        super.validate(id);
    }
}
