package no.ndla.taxonomy.domain;

import java.util.Objects;

public class TaxonomyApiRequest {

    private String method;
    private String path;
    private String body;
    private String timestamp;


    public TaxonomyApiRequest(String method, String path, String body, String timestamp) {
        this.method = method;
        this.path = path;
        this.body = body;
        this.timestamp = timestamp;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public String getBody() {
        return body;
    }

    public String getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaxonomyApiRequest that = (TaxonomyApiRequest) o;
        return Objects.equals(method, that.method) &&
                Objects.equals(path, that.path) &&
                Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(method, path, timestamp);
    }

    @Override
    public String toString() {
        return "TaxonomyApiRequest{" +
                "method='" + method + '\'' +
                ", path='" + path + '\'' +
                ", body='" + body + '\'' +
                ", timestamp='" + timestamp + '\'' +
                '}';
    }
}
