package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.DomainEntity;
import no.ndla.taxonomy.domain.exceptions.IdFormatException;

import java.net.URI;

public class URNValidator {
    public void validate(URI id, DomainEntity entity) {
        validate(id, entity.getClass().getSimpleName());
    }
    public void validate(URI id, String entityName) {
        final String[] idParts = id.toString().split(":");
        if (!idParts[0].equals("urn")) {
            throw new IdFormatException("Id should start with urn:");
        }
        if (!idParts[1].equals(entityName.toLowerCase())) {
            throw new IdFormatException("Id should contain entity name");
        }
        if (idParts.length < 3) {
            throw new IdFormatException("Id should have id field");
        }
    }
}
