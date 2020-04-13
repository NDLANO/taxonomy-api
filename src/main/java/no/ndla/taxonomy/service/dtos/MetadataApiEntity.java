package no.ndla.taxonomy.service.dtos;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class MetadataApiEntity {
    private String publicId;
    private Set<CompetenceAim> competenceAims = new HashSet<>();
    private boolean visible;

    public MetadataApiEntity() {

    }

    public MetadataApiEntity(MetadataDto entityMetadataObject) {
        entityMetadataObject.getGrepCodes().forEach(aim -> addCompetenceAim(new CompetenceAim(aim)));
        this.visible = entityMetadataObject.isVisible();
    }

    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    public Set<CompetenceAim> getCompetenceAims() {
        return competenceAims.stream().collect(Collectors.toUnmodifiableSet());
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    void setCompetenceAims(Set<CompetenceAim> competenceAims) {
        this.competenceAims = competenceAims;
    }

    public void addCompetenceAim(CompetenceAim competenceAim) {
        this.competenceAims.add(competenceAim);
    }

    public void removeCompetenceAim(CompetenceAim competenceAim) {
        this.competenceAims.remove(competenceAim);
    }

    public static class CompetenceAim {
        private String code;

        CompetenceAim() {

        }

        CompetenceAim(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }
    }
}
