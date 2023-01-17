package no.ndla.taxonomy.service.dtos;

import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.repositories.RelevanceRepository;
import no.ndla.taxonomy.rest.v1.dtos.nodes.searchapi.*;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.*;

@Service
public class SearchableTaxonomyContextDTOFactory {
    private final RelevanceRepository relevanceRepository;

    public SearchableTaxonomyContextDTOFactory(RelevanceRepository relevanceRepository) {
        this.relevanceRepository = relevanceRepository;
    }

    public List<SearchableTaxonomyContextDTO> fromNodes(List<Node> nodes, boolean filterVisibles) {
        return nodes.stream().flatMap(node -> {
            return node.buildPaths().stream().map(np -> {
                return fromNodeAndPath(node, np, filterVisibles);
            }).filter(Optional::isPresent).map(Optional::get);
        }).toList();
    }

    public Optional<SearchableTaxonomyContextDTO> fromNodeAndPath(Node node, NodePath nodePath,
            boolean filterVisibles) {
        if (filterVisibles && !nodePath.isVisible())
            return Optional.empty();

        var maybeSubjectNode = nodePath.getNodes().stream().findFirst()
                .filter(s -> s.getPublicId() != node.getPublicId());
        if (maybeSubjectNode.isEmpty())
            return Optional.empty();

        var subjectNode = maybeSubjectNode.get();
        var subjectNames = LanguageField.nameFromNode(subjectNode);
        var path = nodePath.toString();

        var relevance = nodePath.getBaseRelevance().orElseGet(() -> {
            var coreId = URI.create("urn:relevance:core");
            return relevanceRepository.findFirstByPublicIdIncludingTranslations(coreId).orElseThrow();
        });

        var relevanceTranslations = new LanguageField<String>();
        relevanceTranslations.put(DefaultLanguage, relevance.getName());
        relevance.getTranslations()
                .forEach(trans -> relevanceTranslations.put(trans.getLanguageCode(), trans.getName()));

        var relevanceId = nodePath.getBaseRelevance().map(DomainEntity::getPublicId)
                .orElse(URI.create("urn:relevance:core"));
        var resourceTypes = node.getResourceTypes().stream().map(SearchableTaxonomyResourceType::new).toList();
        var isPrimaryConnection = nodePath.getBaseConnection().isPrimary().orElse(false);
        var parentTopicIds = nodePath.withoutBase().stream().filter(n -> n.getNodeType() == NodeType.TOPIC)
                .map(n -> n.getPublicId().toString()).toList();

        var ctx = new SearchableTaxonomyContextDTO(node.getPublicId(), subjectNode.getPublicId(), subjectNames, path,
                nodePath.getBreadcrumbs(), getContextType(node), relevanceId, relevanceTranslations, resourceTypes,
                parentTopicIds, isPrimaryConnection);

        return Optional.of(ctx);
    }

    private Optional<String> getContextType(Node node) {
        var strContentUri = node.getContentUri().toString();
        if (strContentUri.contains("article")) {
            if (node.getNodeType() == NodeType.TOPIC) {
                return Optional.of("topic-article");
            }
            return Optional.of("standard");
        }

        if (strContentUri.contains("learningpath")) {
            return Optional.of("learningpath");
        }

        return Optional.empty();
    }

    public static String DefaultLanguage = "nb";

}
