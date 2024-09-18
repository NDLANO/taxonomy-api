/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.repositories.NodeRepository;
import no.ndla.taxonomy.rest.v1.commands.NodePostPut;
import no.ndla.taxonomy.service.dtos.QualityEvaluationDTO;
import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
@Service
public class QualityEvaluationService {
    Logger logger = LoggerFactory.getLogger(getClass().getName());

    private final NodeRepository nodeRepository;

    public QualityEvaluationService(NodeRepository nodeRepository) {
        this.nodeRepository = nodeRepository;
    }

    private boolean shouldBeIncludedInQualityEvaluationAverage(NodeType nodeType) {
        return nodeType == NodeType.RESOURCE;
    }

    @Transactional
    public void updateQualityEvaluationOfParents(
            URI nodeId, NodeType nodeType, Optional<Grade> oldGrade, UpdatableDto<?> command) {
        if (!(command instanceof NodePostPut nodeCommand)) {
            return;
        }

        Optional<QualityEvaluationDTO> qe =
                nodeCommand.qualityEvaluation.isDelete() ? Optional.empty() : nodeCommand.qualityEvaluation.getValue();
        var newGrade = qe.map(QualityEvaluationDTO::getGrade);

        updateQualityEvaluationOfParents(nodeId, nodeType, oldGrade, newGrade);
    }

    public void updateQualityEvaluationOfNewConnection(Node parent, Node child) {
        // Update parents quality evaluation average with the newly linked one.
        updateQualityEvaluationOfParents(
                child.getPublicId(), child.getNodeType(), Optional.empty(), child.getQualityEvaluationGrade());

        child.getChildQualityEvaluationAverage().ifPresent(parent::addGradeAverageTreeToAverageCalculation);
    }

    public void removeQualityEvaluationOfDeletedConnection(NodeConnection connectionToDelete) {
        var noChild = connectionToDelete.getChild().isEmpty();
        var noParent = connectionToDelete.getParent().isEmpty();
        if (noChild || noParent) return;

        var child = connectionToDelete.getChild().get();

        if (shouldBeIncludedInQualityEvaluationAverage(child.getNodeType())) {
            updateQualityEvaluationOfParents(
                    child.getPublicId(), child.getNodeType(), child.getQualityEvaluationGrade(), Optional.empty());
            return;
        }

        if (child.getChildQualityEvaluationAverage().isEmpty()) return;
        var childAverage = child.getChildQualityEvaluationAverage().get();

        var parent = connectionToDelete.getParent().get();
        parent.removeGradeAverageTreeFromAverageCalculation(childAverage);
    }

    @Transactional
    public void updateQualityEvaluationOfParents(
            URI nodeId, NodeType nodeType, Optional<Grade> oldGrade, Optional<Grade> newGrade) {
        if (!shouldBeIncludedInQualityEvaluationAverage(nodeType)) {
            return;
        }
        if (oldGrade.isEmpty() && newGrade.isEmpty() || oldGrade.equals(newGrade)) {
            return;
        }

        var node = nodeRepository
                .findFirstByPublicId(nodeId)
                .orElseThrow(() -> new NotFoundServiceException("Node was not found"));

        updateQualityEvaluationOf(node.getParentNodes(), oldGrade, newGrade);
    }

    @Transactional
    public void updateQualityEvaluationOf(
            Collection<Node> parents, Optional<Grade> oldGrade, Optional<Grade> newGrade) {
        var parentIds = parents.stream().map(DomainEntity::getPublicId).toList();
        updateQualityEvaluationOfRecursive(parentIds, oldGrade, newGrade);
    }

    @Transactional
    protected void updateQualityEvaluationOfRecursive(
            List<URI> parentIds, Optional<Grade> oldGrade, Optional<Grade> newGrade) {
        parentIds.forEach(pid -> nodeRepository.findFirstByPublicId(pid).ifPresent(p -> {
            p.updateChildQualityEvaluationAverage(oldGrade, newGrade);
            nodeRepository.save(p);
            var parentsParents =
                    p.getParentNodes().stream().map(Node::getPublicId).toList();
            updateQualityEvaluationOfRecursive(parentsParents, oldGrade, newGrade);
        }));
    }


    @Transactional
    public void updateEntireAverageTreeForNode(URI publicId) {
        var node = nodeRepository.findFirstByPublicId(publicId)
                .orElseThrow(() -> new NotFoundServiceException("Node was not found"));
        node.updateEntireAverageTree();
        nodeRepository.save(node);
    }
}
