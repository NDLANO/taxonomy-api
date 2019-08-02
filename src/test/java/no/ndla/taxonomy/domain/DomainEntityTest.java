package no.ndla.taxonomy.domain;

import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.spy;
import static org.springframework.test.util.ReflectionTestUtils.setField;

public class DomainEntityTest {
    private DomainEntity domainEntity;

    @Before
    public void setUp() {
        domainEntity = spy(DomainEntity.class);
    }

    @Test
    public void getId() {
        setField(domainEntity, "id", 1000);
        assertEquals(Integer.valueOf(1000), domainEntity.getId());
    }

    @Test
    public void getAndSetPublicId() throws URISyntaxException {
        assertNull(domainEntity.getPublicId());
        domainEntity.setPublicId(new URI("urn:test1"));
        assertEquals(new URI("urn:test1"), domainEntity.getPublicId());
    }
}