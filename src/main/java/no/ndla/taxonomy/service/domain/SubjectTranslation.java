package no.ndla.taxonomy.service.domain;


import javax.persistence.*;

@Entity
public class SubjectTranslation {
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

    private SubjectTranslation() {
    }

    public SubjectTranslation(Subject subject, String languageCode) {
        this.subject = subject;
        this.languageCode = languageCode;
    }

    public Subject getSubject() {
        return subject;
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
