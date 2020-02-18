package no.ndla.taxonomy.service.dtos;

import no.ndla.taxonomy.domain.MetadataApiEntity;

import java.util.HashSet;
import java.util.Set;

public class MetadataDto {
    private Set<String> competenceAims;

    public MetadataDto() {

    }

    public MetadataDto(MetadataApiEntity metadataApiEntity) {
        this.competenceAims = new HashSet<>();

        metadataApiEntity.getCompetenceAims()
                .stream()
                .map(MetadataApiEntity.CompetenceAim::getCode)
                .forEach(competenceAims::add);
    }

    public Set<String> getCompetenceAims() {
        return competenceAims;
    }

    public void setCompetenceAims(Set<String> competenceAims) {
        this.competenceAims = competenceAims;
    }
}
