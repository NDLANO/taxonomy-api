package no.ndla.taxonomy.service.dtos;

import no.ndla.taxonomy.domain.Node;
import no.ndla.taxonomy.domain.NodePath;
import no.ndla.taxonomy.domain.NodeType;
import no.ndla.taxonomy.repositories.RelevanceRepository;
import no.ndla.taxonomy.rest.v1.dtos.nodes.searchapi.LanguageFieldDTO;
import no.ndla.taxonomy.rest.v1.dtos.nodes.searchapi.SearchableTaxonomyResourceType;
import no.ndla.taxonomy.rest.v1.dtos.nodes.searchapi.TaxonomyContextDTO;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static no.ndla.taxonomy.config.Constants.DefaultLanguage;

@Service
public class TaxonomyContextDTOFactory {
    private final RelevanceRepository relevanceRepository;

    public TaxonomyContextDTOFactory(RelevanceRepository relevanceRepository) {
        this.relevanceRepository = relevanceRepository;
    }

    public List<TaxonomyContextDTO> fromNodes(List<Node> nodes, boolean filterVisibles) {
        return nodes.stream().flatMap(node -> {
            return node.buildPaths().stream().map(np -> {
                return fromNodeAndPath(node, np, filterVisibles);
            }).filter(Optional::isPresent).map(Optional::get);
        }).toList();
    }

    private Optional<TaxonomyContextDTO> fromNodeAndPath(Node node, NodePath nodePath, boolean filterVisibles) {
        if (filterVisibles && !nodePath.isVisible())
            return Optional.empty();

        var maybeFirstNode = nodePath.getNodes().stream().findFirst()
                .filter(s -> s.getPublicId() != node.getPublicId());
        if (maybeFirstNode.isEmpty())
            return Optional.empty();

        var subjectNode = maybeFirstNode.get();
        var subjectNames = LanguageFieldDTO.nameFromNode(subjectNode);
        var path = nodePath.toString();

        var relevance = nodePath.getBaseRelevance().orElseGet(() -> {
            var coreId = URI.create("urn:relevance:core");
            return relevanceRepository.findFirstByPublicIdIncludingTranslations(coreId).orElseThrow();
        });

        var relevanceTranslations = new LanguageFieldDTO<String>();
        relevanceTranslations.put(DefaultLanguage, relevance.getName());
        relevance.getTranslations()
                .forEach(trans -> relevanceTranslations.put(trans.getLanguageCode(), trans.getName()));

        var relevanceId = relevance.getPublicId();

        var resourceTypes = node.getResourceTypes().stream().map(SearchableTaxonomyResourceType::new).toList();
        var isPrimaryConnection = nodePath.getBaseConnection().isPrimary().orElse(false);
        var parentTopicIds = getAllParentTopicIds(node, filterVisibles);

        var ctx = new TaxonomyContextDTO(node.getPublicId(), subjectNode.getPublicId(), subjectNames, path,
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

    private List<URI> getAllParentTopicIds(Node node, boolean filterVisibles) {
        var ids = new ArrayList<URI>();
        var parents = node.getParentNodes();

        for (var parent : parents) {
            var skipForVisibility = filterVisibles && !parent.getMetadata().isVisible();
            if (parent.getNodeType() != NodeType.TOPIC || skipForVisibility) {
                continue;
            }

            ids.add(parent.getPublicId());
            var parentPids = getAllParentTopicIds(parent, filterVisibles);
            ids.addAll(parentPids);
        }

        return ids;
    }

}
