package de.pritcloud.appstow;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class AutomaticSortEngineTest {

    @Test
    public void ranksHigherOverallScoreFirst() {

        Map<String, Integer> overall =
                counts(
                        "app.a", 4,
                        "app.b", 12);

        List<String> ranked =
                AutomaticSortEngine.rankIds(
                        Arrays.asList(
                                "app.a",
                                "app.b",
                                "app.c"),
                        overall,
                        new HashMap<>(),
                        new HashMap<>(),
                        false);

        assertEquals(
                Arrays.asList(
                        "app.b",
                        "app.a",
                        "app.c"),
                ranked);
    }

    @Test
    public void profileWeightIsAppliedAtFiveLaunches() {

        Map<String, Integer> overall =
                counts(
                        "app.a", 20,
                        "app.b", 30);

        Map<String, Integer> profile =
                counts(
                        "app.a", 12,
                        "app.b", 1);

        Map<String, Integer> evidence =
                counts(
                        "app.a", 5,
                        "app.b", 5);

        List<String> ranked =
                AutomaticSortEngine.rankIds(
                        Arrays.asList(
                                "app.b",
                                "app.a"),
                        overall,
                        profile,
                        evidence,
                        true);

        assertEquals(
                Arrays.asList(
                        "app.a",
                        "app.b"),
                ranked);

        assertEquals(
                56L,
                AutomaticSortEngine.getScore(
                        "app.a",
                        overall,
                        profile,
                        evidence,
                        true));
    }

    @Test
    public void profileWeightIsIgnoredBelowFiveLaunches() {

        Map<String, Integer> overall =
                counts(
                        "app.a", 20,
                        "app.b", 30);

        Map<String, Integer> profile =
                counts(
                        "app.a", 50,
                        "app.b", 1);

        Map<String, Integer> evidence =
                counts(
                        "app.a", 4,
                        "app.b", 5);

        List<String> ranked =
                AutomaticSortEngine.rankIds(
                        Arrays.asList(
                                "app.a",
                                "app.b"),
                        overall,
                        profile,
                        evidence,
                        true);

        assertEquals(
                Arrays.asList(
                        "app.b",
                        "app.a"),
                ranked);

        assertEquals(
                20L,
                AutomaticSortEngine.getScore(
                        "app.a",
                        overall,
                        profile,
                        evidence,
                        true));
    }

    @Test
    public void equalScoresKeepBaselineOrder() {

        Map<String, Integer> overall =
                counts(
                        "app.a", 8,
                        "app.b", 8);

        List<String> ranked =
                AutomaticSortEngine.rankIds(
                        Arrays.asList(
                                "app.b",
                                "app.a",
                                "app.c"),
                        overall,
                        new HashMap<>(),
                        new HashMap<>(),
                        false);

        assertEquals(
                Arrays.asList(
                        "app.b",
                        "app.a",
                        "app.c"),
                ranked);
    }

    @Test
    public void unusedEntriesStayStableBehindUsedEntries() {

        Map<String, Integer> overall =
                counts(
                        "app.c", 1);

        List<String> ranked =
                AutomaticSortEngine.rankIds(
                        Arrays.asList(
                                "app.a",
                                "app.b",
                                "app.c",
                                "app.d"),
                        overall,
                        new HashMap<>(),
                        new HashMap<>(),
                        false);

        assertEquals(
                Arrays.asList(
                        "app.c",
                        "app.a",
                        "app.b",
                        "app.d"),
                ranked);
    }

    @Test
    public void selectedIdsAreRankedOnlyInsideTheirSlots() {

        Map<String, Integer> scores =
                counts(
                        "app:a", 1,
                        "app:b", 10);

        Set<String> automatic =
                new HashSet<>(
                        Arrays.asList(
                                "app:a",
                                "app:b"));

        List<String> ranked =
                AutomaticSortEngine.rankSelectedIds(
                        Arrays.asList(
                                "app:a",
                                "shortcut:x",
                                "app:b",
                                "shortcut:y"),
                        automatic,
                        scores,
                        new HashMap<>(),
                        new HashMap<>(),
                        false);

        assertEquals(
                Arrays.asList(
                        "app:b",
                        "shortcut:x",
                        "app:a",
                        "shortcut:y"),
                ranked);
    }

    @Test
    public void selectedAppCanMoveAcrossShortcuts() {

        Map<String, Integer> scores =
                counts(
                        "shortcut:booking", 1,
                        "shortcut:amazon", 2,
                        "shortcut:kleinanzeigen", 3,
                        "app:kitchenowl", 100);

        Set<String> automaticApps =
                new HashSet<>(
                        Arrays.asList(
                                "app:kitchenowl"));

        List<String> ranked =
                AutomaticSortEngine
                        .rankSelectedIdsAcrossPeers(
                                Arrays.asList(
                                        "shortcut:booking",
                                        "shortcut:amazon",
                                        "shortcut:kleinanzeigen",
                                        "app:kitchenowl"),
                                automaticApps,
                                scores,
                                new HashMap<>(),
                                new HashMap<>(),
                                false);

        assertEquals(
                Arrays.asList(
                        "app:kitchenowl",
                        "shortcut:booking",
                        "shortcut:amazon",
                        "shortcut:kleinanzeigen"),
                ranked);
    }

    @Test
    public void selectedShortcutCanMoveAcrossApps() {

        Map<String, Integer> scores =
                counts(
                        "app:a", 1,
                        "app:b", 2,
                        "app:c", 3,
                        "shortcut:hot", 100);

        Set<String> automaticShortcuts =
                new HashSet<>(
                        Arrays.asList(
                                "shortcut:hot"));

        List<String> ranked =
                AutomaticSortEngine
                        .rankSelectedIdsAcrossPeers(
                                Arrays.asList(
                                        "app:a",
                                        "app:b",
                                        "app:c",
                                        "shortcut:hot"),
                                automaticShortcuts,
                                scores,
                                new HashMap<>(),
                                new HashMap<>(),
                                false);

        assertEquals(
                Arrays.asList(
                        "shortcut:hot",
                        "app:a",
                        "app:b",
                        "app:c"),
                ranked);
    }

    @Test
    public void allSelectedAppsAndShortcutsRankTogether() {

        Map<String, Integer> scores =
                counts(
                        "app:a", 5,
                        "shortcut:x", 20,
                        "app:b", 30,
                        "shortcut:y", 10);

        Set<String> automaticItems =
                new HashSet<>(
                        Arrays.asList(
                                "app:a",
                                "shortcut:x",
                                "app:b",
                                "shortcut:y"));

        List<String> ranked =
                AutomaticSortEngine
                        .rankSelectedIdsAcrossPeers(
                                Arrays.asList(
                                        "app:a",
                                        "shortcut:x",
                                        "app:b",
                                        "shortcut:y"),
                                automaticItems,
                                scores,
                                new HashMap<>(),
                                new HashMap<>(),
                                false);

        assertEquals(
                Arrays.asList(
                        "app:b",
                        "shortcut:x",
                        "shortcut:y",
                        "app:a"),
                ranked);
    }

    @Test
    public void manualSaveRestoresAutomaticBaselineOrder() {

        Set<String> automatic =
                new HashSet<>(
                        Arrays.asList(
                                "app:a",
                                "app:b"));

        List<String> persisted =
                AutomaticSortEngine
                        .preserveSelectedBaselineOrder(
                                Arrays.asList(
                                        "shortcut:x",
                                        "app:b",
                                        "app:a",
                                        "shortcut:y"),
                                Arrays.asList(
                                        "app:a",
                                        "shortcut:x",
                                        "app:b",
                                        "shortcut:y"),
                                automatic);

        assertEquals(
                Arrays.asList(
                        "shortcut:x",
                        "app:a",
                        "app:b",
                        "shortcut:y"),
                persisted);
    }

    @Test
    public void categorySlotsCanBeAutomaticWhileOtherSectionsStayPut() {

        Map<String, Integer> scores =
                counts(
                        "category:a", 2,
                        "category:b", 20);

        Set<String> automatic =
                new HashSet<>(
                        Arrays.asList(
                                "category:a",
                                "category:b"));

        List<String> ranked =
                AutomaticSortEngine.rankSelectedIds(
                        Arrays.asList(
                                "favorites",
                                "category:a",
                                "category:b",
                                "shortcuts"),
                        automatic,
                        scores,
                        new HashMap<>(),
                        new HashMap<>(),
                        false);

        assertEquals(
                Arrays.asList(
                        "favorites",
                        "category:b",
                        "category:a",
                        "shortcuts"),
                ranked);
    }

    @Test
    public void allOverviewAreasCanBeRankedTogether() {

        Map<String, Integer> scores =
                counts(
                        "favorites", 8,
                        "category:work", 20,
                        "shortcuts", 12);

        Set<String> automatic =
                new HashSet<>(
                        Arrays.asList(
                                "favorites",
                                "category:work",
                                "shortcuts"));

        List<String> ranked =
                AutomaticSortEngine.rankSelectedIds(
                        Arrays.asList(
                                "favorites",
                                "category:work",
                                "shortcuts"),
                        automatic,
                        scores,
                        new HashMap<>(),
                        new HashMap<>(),
                        false);

        assertEquals(
                Arrays.asList(
                        "category:work",
                        "shortcuts",
                        "favorites"),
                ranked);
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
