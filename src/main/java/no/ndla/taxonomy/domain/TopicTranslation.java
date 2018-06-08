package no.ndla.taxonomy.domain;


import javax.persistence.*;

@Entity
public class TopicTranslation {
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

    private TopicTranslation() {
    }

    public TopicTranslation(Topic topic, String languageCode) {
        this.topic = topic;
        this.languageCode = languageCode;
    }

    public Topic getTopic() {
        return topic;
    }

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
