package de.pritcloud.appstow;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SortingSuggestionEngineTest {

    @Test
    public void suggestionsCoverFavoritesSectionsAppsAndShortcuts() {

        List<String> sectionOrder =
                Arrays.asList(
                        "favorites",
                        "category:office",
                        "shortcuts");

        Map<String, List<String>> sectionItems =
                new HashMap<>();

        sectionItems.put(
                "favorites",
                Arrays.asList(
                        "app:a",
                        "shortcut:y"));

        sectionItems.put(
                "category:office",
                Arrays.asList(
                        "app:a",
                        "shortcut:y",
                        "app:b",
                        "shortcut:x"));

        sectionItems.put(
                "shortcuts",
                Arrays.asList(
                        "shortcut:y",
                        "shortcut:x"));

        Set<String> currentFavorites =
                new HashSet<>(
                        Arrays.asList(
                                "app:a",
                                "shortcut:y"));

        List<String> favoriteCandidates =
                Arrays.asList(
                        "app:a",
                        "shortcut:y",
                        "app:b",
                        "shortcut:x");

        Map<String, Integer> itemScores =
                counts(
                        "app:a", 4,
                        "shortcut:y", 8,
                        "app:b", 12,
                        "shortcut:x", 16);

        Map<String, Integer> sectionScores =
                counts(
                        "favorites", 1,
                        "category:office", 20,
                        "shortcuts", 10);

        SortingSuggestionEngine.Plan plan =
                SortingSuggestionEngine.build(
                        sectionOrder,
                        sectionItems,
                        currentFavorites,
                        favoriteCandidates,
                        2,
                        itemScores,
                        sectionScores);

        assertEquals(
                new HashSet<>(
                        Arrays.asList(
                                "shortcut:x",
                                "app:b")),
                plan.suggestedFavoriteIds);

        assertEquals(
                Arrays.asList(
                        "category:office",
                        "shortcuts",
                        "favorites"),
                plan.suggestedSectionOrder);

        assertEquals(
                Arrays.asList(
                        "shortcut:x",
                        "app:b",
                        "shortcut:y",
                        "app:a"),
                plan.suggestedAppOrders
                        .get(
                                "category:office"));

        assertEquals(
                Arrays.asList(
                        "shortcut:x",
                        "shortcut:y"),
                plan.suggestedShortcutOrders
                        .get(
                                "shortcuts"));

        assertTrue(
                !plan.suggestedShortcutOrders
                        .containsKey(
                                "category:office"));

        assertEquals(
                Arrays.asList(
                        "shortcut:x",
                        "app:b",
                        "shortcut:y",
                        "app:a"),
                plan.suggestedAllItemOrders
                        .get(
                                "category:office"));

        assertEquals(
                Arrays.asList(
                        "shortcut:y",
                        "app:a"),
                plan.favoriteOrderPreview(
                        false,
                        true));

        assertEquals(
                Arrays.asList(
                        "shortcut:x",
                        "app:b"),
                plan.favoriteOrderPreview(
                        true,
                        true));

        assertTrue(
                plan.favoriteAssignmentChanged());

        assertTrue(
                plan.categoryOrderChanged());

        assertTrue(
                plan.appOrderChanged());

        assertTrue(
                plan.shortcutOrderChanged());

        assertTrue(
                plan.hasAnyChange());
    }

    @Test
    public void favoritePreviewCombinesAssignmentAndSortingIndependently() {

        SortingSuggestionEngine.Plan plan =
                SortingSuggestionEngine.build(
                        List.of(
                                "favorites"),
                        Map.of(
                                "favorites",
                                Arrays.asList(
                                        "app:a",
                                        "app:b",
                                        "app:c",
                                        "app:d")),
                        new HashSet<>(
                                Arrays.asList(
                                        "app:a",
                                        "app:b",
                                        "app:c",
                                        "app:d")),
                        Arrays.asList(
                                "app:a",
                                "app:b",
                                "app:c",
                                "app:d",
                                "app:e",
                                "app:f"),
                        4,
                        counts(
                                "app:a", 80,
                                "app:b", 100,
                                "app:c", 10,
                                "app:d", 5,
                                "app:e", 90,
                                "app:f", 70),
                        new HashMap<>());

        /*
         * Weder Neuzuordnung noch Sortierung:
         * Soll entspricht exakt Ist.
         */
        assertEquals(
                Arrays.asList(
                        "app:a",
                        "app:b",
                        "app:c",
                        "app:d"),
                plan.favoriteOrderPreview(
                        false,
                        false));

        /*
         * Nur Sortierung:
         * aktuelle Favoritenmenge wird gerankt.
         */
        assertEquals(
                Arrays.asList(
                        "app:b",
                        "app:a",
                        "app:c",
                        "app:d"),
                plan.favoriteOrderPreview(
                        false,
                        true));

        /*
         * Nur Neuzuordnung:
         * a und b bleiben in manueller Reihenfolge,
         * c und d verschwinden,
         * e und f werden hinten angefügt.
         */
        assertEquals(
                Arrays.asList(
                        "app:a",
                        "app:b",
                        "app:e",
                        "app:f"),
                plan.favoriteOrderPreview(
                        true,
                        false));

        /*
         * Neuzuordnung plus Sortierung:
         * neue Favoritenmenge wird vollständig gerankt.
         */
        assertEquals(
                Arrays.asList(
                        "app:b",
                        "app:e",
                        "app:a",
                        "app:f"),
                plan.favoriteOrderPreview(
                        true,
                        true));
    }

    @Test
    public void singleHotAppCanMoveAheadOfShortcuts() {

        SortingSuggestionEngine.Plan plan =
                SortingSuggestionEngine.build(
                        Arrays.asList(
                                "favorites",
                                "category:shopping"),
                        Map.of(
                                "favorites",
                                List.of(),
                                "category:shopping",
                                Arrays.asList(
                                        "shortcut:booking",
                                        "shortcut:amazon",
                                        "shortcut:kleinanzeigen",
                                        "app:kitchenowl")),
                        Set.of(),
                        Arrays.asList(
                                "shortcut:booking",
                                "shortcut:amazon",
                                "shortcut:kleinanzeigen",
                                "app:kitchenowl"),
                        3,
                        counts(
                                "shortcut:booking", 1,
                                "shortcut:amazon", 2,
                                "shortcut:kleinanzeigen", 3,
                                "app:kitchenowl", 100),
                        new HashMap<>());

        assertTrue(
                plan.appOrderChanged());

        assertEquals(
                Arrays.asList(
                        "app:kitchenowl",
                        "shortcut:kleinanzeigen",
                        "shortcut:amazon",
                        "shortcut:booking"),
                plan.suggestedAppOrders
                        .get(
                                "category:shopping"));

        assertTrue(
                !plan.suggestedShortcutOrders
                        .containsKey(
                                "category:shopping"));
    }

    @Test
    public void unusedEntriesDoNotFillSuggestedFavorites() {

        SortingSuggestionEngine.Plan plan =
                SortingSuggestionEngine.build(
                        List.of(
                                "favorites"),
                        Map.of(
                                "favorites",
                                List.of()),
                        Set.of(),
                        Arrays.asList(
                                "app:used",
                                "shortcut:unused"),
                        10,
                        counts(
                                "app:used", 4),
                        new HashMap<>());

        assertEquals(
                Set.of(
                        "app:used"),
                plan.suggestedFavoriteIds);
    }

    private static Map<String, Integer> counts(
            Object... values) {

        Map<String, Integer> result =
                new HashMap<>();

        for (int index = 0;
             index < values.length;
             index += 2) {

            result.put(
                    (String) values[index],
                    (Integer) values[index + 1]);
        }

        return result;
    }
}
