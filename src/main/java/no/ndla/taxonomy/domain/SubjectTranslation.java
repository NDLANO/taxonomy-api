/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import javax.persistence.*;

@Entity
public class SubjectTranslation implements Translation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @Column
    private String name;

    @Column
    private String languageCode;

    SubjectTranslation() {
    }

    public SubjectTranslation(Subject subject, String languageCode) {
        setSubject(subject);
        this.languageCode = languageCode;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        if (subject != this.subject && this.subject != null && this.subject.getTranslations().contains(this)) {
            this.subject.removeTranslation(this);
        }
        this.subject = subject;

        if (subject != null && !subject.getTranslations().contains(this)) {
            subject.addTranslation(this);
        }
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLanguageCode() {
        return languageCode;
    }
}
