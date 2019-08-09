package no.ndla.taxonomy.domain;

import javax.persistence.*;
import java.time.Instant;

@Entity
public class PathAlias {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Integer id;

    @Column(name = "alias")
    protected String alias;

    @Column(name = "orig_path")
    protected String originalPath;

    @Column(name = "root")
    protected String root;

    @Column(name = "leaf")
    protected String leaf;

    @Column(name = "created")
    protected Instant created;

    @OneToOne
    @JoinColumn(name = "replaced")
    protected PathAlias replacedBy;

    public Integer getId() {
        return id;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getOriginalPath() {
        return originalPath;
    }

    public void setOriginalPath(String originalPath) {
        this.originalPath = originalPath;
    }

    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        this.root = root;
    }

    public String getLeaf() {
        return leaf;
    }

    public void setLeaf(String leaf) {
        this.leaf = leaf;
    }

    public Instant getCreated() {
        return created;
    }

    public void setCreated(Instant created) {
        this.created = created;
    }

    public PathAlias getReplacedBy() {
        return replacedBy;
    }

    public void setReplacedBy(PathAlias replacedBy) {
        this.replacedBy = replacedBy;
    }
}
