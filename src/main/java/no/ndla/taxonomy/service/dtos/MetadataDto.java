package no.ndla.taxonomy.service.dtos;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.HashSet;
import java.util.Set;

public class MetadataDto {
    @JsonIgnore
    private String publicId;
    private Set<String> grepCodes;
    private boolean visible;

    public MetadataDto() {

    }

    public MetadataDto(MetadataApiEntity metadataApiEntity) {
        this.grepCodes = new HashSet<>();
        this.publicId = metadataApiEntity.getPublicId();
        this.visible = metadataApiEntity.isVisible();

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

    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}
