package no.ndla.taxonomy;

import no.ndla.taxonomy.rest.v1.SubjectTopics;
import no.ndla.taxonomy.rest.v1.TopicSubtopics;
import no.ndla.taxonomy.rest.v1.command.subjects.CreateSubjectCommand;
import no.ndla.taxonomy.rest.v1.command.topics.CreateTopicCommand;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Scanner;

public class Import {

    RestTemplate restTemplate = new RestTemplate();

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

    private static boolean isNotBlank(String column) {
        return null != column && column.trim().length() > 0;
    }

    @Test
    @Ignore
    public void name() throws Exception {
        Scanner file = new Scanner(getClass().getClassLoader().getResourceAsStream("import.tsv"));

        URI parentUri = null;
        URI subjectUri = createSubject(1, "Medieuttrykk og mediesamfunnet");

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
                addTopic(subjectUri, parentUri);
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
        TopicSubtopics.AddSubtopicToTopicCommand cmd = new TopicSubtopics.AddSubtopicToTopicCommand();
        cmd.topicid = topicid;
        cmd.subtopicid = subtopicid;
        System.out.println("topicid: " + cmd.topicid + " subtopicid: " + subtopicid);

        URI uri = restTemplate.postForLocation("http://localhost:5000/topic-subtopics", cmd);
        System.out.println("Added subtopic: " + uri);
        return uri;
    }

    private URI createTopic(Integer id, String name) {
        CreateTopicCommand cmd = new CreateTopicCommand();
        if (null != id) cmd.id = URI.create("urn:topic:" + id);
        cmd.name = name;

        URI uri = restTemplate.postForLocation("http://localhost:5000/topics", cmd);
        URI topicid = URI.create(uri.toString().substring(uri.toString().lastIndexOf("/") + 1));
        System.out.println("created: " + topicid);
        return topicid;
    }

    private URI createSubject(Integer id, String name) {
        CreateSubjectCommand cmd = new CreateSubjectCommand();
        if (null != id) cmd.id = URI.create("urn:subject:" + id);
        cmd.name = name;

        URI uri = restTemplate.postForLocation("http://localhost:5000/subjects", cmd);
        URI subjectid = URI.create(uri.toString().substring(uri.toString().lastIndexOf("/") + 1));
        System.out.println("created: " + subjectid);
        return subjectid;
    }

    private URI addTopic(URI subjectid, URI topicid) {
        SubjectTopics.AddTopicToSubjectCommand cmd = new SubjectTopics.AddTopicToSubjectCommand();
        cmd.subjectid = subjectid;
        cmd.topicid = topicid;
        System.out.println("Subjectid: " + cmd.subjectid + " topicid: " + cmd.topicid);

        URI uri = restTemplate.postForLocation("http://localhost:5000/subject-topics", cmd);
        System.out.println("Added subject-topic: " + uri);
        return uri;
    }
}
