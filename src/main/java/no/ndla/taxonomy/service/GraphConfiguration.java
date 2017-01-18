package no.ndla.taxonomy.service;

import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import no.ndla.taxonomy.service.domain.*;
import no.ndla.taxonomy.service.migrations.Migration;
import no.ndla.taxonomy.service.migrations.MigrationRunner;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.MapConfiguration;
import org.apache.tinkerpop.gremlin.orientdb.OrientGraph;
import org.apache.tinkerpop.gremlin.orientdb.OrientGraphFactory;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.apache.tinkerpop.gremlin.orientdb.OrientGraph.*;


@Configuration
@ConfigurationProperties(prefix = "graph")
public class GraphConfiguration {
    private static final Logger log = LoggerFactory.getLogger(GraphConfiguration.class);

    private String url, username, password;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    private Map<String, String> properties = new HashMap<>();

    @Bean
    @Profile("!junit")
    public GraphFactory orientGraph() throws Exception {
        final MapConfiguration configuration = new MapConfiguration(properties);
        configuration.setProperty(CONFIG_URL, url);
        configuration.setProperty(CONFIG_USER, username);
        configuration.setProperty(CONFIG_PASS, password);
        log.info("Graph configuration: \n" + secureToString(properties));

        OrientGraphFactory factory = new OrientGraphFactory(configuration).setupPool(-1, 1000);

        try (OrientGraph graph = factory.getNoTx()) {
            createSchema(graph);
        }

        try (OrientGraph graph = factory.getTx(); Transaction transaction = graph.tx()) {
            new MigrationRunner("no.ndla.taxonomy.service.migrations").run(graph);
            transaction.commit();
        }

        return new GraphFactory() {
            @Override
            public Graph create() {
                return factory.getTx();
            }

            @Override
            public boolean isTest() {
                return false;
            }
        };
    }

    @Bean
    @Profile("junit")
    public GraphFactory testOrientGraph() throws Exception {
        OrientGraphFactory factory = new OrientGraphFactory("memory:taxonomy");
        try (OrientGraph graph = factory.getNoTx()) {
            createSchema(graph);
        }

        return new GraphFactory() {
            @Override
            public Graph create() {
                return factory.getTx();
            }

            @Override
            public boolean isTest() {
                return true;
            }
        };
    }

    private String secureToString(Map<String, String> configuration) {
        StringBuilder result = new StringBuilder();

        for (Map.Entry<String, String> entry : configuration.entrySet()) {
            result.append(entry.getKey()).append("=");
            result.append(entry.getKey().contains("password") || entry.getKey().contains(CONFIG_PASS) ? "*********" : entry.getValue());
            result.append("\n");
        }

        return result.toString();
    }

    private void createSchema(OrientGraph graph) {
        graph.createVertexClass(Migration.LABEL);
        graph.createVertexClass(Subject.LABEL);
        graph.createVertexClass(Topic.LABEL);

        createUniqueVertexIndex(graph, Topic.LABEL, "id");
        createUniqueVertexIndex(graph, Subject.LABEL, "id");
        createUniqueVertexIndex(graph, Migration.LABEL, "name");

    }

    private void createUniqueVertexIndex(OrientGraph graph, String label, String key) {
        BaseConfiguration configuration = new BaseConfiguration();
        configuration.setProperty("type", OClass.INDEX_TYPE.UNIQUE.name());
        configuration.setProperty("keytype", OType.STRING);
        Set<String> keys = graph.getVertexIndexedKeys(label);
        log.info("indexes for " + label + ": " + keys);
        if (!keys.contains(key)) graph.createVertexIndex(key, label, configuration);
    }

    private void createUniqueEdgeIndex(OrientGraph graph, String label, String key) {
        BaseConfiguration configuration = new BaseConfiguration();
        configuration.setProperty("type", OClass.INDEX_TYPE.UNIQUE.name());
        configuration.setProperty("keytype", OType.STRING);
        Set<String> keys = graph.getEdgeIndexedKeys(label);
        log.info("indexes for " + label + ": " + keys);
        if (!keys.contains(key)) graph.createEdgeIndex(key, label, configuration);
    }


    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> props) {
        this.properties = new TreeMap<>(props);
    }
}
