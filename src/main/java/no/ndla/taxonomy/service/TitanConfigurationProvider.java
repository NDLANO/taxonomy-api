package no.ndla.taxonomy.service;

import com.thinkaurelius.titan.diskstorage.configuration.backend.CommonsConfiguration;
import com.thinkaurelius.titan.graphdb.configuration.GraphDatabaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.net.URL;
import java.util.Iterator;
import java.util.Properties;

public class TitanConfigurationProvider {
    private static final Logger LOG = LoggerFactory.getLogger(TitanConfigurationProvider.class);

    private String propertyFile;
    private String storageHostname;
    private Properties properties;

    public GraphDatabaseConfiguration load() throws ConfigurationException, FileNotFoundException {
        final PropertiesConfiguration configuration = new PropertiesConfiguration();

        // load property file if provided
        if (propertyFile != null) {
            final URL resource = getClass().getClassLoader().getResource(propertyFile);

            if (null == resource) {
                LOG.error("File 'titan.properties' cannot be found.");
                throw new FileNotFoundException("File 'titan.properties' cannot be found.");
            }

            configuration.load(resource);
        }

        configuration.setProperty("storage.hostname", storageHostname);

        if (StringUtils.isEmpty(properties.getProperty("storage.dynamodb.client.credentials.class-name"))) {
            properties.remove("storage.dynamodb.client.credentials.class-name");
            properties.remove("storage.dynamodb.client.credentials.constructor-args");
        }

        if (properties != null) {
            properties.stringPropertyNames().forEach(prop -> configuration.setProperty(prop, properties.getProperty(prop)));
        }

        LOG.info("Titan configuration: \n" + secureToString(configuration));

        // Warning: calling GraphDatabaseConfiguration constructor results in opening connections to backend storage
        return new GraphDatabaseConfiguration(new CommonsConfiguration(configuration));
    }


    private String secureToString(final Configuration configuration) {
        final Iterator keys = configuration.getKeys();
        final StringBuilder result = new StringBuilder();

        while (keys.hasNext()) {
            final String key = (String) keys.next();
            final Object value = configuration.getProperty(key);
            result.append(key).append("=");

            if (key.contains("password")) {
                result.append("*********");
            } else {
                result.append(value);
            }

            if (keys.hasNext()) {
                result.append("\n");
            }
        }

        return result.toString();
    }


    public void setStorageHostname(final String storageHostname) {
        this.storageHostname = storageHostname;
    }

    /**
     * Titan property file path. The path must point to classpath resource because it is looked up using java.lang
     * .ClassLoader#getResource(java.lang.String).
     */
    public void setPropertyFile(final String propertyFile) {
        this.propertyFile = propertyFile;
    }

    /**
     * Sets property overrides. The properties override any settings from <code>propertyFile</code>.
     */
    public void setProperties(final Properties props) {
        this.properties = props;
    }
}
