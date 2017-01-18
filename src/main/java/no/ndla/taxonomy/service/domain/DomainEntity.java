package no.ndla.taxonomy.service.domain;

import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import java.net.URI;

@MappedSuperclass
public class DomainEntity {
    @Id
    @GeneratedValue
    private Integer id;

    @Column(name = "public_id")
    @Type(type = "no.ndla.taxonomy.service.domain.UriType")
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
