package de.pritcloud.appstow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class AutomaticSortEngine {

    static final int PROFILE_MIN_LAUNCHES = 5;
    static final int PROFILE_MULTIPLIER = 3;

    private AutomaticSortEngine() {
    }

    static long getScore(
            String id,
            Map<String, Integer> overallWeighted,
            Map<String, Integer> profileWeighted,
            Map<String, Integer> profileLaunches,
            boolean timeProfileEnabled) {

        if (id == null) {
            return 0L;
        }

        long overall =
                positiveValue(
                        overallWeighted,
                        id);

        if (!timeProfileEnabled) {
            return overall;
        }

        int evidence =
                positiveValue(
                        profileLaunches,
                        id);

        if (evidence
                < PROFILE_MIN_LAUNCHES) {

            return overall;
        }

        long profile =
                positiveValue(
                        profileWeighted,
                        id);

        return overall
                + PROFILE_MULTIPLIER
                * profile;
    }

    static List<String> rankIds(
            List<String> baselineOrder,
            Map<String, Integer> overallWeighted,
            Map<String, Integer> profileWeighted,
            Map<String, Integer> profileLaunches,
            boolean timeProfileEnabled) {

        Set<String> unique =
                new LinkedHashSet<>();

        if (baselineOrder != null) {
            for (String id :
                    baselineOrder) {

                if (id != null
                        && !id.isBlank()) {

                    unique.add(
                            id);
                }
            }
        }

        List<String> result =
                new ArrayList<>(
                        unique);

        Map<String, Integer> positions =
                new HashMap<>();

        for (int index = 0;
             index < result.size();
             index++) {

            positions.put(
                    result.get(index),
                    index);
        }

        result.sort(
                (left, right) -> {

                    long leftScore =
                            getScore(
                                    left,
                                    overallWeighted,
                                    profileWeighted,
                                    profileLaunches,
                                    timeProfileEnabled);

                    long rightScore =
                            getScore(
                                    right,
                                    overallWeighted,
                                    profileWeighted,
                                    profileLaunches,
                                    timeProfileEnabled);

                    int scoreComparison =
                            Long.compare(
                                    rightScore,
                                    leftScore);

                    if (scoreComparison != 0) {
                        return scoreComparison;
                    }

                    return Integer.compare(
                            positions.get(left),
                            positions.get(right));
                });

        return result;
    }

    private static int positiveValue(
            Map<String, Integer> values,
            String id) {

        if (values == null) {
            return 0;
        }

        Integer value =
                values.get(
                        id);

        if (value == null
                || value <= 0) {

            return 0;
        }

        return value;
    }
}
