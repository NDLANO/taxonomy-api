package no.ndla.taxonomy.service.rest.v1;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.context.annotation.*;
import org.springframework.core.type.AnnotatedTypeMetadata;

import javax.sql.DataSource;
import java.io.IOException;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Configuration
public class DataSourceFactory {
    @Bean
    @Conditional(UseEmbeddedPostgres.class)
    public DataSource dataSource() throws IOException {
        EmbeddedPostgres pg = EmbeddedPostgres.builder()
                .setLocaleConfig("locale", "no_NO.UTF-8")
                .start();
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
