package no.ndla.taxonomy.rest.v1;

import no.ndla.taxonomy.domain.Resource;
import no.ndla.taxonomy.domain.ResourceType;
import no.ndla.taxonomy.domain.Subject;
import no.ndla.taxonomy.domain.exceptions.IdFormatException;
import no.ndla.taxonomy.service.URNValidator;
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

    private URNValidator validator;
    private ResourceType entity;

    @Before
    public void setUp() {
        validator = new URNValidator();
        entity = new ResourceType();
    }

    @Test
    public void urnWithOutURNIsRejected() {
        URI id = URI.create("resourcetype:1");

        exception.expect(IdFormatException.class);
        exception.expectMessage("Id should start with urn:");
        validator.validate(id, entity);
    }

    @Test
    public void urnWithoutEntityIsRejected() {
        URI id = URI.create("urn:wrong:1");

        exception.expect(IdFormatException.class);
        exception.expectMessage("Id should contain entity name");
        validator.validate(id, entity);
    }

    @Test
    public void urnMustHaveID() {
        URI id = URI.create("urn:resourcetype");

        exception.expect(IdFormatException.class);
        exception.expectMessage("Id should have id field");
        validator.validate(id, entity);
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
    public void differentEntityClassesAreAccepted() {
        URI id = URI.create("urn:subject:134");
        validator.validate(id, new Subject());
    }
}
