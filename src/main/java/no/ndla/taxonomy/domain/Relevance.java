/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.Type;

@Entity
public class Relevance extends DomainObject {

    @Type(JsonBinaryType.class)
    @Column(name = "translations", columnDefinition = "jsonb")
    protected List<JsonTranslation> translations = new ArrayList<>();

    public Relevance() {
        setPublicId(URI.create("urn:relevance:" + UUID.randomUUID()));
    }

    public JsonTranslation addTranslation(String languageCode) {
        var relevanceTranslation = getTranslation(languageCode).orElse(null);
        if (relevanceTranslation != null) return relevanceTranslation;

        relevanceTranslation = new JsonTranslation(languageCode);
        translations.add(relevanceTranslation);
        return relevanceTranslation;
    }

    @Override
    public List<JsonTranslation> getTranslations() {
        return this.translations;
    }

    @Override
    public void setTranslations(List<JsonTranslation> translations) {
        this.translations = translations;
    }
}
