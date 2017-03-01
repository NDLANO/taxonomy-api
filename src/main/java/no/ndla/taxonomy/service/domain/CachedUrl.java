package no.ndla.taxonomy.service.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class CachedUrl {
    @Id
    @GeneratedValue
    @Column(name = "id")
    private int id;

    @Column(name = "public_id")
    private String publicId;

    @Column
    private String path;

    @Column(name = "is_primary")
    private boolean primary;

}
