package no.ndla.taxonomy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;

/**
 *
 */
@Component
public class CachedUrlUpdater {

    private JdbcTemplate jdbcTemplate;
    private Timestamp lastUpdate = Timestamp.valueOf(LocalDateTime.now().minusDays(1));

    @Autowired
    public CachedUrlUpdater(DataSource dataSource) {
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Scheduled(fixedRate = 30000)
    public void updateCacheIfNeccessary() {
        ArrayList<String> changedTables = new ArrayList<>();
        jdbcTemplate.query("SELECT * FROM FRESHNESS WHERE resource IN ('subject_topic', 'topic_subtopic', 'topic_resource')", new Object[]{},
                resultSet -> {
                    String resource = resultSet.getString("resource");
                    Timestamp lastModified = resultSet.getTimestamp("last_modified");
                    if (lastModified.after(lastUpdate)) {
                        changedTables.add(resource);
                    }
                }
        );
        if(!changedTables.isEmpty()) {
            System.out.println("CACHED URL is stale because of changes in "+changedTables.toString());
            jdbcTemplate.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY cached_url;");
            lastUpdate = Timestamp.valueOf(LocalDateTime.now());
            System.out.println("Cache updated");
        } else {
            System.out.println("Cached URL is up to date");
        }
    }

}
