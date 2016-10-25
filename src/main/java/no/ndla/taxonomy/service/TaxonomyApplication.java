package no.ndla.taxonomy.service;

import com.google.common.collect.Lists;
import com.thinkaurelius.titan.core.PropertyKey;
import com.thinkaurelius.titan.core.VertexLabel;
import com.thinkaurelius.titan.core.schema.SchemaStatus;
import com.thinkaurelius.titan.core.schema.TitanGraphIndex;
import com.thinkaurelius.titan.core.schema.TitanManagement;
import com.thinkaurelius.titan.graphdb.tinkerpop.TitanBlueprintsGraph;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ImportResource;

import java.util.List;


@SpringBootApplication
@ImportResource("classpath:META-INF/applicationContext.xml")
public class TaxonomyApplication {

    public static void main(String[] args) throws InterruptedException {
        ApplicationContext applicationContext = SpringApplication.run(TaxonomyApplication.class, args);
        TitanBlueprintsGraph graph = (TitanBlueprintsGraph) applicationContext.getBean("titanGraph");
        createIndexIfNeeded(graph);

        VertexLabel course = graph.getOrCreateVertexLabel("course");

        Vertex norsk = graph.addVertex(T.label, "course", "name", "norsk");
        Vertex engelsk = graph.addVertex(T.label, "course", "name", "engelsk");

    }

    private static void createIndexIfNeeded(TitanBlueprintsGraph titanGraph) {
        final TitanManagement mgmt = titanGraph.openManagement();
        try {
            System.out.println("Existing vertex labels:  " + mgmt.getVertexLabels().toString());

            final List<TitanGraphIndex> existingIndexes = Lists.newArrayList(mgmt.getGraphIndexes(Vertex.class));

            boolean createIndex = true;
            for (final TitanGraphIndex index : existingIndexes) {
                final SchemaStatus indexStatus = index.getIndexStatus(index.getFieldKeys()[0]);
                System.out.println("Existing indexes: " + index.name() + " indexStatus: " + indexStatus);


                if (index.name().equals("by_indexedProperty") && indexStatus.equals(SchemaStatus.ENABLED)) {
                    createIndex = false;
                }
            }

            if (createIndex) {
                PropertyKey indexedProperty = mgmt.getPropertyKey("indexedProperty");
                if (indexedProperty == null) {
                    indexedProperty = mgmt.makePropertyKey("indexedProperty").dataType(String.class).make();
                }

                mgmt.buildIndex("by_indexedProperty", Vertex.class).addKey(indexedProperty).buildCompositeIndex();
                System.out.println("Creating index: by_indexedProperty");
            }

            mgmt.commit();
            System.out.println("TitanManagement committed");
        } catch (final Exception e) {
            e.printStackTrace();
            mgmt.rollback();
        }
    }
}
