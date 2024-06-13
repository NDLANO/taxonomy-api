/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Optional;
import no.ndla.taxonomy.domain.Node;

public class GradeAverageDTO implements Serializable {
    public GradeAverageDTO(double averageValue, int count) {
        this.averageValue = averageValue;
        this.count = count;
    }

    @JsonProperty
    double averageValue;

    @JsonProperty
    int count;

    public static double roundToSingleDecimal(double value) {
        return (double) Math.round(value * 10) / 10;
    }

    public static Optional<GradeAverageDTO> fromNode(Node node) {
        return node.getChildQualityEvaluationAverage().map(ga -> {
            var roundedAvg = roundToSingleDecimal(ga.getAverageValue());
            var count1 = ga.getCount();
            return new GradeAverageDTO(roundedAvg, count1);
        });
    }

    public double getAverageValue() {
        return averageValue;
    }

    public int getCount() {
        return count;
    }
}
