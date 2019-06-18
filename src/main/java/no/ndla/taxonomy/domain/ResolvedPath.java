package no.ndla.taxonomy.domain;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.io.Serializable;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;

@Entity
public class ResolvedPath implements Serializable {
    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid2")
    private UUID id;

    @Type(type = "no.ndla.taxonomy.hibernate.UriType")
    private URI publicId;

    private String path;

    private boolean isPrimary = false;

    @Type(type = "no.ndla.taxonomy.hibernate.UriType")
    private URI parentPublicId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_entity_id")
    private ResolvedPathEntity updatedEntity;

    public UUID getId() {
        return id;
    }

    public URI getPublicId() {
        return publicId;
    }

    public void setPublicId(URI publicId) {
        this.publicId = publicId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    public void setPrimary(boolean primary) {
        isPrimary = primary;
    }

    public URI getParentPublicId() {
        return parentPublicId;
    }

    public void setParentPublicId(URI parentPublicId) {
        this.parentPublicId = parentPublicId;
    }

    public Optional<ResolvedPathEntity> getUpdatedEntity() {
        return Optional.ofNullable(updatedEntity);
    }

    public void setUpdatedEntity(ResolvedPathEntity updatedEntity) {
        this.updatedEntity = updatedEntity;
    }
}
