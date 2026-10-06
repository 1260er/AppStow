package de.pritcloud.appstow;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class AutomaticSortingPlannerTest {

    @Test
    public void automaticFavoritesRankAppsAndShortcutsTogether() {

        Map<String, Integer> scores =
                counts(
                        "app:app.a", 4,
                        "shortcut:shortcut.a", 16,
                        "app:app.b", 12);

        List<String> favorites =
                AutomaticSortingPlanner
                        .selectTopUsedIds(
                                Arrays.asList(
                                        "app:app.a",
                                        "shortcut:shortcut.a",
                                        "app:app.b",
                                        "shortcut:unused"),
                                10,
                                scores,
                                new HashMap<>(),
                                new HashMap<>(),
                                false);

        assertEquals(
                Arrays.asList(
                        "shortcut:shortcut.a",
                        "app:app.b",
                        "app:app.a"),
                favorites);
    }

    @Test
    public void automaticFavoriteLimitIsRespected() {

        Map<String, Integer> scores =
                counts(
                        "app.a", 4,
                        "app.b", 12,
                        "app.c", 8);

        List<String> favorites =
                AutomaticSortingPlanner
                        .selectTopUsedIds(
                                Arrays.asList(
                                        "app.a",
                                        "app.b",
                                        "app.c"),
                                2,
                                scores,
                                new HashMap<>(),
                                new HashMap<>(),
                                false);

        assertEquals(
                Arrays.asList(
                        "app.b",
                        "app.c"),
                favorites);
    }

    @Test
    public void favoritesSectionHasNoSpecialPosition() {

        Map<String, Integer> scores =
                counts(
                        "favorites", 1,
                        "category:office", 20,
                        "shortcuts", 10);

        List<String> sections =
                AutomaticSortingPlanner
                        .rankSections(
                                Arrays.asList(
                                        "favorites",
                                        "category:office",
                                        "shortcuts"),
                                scores,
                                new HashMap<>(),
                                new HashMap<>(),
                                false);

        assertEquals(
                Arrays.asList(
                        "category:office",
                        "shortcuts",
                        "favorites"),
                sections);
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
