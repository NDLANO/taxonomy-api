package no.ndla.taxonomy.service.domain;

import java.net.URI;

public interface Rankable {
    URI getPublicId();
    int getRank();
    void setRank(int rank);
}
