/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2024 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.domain;

import java.util.Collection;
import java.util.Optional;

public class GradeAverage {
    public GradeAverage(double averageValue, int count) {
        this.averageValue = averageValue;
        this.count = count;
    }

    double averageValue;
    int count;

    public static GradeAverage fromGrades(Collection<Optional<Grade>> grades) {
        var existing = grades.stream().flatMap(Optional::stream).toList();
        var count = existing.size();
        var avg = existing.stream().mapToInt(Grade::toInt).average().orElse(0.0);
        return new GradeAverage(avg, count);
    }

    public double getAverageValue() {
        return averageValue;
    }

    public int getCount() {
        return count;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        GradeAverage that = (GradeAverage) obj;
        return Double.compare(that.averageValue, averageValue) == 0 && count == that.count;
    }

    @Override
    public String toString() {
        return "GradeAverage{averageValue=" + averageValue + ", count=" + count + '}';
    }
}
