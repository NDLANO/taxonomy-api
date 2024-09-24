/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service;

import java.net.URI;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import no.ndla.taxonomy.domain.*;
import no.ndla.taxonomy.repositories.NodeRepository;
import no.ndla.taxonomy.rest.v1.commands.NodePostPut;
import no.ndla.taxonomy.service.dtos.QualityEvaluationDTO;
import no.ndla.taxonomy.service.exceptions.NotFoundServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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
    public void updateQualityEvaluationOfParents(Node node, Optional<Grade> oldGrade, UpdatableDto<?> command) {
        if (!(command instanceof NodePostPut nodeCommand)) {
            return;
        }

        if (!nodeCommand.qualityEvaluation.isChanged()) {
            return;
        }

        var newGrade = nodeCommand.qualityEvaluation.getValue().map(QualityEvaluationDTO::getGrade);

        updateQualityEvaluationOfParents(node, oldGrade, newGrade);
    }

    public void updateQualityEvaluationOfNewConnection(Node parent, Node child) {
        // Update parents quality evaluation average with the newly linked one.
        updateQualityEvaluationOfParents(child, Optional.empty(), child.getQualityEvaluationGrade());

        child.getChildQualityEvaluationAverage()
                .ifPresent(childAverage -> addGradeAverageTreeToParents(parent, childAverage));
    }

    private void addGradeAverageTreeToParents(Node node, GradeAverage averageToAdd) {
        node.addGradeAverageTreeToAverageCalculation(averageToAdd);
        node.getParentNodes().forEach(parent -> addGradeAverageTreeToParents(parent, averageToAdd));
    }

    private void removeGradeAverageTreeFromParents(Node node, GradeAverage averageToRemove) {
        node.removeGradeAverageTreeFromAverageCalculation(averageToRemove);
        node.getParentNodes().forEach(parent -> removeGradeAverageTreeFromParents(parent, averageToRemove));
    }

    public void removeQualityEvaluationOfDeletedConnection(NodeConnection connectionToDelete) {
        var noChild = connectionToDelete.getChild().isEmpty();
        var noParent = connectionToDelete.getParent().isEmpty();
        if (noChild || noParent) return;

        var child = connectionToDelete.getChild().get();

        if (shouldBeIncludedInQualityEvaluationAverage(child.getNodeType())) {
            updateQualityEvaluationOfParents(child, child.getQualityEvaluationGrade(), Optional.empty());
            return;
        }

        if (child.getChildQualityEvaluationAverage().isEmpty()) return;
        var childAverage = child.getChildQualityEvaluationAverage().get();

        var parent = connectionToDelete.getParent().get();
        removeGradeAverageTreeFromParents(parent, childAverage);
    }

    @Transactional
    protected void updateQualityEvaluationOfParents(Node node, Optional<Grade> oldGrade, Optional<Grade> newGrade) {
        if (!shouldBeIncludedInQualityEvaluationAverage(node.getNodeType())) {
            return;
        }
        if (oldGrade.isEmpty() && newGrade.isEmpty() || oldGrade.equals(newGrade)) {
            return;
        }

        logger.info(
                "Updating quality evaluation of parents for node: {} with parents: {}, Grade: {} -> {}",
                node.getPublicId(),
                node.getParentNodes(),
                oldGrade,
                newGrade);

        updateQualityEvaluationOf(node.getParentNodes(), oldGrade, newGrade);
    }

    @Transactional
    public void updateQualityEvaluationOf(
            Collection<Node> parents, Optional<Grade> oldGrade, Optional<Grade> newGrade) {
        updateQualityEvaluationOfRecursive(parents, oldGrade, newGrade);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void updateQualityEvaluationOfRecursive(
            Collection<Node> parents, Optional<Grade> oldGrade, Optional<Grade> newGrade) {
        var updatedParents = parents.stream()
                .peek(p -> {
                    p.updateChildQualityEvaluationAverage(oldGrade, newGrade);
                    var parentsParents = p.getParentNodes();
                    updateQualityEvaluationOfRecursive(parentsParents, oldGrade, newGrade);
                })
                .toList();

        nodeRepository.saveAll(updatedParents);
    }

    @Transactional
    public void updateEntireAverageTreeForNode(URI publicId) {
        var node = nodeRepository
                .findFirstByPublicId(publicId)
                .orElseThrow(() -> new NotFoundServiceException("Node was not found"));
        node.updateEntireAverageTree();
        nodeRepository.save(node);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateQualityEvaluationOfAllNodes() {
        var ids = nodeRepository.findIdsWithChildren();
        final var counter = new AtomicInteger();
        ids.stream()
                .collect(Collectors.groupingBy(i -> counter.getAndIncrement() / 1000))
                .values()
                .forEach(idChunk -> {
                    var toSave = nodeRepository.findByIds(idChunk).stream()
                            .peek(Node::updateEntireAverageTree)
                            .toList();
                    nodeRepository.saveAll(toSave);
                });
    }
}
