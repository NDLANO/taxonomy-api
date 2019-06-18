package no.ndla.taxonomy.domain;

import java.net.URI;
import java.util.Optional;

public class GeneratedPath {
    private String path;
    private boolean isPrimary;
    private URI parentId;

    private GeneratedPath(String path, URI parentId, boolean isPrimary) {
        this.path = path;
        this.parentId = parentId;
        this.isPrimary = isPrimary;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getPath() {
        return path;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    public Optional<URI> getParentId() {
        return Optional.ofNullable(parentId);
    }

    public static class Builder {
        private GeneratedPath parentPath;
        private String subPath;
        private boolean isPrimary = false;
        private URI parentId;

        private Builder() {

        }

        private Optional<GeneratedPath> getParentPath() {
            return Optional.ofNullable(parentPath);
        }

        public Builder setParentPath(GeneratedPath parentPath) {
            this.parentPath = parentPath;
            return this;
        }

        public Builder setSubPath(String subPath) {
            this.subPath = subPath;
            return this;
        }

        public Builder setIsPrimary(boolean isPrimary) {
            this.isPrimary = isPrimary;
            return this;
        }

        public Builder setParentId(URI parentId) {
            this.parentId = parentId;

            return this;
        }

        public GeneratedPath build() {
            return new GeneratedPath(
                    this.getParentPath().map(GeneratedPath::getPath).orElse("") + "/" + this.subPath,
                    parentId,
                    isPrimary
            );
        }
    }
}
