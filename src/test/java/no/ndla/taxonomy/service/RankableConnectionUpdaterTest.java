/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.EntityWithPath;
import no.ndla.taxonomy.domain.EntityWithPathConnection;
import no.ndla.taxonomy.domain.Relevance;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RankableConnectionUpdaterTest {
    private void verifyOrder(List<TestRankable> list, List<String> expectedOrder) {
        assertEquals(expectedOrder.size(), list.size());

        for (var index = 0; index < expectedOrder.size(); index++) {
            assertEquals(expectedOrder.get(index), list.get(index).getPublicId().toString());
        }
    }

    @Test
    public void rank() throws URISyntaxException {
        final var relevance = new Relevance();

        final var rankable1 = new TestRankable("urn:1", relevance, 0);
        final var rankable2 = new TestRankable("urn:2", relevance, 1);
        final var rankable3 = new TestRankable("urn:3", relevance, 10);
        final var rankable4 = new TestRankable("urn:4", relevance, 11);
        final var rankable5 = new TestRankable("urn:5", relevance, 12);
        final var rankable6 = new TestRankable("urn:6", relevance, 20);
        final var rankable7 = new TestRankable("urn:7", relevance, 100);
        final var rankable8 = new TestRankable("urn:8", relevance, 1000);

        final var rankableList = new ArrayList<TestRankable>();

        RankableConnectionUpdater.rank(rankableList, rankable1, 0);
        RankableConnectionUpdater.rank(rankableList, rankable2, 1);
        RankableConnectionUpdater.rank(rankableList, rankable3, 10);
        RankableConnectionUpdater.rank(rankableList, rankable4, 11);
        RankableConnectionUpdater.rank(rankableList, rankable5, 12);
        RankableConnectionUpdater.rank(rankableList, rankable6, 20);
        RankableConnectionUpdater.rank(rankableList, rankable7, 100);

        assertEquals(0, rankable1.getRank());
        assertEquals(1, rankable2.getRank());
        assertEquals(10, rankable3.getRank());
        assertEquals(11, rankable4.getRank());
        assertEquals(12, rankable5.getRank());
        assertEquals(20, rankable6.getRank());
        assertEquals(100, rankable7.getRank());
        assertEquals(1000, rankable8.getRank());

        verifyOrder(rankableList, List.of("urn:1", "urn:2", "urn:3", "urn:4", "urn:5", "urn:6", "urn:7"));

        RankableConnectionUpdater.rank(rankableList, rankable3, 0);
        verifyOrder(rankableList, List.of("urn:3", "urn:1", "urn:2", "urn:4", "urn:5", "urn:6", "urn:7"));

        assertEquals(1, rankable1.getRank());
        assertEquals(2, rankable2.getRank());
        assertEquals(0, rankable3.getRank());
        assertEquals(11, rankable4.getRank());
        assertEquals(12, rankable5.getRank());
        assertEquals(20, rankable6.getRank());
        assertEquals(100, rankable7.getRank());
        assertEquals(1000, rankable8.getRank());

        RankableConnectionUpdater.rank(rankableList, rankable2, 200);
        verifyOrder(rankableList, List.of("urn:3", "urn:1", "urn:4", "urn:5", "urn:6", "urn:7", "urn:2"));

        assertEquals(1, rankable1.getRank());
        assertEquals(200, rankable2.getRank());
        assertEquals(0, rankable3.getRank());
        assertEquals(11, rankable4.getRank());
        assertEquals(12, rankable5.getRank());
        assertEquals(20, rankable6.getRank());
        assertEquals(100, rankable7.getRank());
        assertEquals(1000, rankable8.getRank());

        RankableConnectionUpdater.rank(rankableList, rankable2, 11);
        verifyOrder(rankableList, List.of("urn:3", "urn:1", "urn:2", "urn:4", "urn:5", "urn:6", "urn:7"));

        assertEquals(1, rankable1.getRank());
        assertEquals(11, rankable2.getRank());
        assertEquals(0, rankable3.getRank());
        assertEquals(12, rankable4.getRank());
        assertEquals(13, rankable5.getRank());
        assertEquals(20, rankable6.getRank());
        assertEquals(100, rankable7.getRank());
        assertEquals(1000, rankable8.getRank());

        RankableConnectionUpdater.rank(rankableList, rankable8, 11);
        verifyOrder(rankableList, List.of("urn:3", "urn:1", "urn:8", "urn:2", "urn:4", "urn:5", "urn:6", "urn:7"));

        assertEquals(1, rankable1.getRank());
        assertEquals(12, rankable2.getRank());
        assertEquals(0, rankable3.getRank());
        assertEquals(13, rankable4.getRank());
        assertEquals(14, rankable5.getRank());
        assertEquals(20, rankable6.getRank());
        assertEquals(100, rankable7.getRank());
        assertEquals(11, rankable8.getRank());
    }

    private static class TestRankable implements EntityWithPathConnection {
        private final URI publicId;
        private int rank;
        private Relevance relevance;

        private TestRankable(String publicId, Relevance relevance, int rank) throws URISyntaxException {
            this.publicId = new URI(publicId);
            this.rank = rank;
            this.relevance = relevance;
        }

        @Override
        public URI getPublicId() {
            return publicId;
        }

        @Override
        public int getRank() {
            return rank;
        }

        @Override
        public void setRank(int rank) {
            this.rank = rank;
        }

        @Override
        public Optional<Boolean> isPrimary() {
            return Optional.empty();
        }

        @Override
        public void setPrimary(boolean isPrimary) {

        }

        @Override
        public Optional<EntityWithPath> getConnectedParent() {
            return Optional.empty();
        }

        @Override
        public Optional<EntityWithPath> getConnectedChild() {
            return Optional.empty();
        }

        @Override
        public Optional<Relevance> getRelevance() {
            return Optional.ofNullable(this.relevance);
        }

        @Override
        public void setRelevance(Relevance relevance) {
            this.relevance = relevance;
        }
    }
}