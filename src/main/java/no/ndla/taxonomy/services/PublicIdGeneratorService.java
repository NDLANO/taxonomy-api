package no.ndla.taxonomy.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.net.URI;
import java.util.UUID;

@Service
public class PublicIdGeneratorService {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    public PublicIdGeneratorService(DataSource dataSource) {
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public URI getNext(String prefix){
        Long sequence = jdbcTemplate.queryForObject("SELECT NEXTVAL('uuid_generator_seq')", Long.class);
        return sequence != null ?
                URI.create(prefix+ ":"+UUID.nameUUIDFromBytes(("SaltOfTheEarth"+sequence).getBytes()))
                : null;
    }
}
