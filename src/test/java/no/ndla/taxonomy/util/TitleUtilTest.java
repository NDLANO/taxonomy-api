/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.util;

import static org.junit.Assert.assertEquals;

import org.junit.jupiter.api.Test;

public class TitleUtilTest {

    @Test
    void test_create_pretty_url() {
        assertEquals(
                "/this-is-a-title__hash",
                TitleUtil.createPrettyUrl("This is a title", "hash").get());
    }

    @Test
    void test_create_pretty_url_with_punctuation() {
        assertEquals(
                "/this-is-a-title-seriously__hash",
                TitleUtil.createPrettyUrl("This is a title, seriously", "hash").get());
    }

    @Test
    void test_create_pretty_url_from_html_formatted_text() {
        assertEquals(
                "/this-is-a-italics-title__hash",
                TitleUtil.createPrettyUrl("This is a <em>italics title</em>", "hash")
                        .get());
    }

    @Test
    void test_get_hash_from_title() {
        assertEquals("hash", TitleUtil.getHashFromPath("/this-is-a-title__hash"));
    }
}
