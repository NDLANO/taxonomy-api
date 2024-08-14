/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.util.*;
import no.ndla.taxonomy.rest.v1.dtos.RelevanceDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RelevanceTest {

    @BeforeEach
    public void setUp() {}

    @Test
    public void testThatAllEnumValuesResultInRelevance() {
        for (var enumValue : Relevance.values()) {
            var relevance = RelevanceStore.fromEnum(enumValue);

            assertEquals(
                    relevance.id, URI.create("urn:relevance:" + enumValue.name().toLowerCase()));
        }
    }

    @Test
    public void testThatAllStoredRelevancesHasADifferentEnumValue() {

        var enums = RelevanceStore.relevances.stream()
                .map(RelevanceDTO::getRelevanceEnumValue)
                .toList();
        Set<Relevance> s = new HashSet<Relevance>(enums);

        if (s.size() != enums.size()) {
            fail("Got duplicate enum values in RelevanceStore.relevances. They should be unique.");
        }
    }
}
