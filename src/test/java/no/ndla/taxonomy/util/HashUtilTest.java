/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class HashUtilTest {

    @Test
    public void get_different_length_hashes() {
        String shortHash = new HashUtil("original", "").shortHash();
        String mediumHash = new HashUtil("original", "").mediumHash();
        String semiHash = new HashUtil("original", "").semiHash();
        String longHash = new HashUtil("original", "").longHash();
        String fullHash = new HashUtil("original", "").fullHash();

        assertEquals(4, shortHash.length());
        assertEquals(8, mediumHash.length());
        assertEquals(12, semiHash.length());
        assertEquals(16, longHash.length());
        assertTrue(fullHash.length() > 16);
    }

    @Test
    public void same_string_gives_same_hashes() {
        String hash1 = new HashUtil("original", "").mediumHash();
        String hash2 = new HashUtil("original", "").mediumHash();
        String hash3 = new HashUtil("original", "").longHash();
        assertEquals(hash1, hash2);
        assertTrue(hash3.startsWith(hash2));
    }
}
