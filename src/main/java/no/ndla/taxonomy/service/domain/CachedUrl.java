package no.ndla.taxonomy.service.domain;

import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.net.URI;

@Entity
public class CachedUrl {

    public CachedUrl(URI publicId, String path, boolean primary) {
        this.publicId = publicId;
        this.path = path;
        this.primary = primary;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private int id;

    @Column
    @Type(type = "no.ndla.taxonomy.service.hibernate.UriType")
    private URI publicId;

    @Column
    private String path;

    @Column(name = "is_primary")
    private boolean primary;

    public URI getPublicId() {
        return publicId;
    }

    public void setPublicId(URI publicId) {
        this.publicId = publicId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }
}