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

        List<String> result =
                normalizeOrder(
                        baselineOrder);

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

    static List<String> rankSelectedIds(
            List<String> baselineOrder,
            Set<String> selectedIds,
            Map<String, Integer> overallWeighted,
            Map<String, Integer> profileWeighted,
            Map<String, Integer> profileLaunches,
            boolean timeProfileEnabled) {

        List<String> result =
                normalizeOrder(
                        baselineOrder);

        if (selectedIds == null
                || selectedIds.isEmpty()) {

            return result;
        }

        List<String> selected =
                new ArrayList<>();

        for (String id : result) {
            if (selectedIds.contains(id)) {
                selected.add(id);
            }
        }

        if (selected.size() < 2) {
            return result;
        }

        List<String> ranked =
                rankIds(
                        selected,
                        overallWeighted,
                        profileWeighted,
                        profileLaunches,
                        timeProfileEnabled);

        int rankedIndex = 0;

        for (int index = 0;
             index < result.size();
             index++) {

            if (!selectedIds.contains(
                    result.get(index))) {

                continue;
            }

            result.set(
                    index,
                    ranked.get(
                            rankedIndex));

            rankedIndex++;
        }

        return result;
    }

    static List<String> preserveSelectedBaselineOrder(
            List<String> displayedOrder,
            List<String> baselineOrder,
            Set<String> selectedIds) {

        List<String> displayed =
                normalizeOrder(
                        displayedOrder);

        if (selectedIds == null
                || selectedIds.isEmpty()) {

            return displayed;
        }

        Set<String> displayedSelected =
                new LinkedHashSet<>();

        for (String id : displayed) {
            if (selectedIds.contains(id)) {
                displayedSelected.add(id);
            }
        }

        List<String> baselineSelected =
                new ArrayList<>();

        Set<String> added =
                new LinkedHashSet<>();

        for (String id :
                normalizeOrder(
                        baselineOrder)) {

            if (displayedSelected.contains(id)
                    && added.add(id)) {

                baselineSelected.add(id);
            }
        }

        for (String id : displayed) {
            if (displayedSelected.contains(id)
                    && added.add(id)) {

                baselineSelected.add(id);
            }
        }

        int selectedIndex = 0;

        for (int index = 0;
             index < displayed.size();
             index++) {

            if (!selectedIds.contains(
                    displayed.get(index))) {

                continue;
            }

            if (selectedIndex
                    >= baselineSelected.size()) {

                break;
            }

            displayed.set(
                    index,
                    baselineSelected.get(
                            selectedIndex));

            selectedIndex++;
        }

        return displayed;
    }

    private static List<String> normalizeOrder(
            List<String> order) {

        Set<String> unique =
                new LinkedHashSet<>();

        if (order != null) {
            for (String id : order) {
                if (id != null
                        && !id.isBlank()) {

                    unique.add(id);
                }
            }
        }

        return new ArrayList<>(
                unique);
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
