package no.ndla.taxonomy.rest.v1.controllers;

import no.ndla.taxonomy.domain.*;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;

@SpringBootTest
@ActiveProfiles("junit")
public class URNValidatorTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    URNValidator validator;
    ResourceType entity;

    @Before
    public void setUp() throws Exception {
        validator = new URNValidator();
        entity = new ResourceType();
    }

    @Test
    public void urnWithOutURNIsRejected() throws Exception {
        URI id = URI.create("resourcetype:1");

        exception.expect(IdFormatException.class);
        exception.expectMessage("Id should start with urn:");
        validator.validate(id, entity);
    }

    @Test
    public void urnWithoutEntityIsRejected() throws Exception {
        URI id = URI.create("urn:wrong:1");

        exception.expect(IdFormatException.class);
        exception.expectMessage("Id should contain entity name");
        validator.validate(id, entity);
    }

    @Test
    public void urnMustHaveID() throws Exception {
        URI id = URI.create("urn:resourcetype");

        exception.expect(IdFormatException.class);
        exception.expectMessage("Id should have id field");
        validator.validate(id, entity);
    }

    @Test
    public void validURIPasses() throws Exception {
        URI id = URI.create("urn:resourcetype:2");

        validator.validate(id, entity);
    }

    @Test
    public void uriCanHaveVersionField() throws Exception {
        URI id = URI.create("urn:resourcetype:1:134");

        validator.validate(id, entity);
    }

    @Test
    public void listingStyleURIIsAccepted() {
        URI id = URI.create("urn:resource:1:verktoy:blikkenslageren");
        validator.validate(id, new Resource());
    }

    @Test
    public void differentEntityClassesAreAccepted() throws Exception {
        URI id = URI.create("urn:subject:134");
        validator.validate(id, new Subject());
    }
}
