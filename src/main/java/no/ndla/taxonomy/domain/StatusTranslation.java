package no.ndla.taxonomy.domain;

import javax.persistence.*;

@Entity
public class StatusTranslation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "status_id")
    private Status status;

    @Column
    private String name;

    @Column
    private String languageCode;

    StatusTranslation() {
    }

    public StatusTranslation(Status status, String languageCode) {
        setStatus(status);
        this.languageCode = languageCode;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        if (status != this.status && this.status != null && this.status.getTranslations().contains(this)) {
            this.status.removeTranslation(this);
        }
        this.status = status;

        if (status != null && !status.getTranslations().contains(this)) {
            status.addTranslation(this);
        }
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
