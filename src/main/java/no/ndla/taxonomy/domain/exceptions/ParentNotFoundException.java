/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain.exceptions;

import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.Resource;
import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@ResponseStatus(NOT_FOUND)
public class ParentNotFoundException extends RuntimeException {
    public ParentNotFoundException(Node child, Node parent) {
        super("Node with id " + child.getPublicId() + " has no parent with id " + parent.getPublicId());
    }

    public ParentNotFoundException(Resource resource, Node node) {
        super("Resource with id " + resource.getPublicId() + " has no parent with id " + node.getPublicId());
    }
}
