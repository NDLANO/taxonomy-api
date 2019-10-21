package no.ndla.taxonomy.domain;

import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.net.URI;

@Entity
@Table(name = "cached_url")
@Cacheable(false)
public class CachedUrl {

    public CachedUrl() {
    }

    public CachedUrl(URI publicId, String path) {
        this.publicId = publicId;
        this.path = path;
    }

    @Column
    private int id;

    @Column
    @Type(type = "no.ndla.taxonomy.hibernate.UriType")
    private URI publicId;

    @Id
    @Column
    private String path;

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
}
