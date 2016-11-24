package no.ndla.taxonomy.service;

import no.ndla.taxonomy.service.rest.TopicResource;
import no.ndla.taxonomy.service.rest.TopicSubtopicResource;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Scanner;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class Import {

    RestTemplate restTemplate = new RestTemplate();

    @Test
    @Ignore
    public void name() throws Exception {
        Scanner file = new Scanner(getClass().getClassLoader().getResourceAsStream("import.tsv"));

        URI parentUri = null;

        while (file.hasNextLine()) {
            String line = file.nextLine();
            String[] columns = line.split("\t");
            if (columns.length < 11) continue;

            String type = columns[10];
            if (!"N".equals(type)) continue;

            Integer id;
            String name;

            if (isNotBlank(columns[4])) {
                //new parent topic
                id = getInt(columns[1]);
                name = columns[4];
                System.out.println(id + ":\t" + name);
                parentUri = createTopic(id, name);
            } else {
                //new child topic
                id = getInt(columns[2]);
                name = getString(columns[5]).replaceAll("Emne.*:", "").trim();
                System.out.println("id:     " + id + ":\t" + name);
                URI uri = createTopic(id, name);
                addSubtopic(parentUri, uri);
            }
        }
    }

    private URI addSubtopic(URI topicid, URI subtopicid) {
        TopicSubtopicResource.AddSubtopicToTopicCommand cmd = new TopicSubtopicResource.AddSubtopicToTopicCommand();
        cmd.topicid = topicid;
        cmd.subtopicid = subtopicid;
        System.out.println("topicid: " + cmd.topicid + " subtopicid: " + subtopicid);

        URI uri = restTemplate.postForLocation("http://localhost:5000/topic-subtopics", cmd);
        System.out.println("Added subtopic: " + uri);
        return uri;
    }

    private URI createTopic(Integer id, String name) {
        TopicResource.CreateTopicCommand cmd = new TopicResource.CreateTopicCommand();
        if (null != id) cmd.id = URI.create("urn:topic:" + id);
        cmd.name = name;

        URI uri = restTemplate.postForLocation("http://localhost:5000/topics", cmd);
        URI topicid = URI.create(uri.toString().substring(uri.toString().lastIndexOf("/") + 1));
        System.out.println("created: " + topicid);
        return topicid;
    }

    private static Integer getInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return null;
        }
    }

    private static String getString(String value) {
        return value == null ? "" : value;
    }
}
