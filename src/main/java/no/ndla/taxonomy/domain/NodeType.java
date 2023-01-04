/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

public enum NodeType {
    NODE("node"), SUBJECT("subject"), TOPIC("topic"), RESOURCE("resource");

    private String name;

    NodeType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
