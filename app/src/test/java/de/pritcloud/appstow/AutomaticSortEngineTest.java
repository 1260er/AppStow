package de.pritcloud.appstow;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
