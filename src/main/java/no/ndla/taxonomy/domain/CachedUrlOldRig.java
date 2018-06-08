package no.ndla.taxonomy.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "cached_url_old_rig")
public class CachedUrlOldRig {
    @Id
    @Column
    private int id;

    @Column
    private String oldUrl;

    @Column
    private String newPath;

    public String getOldUrl() {
        return oldUrl;
    }

    public void setOldUrl(String oldUrl) {
        this.oldUrl = oldUrl;
    }

    public String getNewPath() {
        return newPath;
    }

    public void setNewPath(String newPath) {
        this.newPath = newPath;
    }
}
