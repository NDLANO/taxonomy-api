/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.spy;

public class DomainObjectTest {
    private DomainObject domainObject;

    @BeforeEach
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