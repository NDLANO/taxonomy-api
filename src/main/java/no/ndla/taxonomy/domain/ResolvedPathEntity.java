package no.ndla.taxonomy.domain;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "resolved_path_entity")
public class ResolvedPathEntity {
    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid2")
    private UUID id;

    private String entityType;
    private Integer entityId;

    private Instant urlMapUpdatedAt;

    private Integer update = 0;

    public UUID getId() {
        return id;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    public Optional<Instant> getUrlMapUpdatedAt() {
        return Optional.ofNullable(urlMapUpdatedAt);
    }

    public void setUrlMapUpdatedAt(Instant urlMapUpdatedAt) {
        this.urlMapUpdatedAt = urlMapUpdatedAt;
    }

    public Integer getUpdate() {
        return this.update;
    }
}
