package no.ndla.taxonomy.domain;

import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.net.URI;

@Entity
@Table(name = "cached_url")
public class CachedUrl {

    public CachedUrl() {
    }

    public CachedUrl(URI publicId, String path, boolean primary) {
        this.publicId = publicId;
        this.path = path;
        this.primary = primary;
    }

    @Id
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

    @Override
    public String toString() {
        return "CachedUrl{" +
                "publicId=" + publicId +
                ", path='" + path + '\'' +
                ", primary=" + primary +
                '}';
    }
}
