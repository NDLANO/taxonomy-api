package no.ndla.taxonomy.service;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Course {
    @JsonProperty
    private Object id;

    @JsonProperty
    private String name;


    public Object getId() {
        return id;
    }

    public void setId(Object id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Course name(String name) {
        this.name = name;
        return this;
    }

    public Course id(Object id) {
        this.id = id;
        return this;
    }
}
