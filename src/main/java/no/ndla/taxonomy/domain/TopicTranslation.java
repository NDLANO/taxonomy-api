/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import javax.persistence.*;

@Entity
public class TopicTranslation implements Translation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @Column
    private String name;

    @Column
    private String languageCode;

    TopicTranslation() {
    }

    public TopicTranslation(Topic topic, String languageCode) {
        setTopic(topic);
        this.languageCode = languageCode;
    }

    public Topic getTopic() {
        return topic;
    }

    public void setTopic(Topic topic) {
        if (topic != this.topic && this.topic != null && this.topic.getTranslations().contains(this)) {
            this.topic.removeTranslation(this);
        }
        this.topic = topic;

        if (topic != null && !topic.getTranslations().contains(this)) {
            topic.addTranslation(this);
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
