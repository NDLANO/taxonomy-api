package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.DomainEntity;
import no.ndla.taxonomy.domain.exceptions.IdFormatException;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class URNValidatorTest {
    @Test
    public void validate() throws URISyntaxException {
        final var urnValidator = new URNValidator();

        urnValidator.validate(new URI("urn:testentity:100"), new TestEntity());

        try {
            urnValidator.validate(new URI("http:testentity:100"), new TestEntity());
            fail("Expected IdFormatException");
        } catch (IdFormatException exception) {
            assertEquals("Id should start with urn:", exception.getMessage());
        }

        try {
            urnValidator.validate(new URI("urn:entity:100"), new TestEntity());
            fail("Expected IdFormatException");
        } catch (IdFormatException exception) {
            assertEquals("Id should contain entity name", exception.getMessage());
        }

        try {
            urnValidator.validate(new URI("urn:testentity"), new TestEntity());
            fail("Expected IdFormatException");
        } catch (IdFormatException exception) {
            assertEquals("Id should have id field", exception.getMessage());
        }
    }

    private static class TestEntity extends DomainEntity {

    }
}