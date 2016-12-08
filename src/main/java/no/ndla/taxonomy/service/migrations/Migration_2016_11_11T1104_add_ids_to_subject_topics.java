package no.ndla.taxonomy.service.migrations;

public class Migration_2016_11_11T1104_add_ids_to_subject_topics extends Migration {
    @Override
    public void run() throws Exception {
        graph.traversal().E().hasLabel("subject-has-topics").hasNot("id")
                .forEachRemaining(e -> e.property("id", ("urn:subject-topic:" + e.id())));
    }
}
