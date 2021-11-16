/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import com.fasterxml.jackson.databind.JsonNode;
import no.ndla.taxonomy.domain.Version;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Map;

public interface VersionGeneratorService {

    @Async
    default void generateJsonForApiEndpoints(Version version, RestTemplate restTemplate, int serverPort) {
        String[] types = { "filters", "relevances", "subjects", "topics", "resources", "nodes",
                "resource-resourcetypes", "resource-types", "subject-topics", "topic-resources", "topic-subtopics" };
        Map<String, String[]> subs = Map.of("subjects",
                new String[] { "metadata", "topics", "resources", "translations" }, "topics",
                new String[] { "metadata", "resources", "connections", "translations" }, "resources",
                new String[] { "metadata", "full", "resource-types", "translations" }, "nodes",
                new String[] { "metadata", "connections", "resources", "nodes" });
        String baseUrl = "http://localhost:" + serverPort;
        try {
            for (String type : types) {
                String path = "/v1/" + type;
                JsonNode response = restTemplate.getForObject(baseUrl + path, JsonNode.class);
                writeFile(version.getPublicId().toString(), path + ".json", response.toString().getBytes(), null);
                response.elements().forEachRemaining(r -> {
                    try {
                        String id = r.get("id").textValue();
                        String subPath = path + "/" + id;
                        JsonNode subResponse = restTemplate.getForObject(baseUrl + subPath, JsonNode.class);
                        writeFile(version.getPublicId().toString(), subPath + ".json",
                                subResponse.toString().getBytes(), null);
                        if (subs.get(type) != null) {
                            for (String sub : subs.get(type)) {
                                String subSubPath = subPath + "/" + sub;
                                JsonNode subSubResponse = restTemplate.getForObject(baseUrl + subSubPath,
                                        JsonNode.class);
                                writeFile(version.getPublicId().toString(), subSubPath + ".json",
                                        subSubResponse.toString().getBytes(), null);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void generateJson(Version version);

    void writeFile(String directory, String filename, byte[] filecontent, String contentType) throws IOException;
}
