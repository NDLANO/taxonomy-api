package no.ndla.taxonomy.service;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class OldUrlCanonifierTest {

    OldUrlCanonifier canonifier;

    @Before
    public void init() {
        canonifier = new OldUrlCanonifier();
    }

    @Test
    public void verifyCanonification() {
        String pathWithMenu = "ndla.no/nb/node/63920/menu";
        String result1 = canonifier.canonify(pathWithMenu);
        assertEquals("ndla.no/node/63920", result1);
        String pathWithNumberedMenu = "ndla.no/nb/node/13075/menu316?fag=56850";
        String result2 = canonifier.canonify(pathWithNumberedMenu);
        assertEquals("ndla.no/node/13075?fag=56850", result2);
        String pathWithMenuAndSlash = "ndla.no/nb/node/63920/menu/";
        String result3 = canonifier.canonify(pathWithMenuAndSlash);
        assertEquals("ndla.no/node/63920", result3);
    }


}