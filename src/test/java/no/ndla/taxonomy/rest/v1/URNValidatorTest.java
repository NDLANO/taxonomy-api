package no.ndla.taxonomy.rest.v1;

import no.ndla.taxonomy.domain.Resource;
import no.ndla.taxonomy.domain.ResourceType;
import no.ndla.taxonomy.domain.Topic;
import no.ndla.taxonomy.domain.exceptions.IdFormatException;
import no.ndla.taxonomy.service.SubjectURNValidator;
import no.ndla.taxonomy.service.URNValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class URNValidatorTest {
    private URNValidator validator;
    private ResourceType entity;

    @BeforeEach
    public void setUp() {
        validator = new URNValidator();
        entity = new ResourceType();
    }

    @Test
    public void urnWithOutURNIsRejected() {
        URI id = URI.create("resourcetype:1");

        assertThrows(IdFormatException.class, () -> validator.validate(id, entity), "Id should start with urn:");
    }

    @Test
    public void urnWithoutEntityIsRejected() {
        URI id = URI.create("urn:wrong:1");

        assertThrows(IdFormatException.class, () -> validator.validate(id, entity), "Id should have entity name");
    }

    @Test
    public void urnMustHaveID() {
        URI id = URI.create("urn:resourcetype");

        assertThrows(IdFormatException.class, () -> validator.validate(id, entity), "Id should have id field");
    }

    @Test
    public void validURIPasses() {
        URI id = URI.create("urn:resourcetype:2");

        validator.validate(id, entity);
    }

    @Test
    public void uriCanHaveVersionField() {
        URI id = URI.create("urn:resourcetype:1:134");

        validator.validate(id, entity);
    }

    @Test
    public void listingStyleURIIsAccepted() {
        URI id = URI.create("urn:resource:1:verktoy:blikkenslageren");
        validator.validate(id, new Resource());
    }

    @Test
    public void testNewTopicSubjectValidator() {
        URNValidator validator = new SubjectURNValidator();
        URI id = URI.create("urn:subject:134");
        validator.validate(id, new Topic());
    }

    @Test
    public void testNewTopicSubjectValidatorWrongEntityType() {
        URNValidator validator = new SubjectURNValidator();
        URI id = URI.create("urn:subject:134");
        assertThrows(IdFormatException.class, () -> validator.validate(id, new Resource()));
    }
}
