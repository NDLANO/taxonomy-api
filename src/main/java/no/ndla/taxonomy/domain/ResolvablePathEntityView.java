package no.ndla.taxonomy.domain;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.Instant;
import java.util.Optional;

@Entity
@Table(name = "resolvable_path_entity")
public class ResolvablePathEntityView {
    @Id
    private Integer entityId;

    private String entityType;

    private Instant updatedAt;

    private Instant urlMapUpdatedAt;

    private Integer update;

    public Integer getEntityId() {
        return entityId;
    }

    public String getEntityType() {
        return entityType;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Optional<Instant> getUrlMapUpdatedAt() {
        return Optional.ofNullable(urlMapUpdatedAt);
    }

    public Optional<Integer> getUpdate() {
        return Optional.ofNullable(update);
    }
}
