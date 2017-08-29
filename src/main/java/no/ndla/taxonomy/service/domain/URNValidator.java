package no.ndla.taxonomy.service.domain;

import java.net.URI;

public class URNValidator {
    public void validate(URI id, DomainEntity entity) {
        final String[] idParts = id.toString().split(":");
        if (!idParts[0].equals("urn")) {
            throw new IdFormatException("Id should start with urn:");
        }
        if (!idParts[1].equals(entity.getClass().getSimpleName().toLowerCase())) {
            throw new IdFormatException("Id should contain entity name");
        }
        if (idParts.length < 3) {
            throw new IdFormatException("Id should have id field");
        }
    }
}
