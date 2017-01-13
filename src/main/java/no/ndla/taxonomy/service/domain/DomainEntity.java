package no.ndla.taxonomy.service.domain;

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

    @Column
    private URI publicId;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public URI getPublicId() {
        return publicId;
    }

    public void setPublicId(URI publicId) {
        this.publicId = publicId;
    }
}
