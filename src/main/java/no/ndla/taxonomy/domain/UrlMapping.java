package no.ndla.taxonomy.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.net.URI;

@Entity
@Table(name = "cached_url_old_rig")
public class UrlMapping {
    @Id
    @Column
    private String oldUrl;

    @Column
    private String public_id;

    @Column
    private String subject_id;

    public String getOldUrl() {
        return oldUrl;
    }

    public void setOldUrl(String oldUrl) {
        this.oldUrl = oldUrl;
    }

    public URI getPublic_id() {
        return URI.create(public_id);
    }

    public void setPublic_id(URI public_id) {
        this.public_id = public_id.toString();
    }

    public void setPublic_id(String public_id) {
        this.public_id = URI.create(public_id).toString(); //if not valid URI - will force an IllegalArgumentException
    }

    public URI getSubject_id() {
        return subject_id != null ? URI.create(subject_id) : null;
    }

    public void setSubject_id(URI subject_id) {
        this.subject_id = subject_id.toString();
    }

    public void setSubject_id(String subject_id) {
        this.subject_id = URI.create(subject_id).toString(); //if not valid URI - will force an IllegalArgumentException
    }
}
