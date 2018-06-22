package no.ndla.taxonomy.service;

import no.ndla.taxonomy.domain.SubjectTopic;

import java.util.Comparator;
import java.util.List;

public class SubjectTopicRankUpdater {

    public static List<SubjectTopic> rank(List<SubjectTopic> existingConnections, SubjectTopic updatedConnection, int desiredRank) {
        updatedConnection.setRank(desiredRank);
        if (!existingConnections.isEmpty()) {
            existingConnections.removeIf(subjectTopic -> subjectTopic.getPublicId().equals(updatedConnection.getPublicId()));
            existingConnections.sort(Comparator.comparingInt(SubjectTopic::getRank));
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

    private static void updateAdjacentRankedConnections(List<SubjectTopic> existingConnections, SubjectTopic updatedConnection, int startFromIndex) {
        int lastUpdatedConnectionRank = updatedConnection.getRank();
        for (int i = startFromIndex; i < existingConnections.size(); i++) {
            SubjectTopic currentItem = existingConnections.get(i);
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
    private static int insertInRankOrder(List<SubjectTopic> existingConnections, SubjectTopic connectionToRank) {
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
