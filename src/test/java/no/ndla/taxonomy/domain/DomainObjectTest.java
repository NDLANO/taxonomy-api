package no.ndla.taxonomy.domain;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.spy;

public class DomainObjectTest {
    private DomainObject domainObject;

    @Before
    public void setUp() {
        this.domainObject = spy(DomainObject.class);
    }

    @Test
    public void setAndGetName() {
        assertNull(domainObject.getName());

        domainObject.setName("testname");
        assertEquals("testname", domainObject.getName());
    }
}