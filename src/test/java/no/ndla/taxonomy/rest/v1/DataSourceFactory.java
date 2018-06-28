package no.ndla.taxonomy.rest.v1;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.*;
import org.springframework.core.type.AnnotatedTypeMetadata;

import javax.sql.DataSource;
import java.io.IOException;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Configuration
public class DataSourceFactory {
    Logger logger = LoggerFactory.getLogger(DataSourceFactory.class);

    @Bean
    @Conditional(UseEmbeddedPostgres.class)
    public DataSource dataSource() throws IOException {
        EmbeddedPostgres pg = EmbeddedPostgres.builder()
                .setLocaleConfig("locale", "en_US.UTF-8")
                .start();
        logger.info("Opened embedded database on jdbc:postgresql://localhost:" + pg.getPort() + "/postgres");
        return pg.getPostgresDatabase();
    }

    public static class UseEmbeddedPostgres implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            String embedded = context.getEnvironment().getProperty("embedded");
            return isBlank(embedded) || !embedded.equals("false");
        }
    }
}
