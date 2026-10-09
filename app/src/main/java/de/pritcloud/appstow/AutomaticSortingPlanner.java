package de.pritcloud.appstow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class AutomaticSortingPlanner {

    private AutomaticSortingPlanner() {
    }

    static List<String> selectTopUsedIds(
            List<String> baselineOrder,
            int limit,
            Map<String, Integer> overallWeighted,
            Map<String, Integer> profileWeighted,
            Map<String, Integer> profileLaunches,
            boolean timeProfileEnabled) {

        List<String> result =
                new ArrayList<>();

        if (limit < 1) {
            return result;
        }

        List<String> ranked =
                AutomaticSortEngine.rankIds(
                        baselineOrder,
                        overallWeighted,
                        profileWeighted,
                        profileLaunches,
                        timeProfileEnabled);

        for (String id :
                ranked) {

            long score =
                    AutomaticSortEngine.getScore(
                            id,
                            overallWeighted,
                            profileWeighted,
                            profileLaunches,
                            timeProfileEnabled);

            if (score <= 0) {
                continue;
            }

            result.add(
                    id);

            if (result.size()
                    >= limit) {

                break;
            }
        }

        return result;
    }

    static List<String> preserveRestoredFavoritesWithoutUsage(
            List<String> ranked,
            List<String> restored,
            boolean fallbackEnabled) {

        List<String> result =
                ranked == null
                        ? new ArrayList<>()
                        : new ArrayList<>(ranked);

        if (!fallbackEnabled
                || !result.isEmpty()) {

            return result;
        }

        if (restored == null) {
            return result;
        }

        for (String id : restored) {

            if (id != null
                    && !id.isBlank()
                    && !result.contains(id)) {

                result.add(id);
            }
        }

        return result;
    }

    static List<String> rankSections(
            List<String> baselineOrder,
            Map<String, Integer> overallWeighted,
            Map<String, Integer> profileWeighted,
            Map<String, Integer> profileLaunches,
            boolean timeProfileEnabled) {

        /*
         * Favoriten haben bewusst KEINE Sonderposition.
         * Sie werden exakt wie Kategorien und Eigene
         * Shortcuts anhand ihres Nutzungswerts gerankt.
         */
        return AutomaticSortEngine.rankIds(
                baselineOrder,
                overallWeighted,
                profileWeighted,
                profileLaunches,
                timeProfileEnabled);
    }
}
