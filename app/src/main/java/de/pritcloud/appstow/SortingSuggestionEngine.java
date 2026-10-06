package de.pritcloud.appstow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class SortingSuggestionEngine {

    private static final String FAVORITES_SECTION =
            "favorites";

    private SortingSuggestionEngine() {
    }

    static final class Plan {

        final List<String> currentSectionOrder;
        final List<String> suggestedSectionOrder;

        final Map<String, List<String>>
                currentSectionItemOrders;

        final Map<String, List<String>>
                suggestedAppOrders;

        final Map<String, List<String>>
                suggestedShortcutOrders;

        final Map<String, List<String>>
                suggestedAllItemOrders;

        final Set<String> currentFavoriteIds;
        final Set<String> suggestedFavoriteIds;

        final List<String> currentFavoriteOrder;
        final List<String> suggestedFavoriteOrder;
        final List<String> suggestedAssignedFavoriteOrder;

        Plan(
                List<String> currentSectionOrder,
                List<String> suggestedSectionOrder,
                Map<String, List<String>> currentSectionItemOrders,
                Map<String, List<String>> suggestedAppOrders,
                Map<String, List<String>> suggestedShortcutOrders,
                Map<String, List<String>> suggestedAllItemOrders,
                Set<String> currentFavoriteIds,
                Set<String> suggestedFavoriteIds,
                List<String> currentFavoriteOrder,
                List<String> suggestedFavoriteOrder,
                List<String> suggestedAssignedFavoriteOrder) {

            this.currentSectionOrder =
                    new ArrayList<>(
                            currentSectionOrder);

            this.suggestedSectionOrder =
                    new ArrayList<>(
                            suggestedSectionOrder);

            this.currentSectionItemOrders =
                    copyOrders(
                            currentSectionItemOrders);

            this.suggestedAppOrders =
                    copyOrders(
                            suggestedAppOrders);

            this.suggestedShortcutOrders =
                    copyOrders(
                            suggestedShortcutOrders);

            this.suggestedAllItemOrders =
                    copyOrders(
                            suggestedAllItemOrders);

            this.currentFavoriteIds =
                    new LinkedHashSet<>(
                            currentFavoriteIds);

            this.suggestedFavoriteIds =
                    new LinkedHashSet<>(
                            suggestedFavoriteIds);

            this.currentFavoriteOrder =
                    new ArrayList<>(
                            currentFavoriteOrder);

            this.suggestedFavoriteOrder =
                    new ArrayList<>(
                            suggestedFavoriteOrder);

            this.suggestedAssignedFavoriteOrder =
                    new ArrayList<>(
                            suggestedAssignedFavoriteOrder);
        }

        boolean favoriteOrderChanged() {

            return !currentFavoriteOrder.equals(
                    suggestedFavoriteOrder);
        }

        boolean favoriteAssignmentChanged() {

            return !currentFavoriteIds.equals(
                    suggestedFavoriteIds);
        }

        boolean categoryOrderChanged() {

            return !currentSectionOrder.equals(
                    suggestedSectionOrder);
        }

        boolean appOrderChanged() {

            return ordersChanged(
                    currentSectionItemOrders,
                    suggestedAppOrders);
        }

        boolean shortcutOrderChanged() {

            return ordersChanged(
                    currentSectionItemOrders,
                    suggestedShortcutOrders);
        }

        boolean hasAnyChange() {

            return favoriteOrderChanged()
                    || favoriteAssignmentChanged()
                    || categoryOrderChanged()
                    || appOrderChanged()
                    || shortcutOrderChanged();
        }
    }

    static Plan build(
            List<String> sectionOrder,
            Map<String, List<String>> sectionItemOrders,
            Set<String> currentFavoriteIds,
            List<String> favoriteCandidateBaseline,
            int favoriteCount,
            Map<String, Integer> itemScores,
            Map<String, Integer> sectionScores) {

        List<String> safeSections =
                new ArrayList<>(
                        sectionOrder);

        Map<String, List<String>> currentOrders =
                copyOrders(
                        sectionItemOrders);

        Set<String> safeFavorites =
                new LinkedHashSet<>(
                        currentFavoriteIds);

        List<String> currentFavoriteOrder =
                new ArrayList<>();

        for (String id :
                currentOrders.getOrDefault(
                        FAVORITES_SECTION,
                        new ArrayList<>())) {

            if (safeFavorites.contains(
                    id)) {

                currentFavoriteOrder.add(
                        id);
            }
        }

        List<String> suggestedFavoriteOrder =
                AutomaticSortEngine.rankIds(
                        currentFavoriteOrder,
                        itemScores,
                        new HashMap<>(),
                        new HashMap<>(),
                        false);

        List<String> suggestedAssignedFavoriteOrder =
                AutomaticSortingPlanner
                        .selectTopUsedIds(
                                favoriteCandidateBaseline,
                                favoriteCount,
                                itemScores,
                                new HashMap<>(),
                                new HashMap<>(),
                                false);

        Set<String> suggestedFavoriteIds =
                new LinkedHashSet<>(
                        suggestedAssignedFavoriteOrder);

        List<String> suggestedSectionOrder =
                AutomaticSortingPlanner
                        .rankSections(
                                safeSections,
                                sectionScores,
                                new HashMap<>(),
                                new HashMap<>(),
                                false);

        Map<String, List<String>> appOrders =
                new HashMap<>();

        Map<String, List<String>> shortcutOrders =
                new HashMap<>();

        Map<String, List<String>> allOrders =
                new HashMap<>();

        for (Map.Entry<String, List<String>>
                entry :
                currentOrders.entrySet()) {

            String sectionId =
                    entry.getKey();

            List<String> current =
                    new ArrayList<>(
                            entry.getValue());

            if (FAVORITES_SECTION.equals(
                    sectionId)) {

                continue;
            }

            Set<String> appIds =
                    new HashSet<>();

            Set<String> shortcutIds =
                    new HashSet<>();

            for (String id : current) {

                if (id.startsWith(
                        "app:")) {

                    appIds.add(
                            id);

                } else if (id.startsWith(
                        "shortcut:")) {

                    shortcutIds.add(
                            id);
                }
            }

            appOrders.put(
                    sectionId,
                    AutomaticSortEngine
                            .rankSelectedIds(
                                    current,
                                    appIds,
                                    itemScores,
                                    new HashMap<>(),
                                    new HashMap<>(),
                                    false));

            shortcutOrders.put(
                    sectionId,
                    AutomaticSortEngine
                            .rankSelectedIds(
                                    current,
                                    shortcutIds,
                                    itemScores,
                                    new HashMap<>(),
                                    new HashMap<>(),
                                    false));

            allOrders.put(
                    sectionId,
                    AutomaticSortEngine
                            .rankIds(
                                    current,
                                    itemScores,
                                    new HashMap<>(),
                                    new HashMap<>(),
                                    false));
        }

        return new Plan(
                safeSections,
                suggestedSectionOrder,
                currentOrders,
                appOrders,
                shortcutOrders,
                allOrders,
                safeFavorites,
                suggestedFavoriteIds,
                currentFavoriteOrder,
                suggestedFavoriteOrder,
                suggestedAssignedFavoriteOrder);
    }

    private static Map<String, List<String>>
            copyOrders(
                    Map<String, List<String>> source) {

        Map<String, List<String>> result =
                new HashMap<>();

        if (source == null) {
            return result;
        }

        for (Map.Entry<String, List<String>>
                entry :
                source.entrySet()) {

            result.put(
                    entry.getKey(),
                    new ArrayList<>(
                            entry.getValue()));
        }

        return result;
    }

    private static boolean ordersChanged(
            Map<String, List<String>> current,
            Map<String, List<String>> suggested) {

        for (Map.Entry<String, List<String>>
                entry :
                suggested.entrySet()) {

            List<String> baseline =
                    current.get(
                            entry.getKey());

            if (baseline != null
                    && !baseline.equals(
                            entry.getValue())) {

                return true;
            }
        }

        return false;
    }
}
