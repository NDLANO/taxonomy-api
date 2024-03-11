/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.util;

import org.apache.commons.codec.digest.DigestUtils;

/**
 * Util for generating hashes in different lengths
 */
public class HashUtil {

    private final String hash;

    public HashUtil(Object parentId, Object connectionId) {
        this.hash = generateHash(parentId, connectionId);
    }

    public HashUtil(String hash) {
        this.hash = hash;
    }

    public String shortHash() {
        return hash.substring(0, 4);
    }

    public String mediumHash() {
        return hash.substring(0, 8);
    }

    public String semiHash() {
        return hash.substring(0, 12);
    }

    public String longHash() {
        return hash.substring(0, 16);
    }

    public String fullHash() {
        return hash;
    }

    private String generateHash(Object part1, Object part2) {
        return new DigestUtils("SHA3-256").digestAsHex(part1.toString() + part2.toString());
    }
}
