/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Collection;
import java.util.Optional;

public class GradeAverage implements Serializable {
    public GradeAverage(double averageValue, int count) {
        this.averageValue = averageValue;
        this.count = count;
    }

    @JsonProperty
    double averageValue;

    @JsonProperty
    int count;

    public static GradeAverage fromGrades(Collection<Optional<Grade>> grades) {
        var existing = grades.stream().flatMap(Optional::stream).toList();
        var count = existing.size();
        var avg = existing.stream().mapToInt(Grade::toInt).average().orElse(0.0);
        return new GradeAverage(avg, count);
    }

    public static GradeAverage fromNodes(Collection<Node> nodes) {
        var avg = nodes.stream()
                .map(Node::getQualityEvaluationGrade)
                .flatMap(Optional::stream)
                .mapToInt(Grade::toInt)
                .average()
                .orElse(0.0);
        var count = nodes.size();
        return new GradeAverage(avg, count);
    }

    public GradeAverage add(GradeAverage other) {
        var newValue = (averageValue * count + other.averageValue * other.count) / (count + other.count);
        var newCount = this.count + other.count;
        return new GradeAverage(newValue, newCount);
    }
}
