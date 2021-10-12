/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import java.util.Optional;

@MappedSuperclass
public abstract class DomainObject extends DomainEntity {
    @Column
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    abstract public Optional<? extends Translation> getTranslation(String languageCode);
}
