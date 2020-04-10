package no.ndla.taxonomy.service.dtos;

import no.ndla.taxonomy.domain.MetadataApiEntity;

import java.util.HashSet;
import java.util.Set;

public class MetadataDto {
    private Set<String> grepCodes;

    public MetadataDto() {

    }

    public MetadataDto(MetadataApiEntity metadataApiEntity) {
        this.grepCodes = new HashSet<>();

        metadataApiEntity.getCompetenceAims()
                .stream()
                .map(MetadataApiEntity.CompetenceAim::getCode)
                .forEach(grepCodes::add);
    }

    public Set<String> getGrepCodes() {
        return grepCodes;
    }

    public void setGrepCodes(Set<String> competenceAims) {
        this.grepCodes = competenceAims;
    }
}
