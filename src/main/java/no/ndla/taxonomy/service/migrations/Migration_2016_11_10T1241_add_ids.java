package no.ndla.taxonomy.service.migrations;

public class Migration_2016_11_10T1241_add_ids extends Migration {
    @Override
    public void run() throws Exception {
        transaction.traversal().V().hasLabel("topic").hasNot("id").forEachRemaining(v -> v.property("id", ("urn:topic:" + v.id())));
        transaction.traversal().V().hasLabel("subject").hasNot("id").forEachRemaining(v -> v.property("id", "urn:subject:" + v.id()));
    }
}
