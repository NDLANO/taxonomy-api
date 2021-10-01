/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.EntityWithPathConnection;

import java.util.Comparator;
import java.util.List;

public class RankableConnectionUpdater {

    public static <T extends EntityWithPathConnection> List<T> rank(List<T> existingConnections, T updatedConnection, int desiredRank) {
        updatedConnection.setRank(desiredRank);
        if (!existingConnections.isEmpty()) {
            existingConnections.removeIf(subjectTopic -> subjectTopic.getPublicId().equals(updatedConnection.getPublicId()));
            existingConnections.sort(Comparator.comparingInt(EntityWithPathConnection::getRank));
            int newIndex = insertInRankOrder(existingConnections, updatedConnection);
            if (!connectionWasInsertedAtEnd(newIndex)) {
                updateAdjacentRankedConnections(existingConnections, updatedConnection, newIndex + 1);
            }
        } else {
            existingConnections.add(updatedConnection);
        }
        return existingConnections;
    }

    private static boolean connectionWasInsertedAtEnd(int insertedAtIndex) {
        return insertedAtIndex == -1;
    }

    private static <T extends EntityWithPathConnection> void updateAdjacentRankedConnections(List<T> existingConnections, EntityWithPathConnection updatedConnection, int startFromIndex) {
        int lastUpdatedConnectionRank = updatedConnection.getRank();
        for (int i = startFromIndex; i < existingConnections.size(); i++) {
            EntityWithPathConnection currentItem = existingConnections.get(i);
            int currentRank = currentItem.getRank();
            if (currentRank == lastUpdatedConnectionRank) {
                currentItem.setRank(currentRank + 1);
                lastUpdatedConnectionRank = currentRank + 1;
            } else return;
        }
    }

    /**
     * @return the index the connectionToRank was inserted at, or -1 if it was inserted at the end
     */
    private static <T extends EntityWithPathConnection> int insertInRankOrder(List<T> existingConnections, T connectionToRank) {
        for (int i = 0; i < existingConnections.size(); i++) {
            if (existingConnections.get(i).getRank() >= connectionToRank.getRank()) {
                existingConnections.add(i, connectionToRank);
                return i;
            }
        }
        existingConnections.add(connectionToRank);
        return -1;
    }

}
