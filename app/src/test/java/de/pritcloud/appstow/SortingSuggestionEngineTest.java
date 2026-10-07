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
                        "app:b",
                        "shortcut:y",
                        "app:a",
                        "shortcut:x"),
                plan.suggestedAppOrders
                        .get(
                                "category:office"));

        assertEquals(
                Arrays.asList(
                        "app:a",
                        "shortcut:x",
                        "app:b",
                        "shortcut:y"),
                plan.suggestedShortcutOrders
                        .get(
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
                        false));

        assertEquals(
                Arrays.asList(
                        "shortcut:x",
                        "app:b"),
                plan.favoriteOrderPreview(
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
