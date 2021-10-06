package no.ndla.taxonomy.domain;

public enum NodeType {
    SUBJECT("subject"),
    TOPIC("topic");

    private String name;

    NodeType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}