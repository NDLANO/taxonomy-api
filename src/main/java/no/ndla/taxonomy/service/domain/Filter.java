package no.ndla.taxonomy.service.domain;


import javax.persistence.Entity;
import java.net.URI;
import java.util.UUID;

@Entity
public class Filter extends DomainObject {

    public Filter() {
        setPublicId(URI.create("urn:filter:" + UUID.randomUUID()));
    }


    public Filter name(String name) {
        setName(name);
        return this;
    }
}