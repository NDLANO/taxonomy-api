package no.ndla.taxonomy.service;

import no.ndla.taxonomy.service.domain.SubjectTopic;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 *
 */
public class SubjectTopicRankUpdater {


    public List<SubjectTopic> rank(List<SubjectTopic> existing, SubjectTopic connectionToRank, int desiredRank) {

        connectionToRank.setRank(desiredRank);

        if (!existing.isEmpty()) {
            existing.sort(Comparator.comparingInt(SubjectTopic::getRank));
            //does the connection exist already?
            boolean existedAlready = existing.removeIf(subjectTopic -> subjectTopic.getPublicId().equals(connectionToRank.getPublicId()));
            int insertedAt = -1;
            for (int i = 0; i < existing.size(); i++) {
                if (existing.get(i).getRank() >= desiredRank) {
                    existing.add(i, connectionToRank);
                    insertedAt = i;
                    break;
                }
            }
            if (insertedAt == -1) { //the rank is higher than any existing
                existing.add(connectionToRank);
            } else { //the rank was lower or equal to an existing connection
                //is the following item at the same rank?
                SubjectTopic nextItem = existing.get(insertedAt + 1);
                if(nextItem.getRank() == desiredRank){
                    //update following items until no longer contiguous or end of list
                    System.out.println("Must update items after index "+insertedAt);
                    int rankOfLastUpdated = desiredRank;
                    for(int i = (insertedAt+1); i < existing.size(); i++){
                        SubjectTopic currentItem = existing.get(i);
                        int currentRank = currentItem.getRank();
                        if(currentRank - rankOfLastUpdated == 0){
                            currentItem.setRank(currentRank+1);
                            rankOfLastUpdated = currentRank+1;
                        }
                    }
                }
            }

        } else {
            existing.add(connectionToRank);
        }
        return existing;
    }

}
