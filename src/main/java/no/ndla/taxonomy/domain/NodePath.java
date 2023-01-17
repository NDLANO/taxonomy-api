package no.ndla.taxonomy.domain;

import no.ndla.taxonomy.rest.v1.dtos.nodes.searchapi.LanguageField;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static no.ndla.taxonomy.service.dtos.SearchableTaxonomyContextDTOFactory.DefaultLanguage;

/**
 * Helper class to build paths of nodes :^)
 */
public class NodePath {
    ArrayList<Node> nodes = new ArrayList<>();
    ArrayList<NodeConnection> nodeConnections = new ArrayList<>();

    public NodePath() {

    }

    public NodePath(NodePath nodePath) {
        this.nodeConnections.addAll(nodePath.nodeConnections);
        this.nodes.addAll(nodePath.nodes);
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        for (var node : nodes) {
            str.append("/").append(node.getPublicId().toString().replaceFirst("urn:", ""));
        }
        return str.toString();
    }

    public NodePath add(Node node, Optional<NodeConnection> maybeConnection) {
        var newOne = new NodePath(this);
        newOne.nodes.add(0, node);

        if (this.nodes.size() > 1 && maybeConnection.isEmpty()) {
            throw new IllegalStateException("Tried to add non-base node without connection to nodePath");
        }

        maybeConnection.ifPresent(nodeConnection -> {
            newOne.nodeConnections.add(0, nodeConnection);
        });

        return newOne;
    }

    public List<Node> getNodes() {
        return this.nodes;
    }

    private List<Node> getBreadcrumbNodes() {
        var breadcrumbNodes = new ArrayList<Node>();
        for (var i = 0; i < this.nodes.size() - 1; ++i) {
            breadcrumbNodes.add(this.nodes.get(i));
        }
        return breadcrumbNodes;
    }

    public LanguageField<List<String>> getBreadcrumbs() {
        var breadcrumbNodes = getBreadcrumbNodes();
        var breadcrumbs = new LanguageField<List<String>>();
        var allLanguages = breadcrumbNodes.stream()
                .flatMap(n -> n.getTranslations().stream().map(NodeTranslation::getLanguageCode))
                .collect(Collectors.toSet());
        allLanguages.add(DefaultLanguage);

        for (var lang : allLanguages) {
            var crumbsForLanguage = new ArrayList<String>();
            breadcrumbNodes.forEach(n -> {
                var translatedName = n.getTranslation(lang).map(NodeTranslation::getName).orElse(n.getName());
                crumbsForLanguage.add(translatedName);
            });
            breadcrumbs.put(lang, crumbsForLanguage);
        }

        return breadcrumbs;
    }

    public NodeConnection getBaseConnection() {
        return this.nodeConnections.get(this.nodeConnections.size() - 1);
    }

    public Optional<Relevance> getBaseRelevance() {
        var connection = this.getBaseConnection();
        return connection.getRelevance();
    }

    public boolean isVisible() {
        for (var node : this.nodes) {
            if (!node.getMetadata().isVisible()) {
                return false;
            }
        }
        return true;
    }

    public List<Node> withoutBase() {
        return this.nodes.stream().limit(this.nodes.size() - 1).toList();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        var that = (NodePath) o;
        return Objects.equals(this.nodes, that.nodes) && Objects.equals(this.nodeConnections, that.nodeConnections);
    }
}
