package no.ndla.taxonomy.domain;

import java.net.URI;
import java.util.Optional;

public interface EntityWithPathConnection {
    URI getPublicId();

    int getRank();

    void setRank(int rank);

    Optional<Boolean> isPrimary();

    void setPrimary(boolean isPrimary);

    Optional<EntityWithPath> getConnectedParent();

    Optional<EntityWithPath> getConnectedChild();
}
