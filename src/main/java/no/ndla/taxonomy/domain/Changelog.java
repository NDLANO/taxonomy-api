/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import java.net.URI;
import java.sql.Timestamp;

@Entity
public class Changelog extends DomainEntity {

    @Column
    private String sourceSchema;

    @Column
    private String destinationSchema;

    @Column
    private Timestamp timestamp;

    @Column
    private boolean cleanUp;

    @Column
    private boolean done;

    public Changelog() {}

    public Changelog(String sourceSchema, String destinationSchema, URI publicId, boolean cleanUp) {
        setPublicId(publicId);
        this.sourceSchema = sourceSchema;
        this.destinationSchema = destinationSchema;
        this.timestamp = new Timestamp(System.currentTimeMillis());
        this.cleanUp = cleanUp;
        this.done = false;
    }

    public String getSourceSchema() {
        return sourceSchema;
    }

    public void setSourceSchema(String sourceSchema) {
        this.sourceSchema = sourceSchema;
    }

    public String getDestinationSchema() {
        return destinationSchema;
    }

    public void setDestinationSchema(String destinationSchema) {
        this.destinationSchema = destinationSchema;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public void setCleanUp(boolean cleanUp) {
        this.cleanUp = cleanUp;
    }

    public boolean isCleanUp() {
        return cleanUp;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public boolean isDone() {
        return done;
    }
}
