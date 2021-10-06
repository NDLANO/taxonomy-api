/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import javax.persistence.*;
import java.io.Serializable;
import java.net.URI;

@MappedSuperclass
public class DomainEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column
    private URI publicId;

    public Integer getId() {
        return id;
    }

    public URI getPublicId() {
        return publicId;
    }

    public void setPublicId(URI publicId) {
        this.publicId = publicId;
    }

    /**
     * Defaults to class name, but can be overrided
     * @return
     */
    public String getEntityName() {
        return getClass().getSimpleName().toLowerCase();
    }
}
