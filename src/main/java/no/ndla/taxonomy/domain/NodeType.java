package no.ndla.taxonomy.domain;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.net.URI;
import java.util.UUID;

@Entity
public class NodeType extends DomainObject {
    public NodeType() {
        setPublicId(URI.create("urn:nodetype:" + UUID.randomUUID()));
    }
}
