package no.ndla.taxonomy.service;

import com.thinkaurelius.titan.core.TitanFactory;
import com.thinkaurelius.titan.core.TitanGraph;
import com.thinkaurelius.titan.core.TitanTransaction;
import com.thinkaurelius.titan.diskstorage.configuration.backend.CommonsConfiguration;
import com.thinkaurelius.titan.graphdb.configuration.GraphDatabaseConfiguration;
import com.thinkaurelius.titan.graphdb.database.StandardTitanGraph;
import no.ndla.taxonomy.service.migrations.MigrationRunner;
import org.apache.commons.configuration.MapConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.Map;
import java.util.TreeMap;


@Configuration
@ConfigurationProperties(prefix = "titan")
public class TitanConfiguration {
    private static final Logger log = LoggerFactory.getLogger(TitanConfiguration.class);
    private Map<String, String> properties;

    @Bean
    @Profile("!junit")
    public TitanGraph titanGraph() {
        final MapConfiguration configuration = new MapConfiguration(properties);
        log.info("Titan configuration: \n" + secureToString(properties));
        StandardTitanGraph graph = new StandardTitanGraph(new GraphDatabaseConfiguration(new CommonsConfiguration(configuration)));

        try (TitanTransaction transaction = graph.newTransaction()) {
            new MigrationRunner("no.ndla.taxonomy.service.migrations").run(transaction);
            transaction.commit();
        }

        return graph;
    }

    @Bean
    @Profile("junit")
    public TitanGraph testTitanGraph() {
        return TitanFactory.build().set("storage.backend", "inmemory").open();
    }

    private String secureToString(Map<String, String> configuration) {
        StringBuilder result = new StringBuilder();

        for (Map.Entry<String, String> entry : configuration.entrySet()) {
            result.append(entry.getKey()).append("=");
            result.append(entry.getKey().contains("password") ? "*********" : entry.getValue());
            result.append("\n");
        }

        return result.toString();
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> props) {
        this.properties = new TreeMap<>(props);
    }
}
