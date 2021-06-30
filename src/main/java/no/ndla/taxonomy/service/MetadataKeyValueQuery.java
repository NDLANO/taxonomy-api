package no.ndla.taxonomy.service;

import no.ndla.taxonomy.service.dtos.MetadataDto;

import java.util.List;

public class MetadataKeyValueQuery {
    private String key;
    private String value;
    private List<MetadataDto> dtos;

    public MetadataKeyValueQuery(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public List<MetadataDto> getDtos() {
        return dtos;
    }

    public void setDtos(List<MetadataDto> dtos) {
        this.dtos = dtos;
    }
}
