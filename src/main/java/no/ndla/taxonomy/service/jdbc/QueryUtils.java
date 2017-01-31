package no.ndla.taxonomy.service.jdbc;

import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.PreparedStatementSetter;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Scanner;

public class QueryUtils {
    public static PreparedStatementSetter setQueryParameters(final List<Object> args) {
        return statement -> {
            for (int i = 0; i < args.size(); i++) {
                statement.setObject(i + 1, args.get(i));
            }
        };
    }

    public static URI toURI(String uri) {
        if (uri == null) return null;
        return URI.create(uri);
    }

    public static String getQuery(String name) {
        String path = "/db/queries/" + name + ".sql";
        try (
                InputStream inputStream = new ClassPathResource(path, QueryUtils.class.getClassLoader()).getInputStream()
        ) {
            return new Scanner(inputStream).useDelimiter("\\Z").next();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
