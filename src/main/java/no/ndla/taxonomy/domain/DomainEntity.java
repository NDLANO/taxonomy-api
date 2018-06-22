package no.ndla.taxonomy.domain;

import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.net.URI;

@MappedSuperclass
public class DomainEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column
    @Type(type = "no.ndla.taxonomy.hibernate.UriType")
    private URI publicId;

    protected Integer getId() {
        return id;
    }

    public URI getPublicId() {
        return publicId;
    }

    public void setPublicId(URI publicId) {
        this.publicId = publicId;
    }
}
