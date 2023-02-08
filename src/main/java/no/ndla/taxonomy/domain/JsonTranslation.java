package no.ndla.taxonomy.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class JsonTranslation implements Serializable, Translation {
    @JsonProperty("name")
    String name;

    @JsonProperty("languageCode")
    String languageCode;

    @JsonIgnore
    Translatable parent;

    public JsonTranslation(JsonTranslation js) {
        this.name = js.getName();
        this.languageCode = js.getLanguageCode();
    }

    public JsonTranslation(String languageCode) {
        this.languageCode = languageCode;
    }

    public JsonTranslation(String name, String languageCode) {
        this.name = name;
        this.languageCode = languageCode;
    }

    public JsonTranslation() {
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getLanguageCode() {
        return this.languageCode;
    }

    @JsonIgnore
    public Translatable getNode() {
        return this.parent;
    }

    @JsonIgnore
    public void setNode(Translatable parent) {
        this.parent = parent;
    }
}
