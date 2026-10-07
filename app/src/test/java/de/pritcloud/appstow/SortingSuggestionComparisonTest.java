package de.pritcloud.appstow;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

public class SortingSuggestionComparisonTest {

    @Test
    public void categorySwapMarksOnlyChangedTargetPositions() {

        assertArrayEquals(
                new boolean[] {
                        false,
                        true,
                        true,
                        false
                },
                SortingSuggestionComparison
                        .changedTargetLines(
                                "1. Favoriten\n2. Office\n3. Health\n4. Install",
                                "1. Favoriten\n2. Health\n3. Office\n4. Install"));
    }

    @Test
    public void favoriteReassignmentMarksAddedTargets() {

        assertArrayEquals(
                new boolean[] {
                        true,
                        true
                },
                SortingSuggestionComparison
                        .changedTargetLines(
                                "\u2212 Home Assistant\n\u2212 openCook DEV",
                                "+ Feeder\n+ Mullvad VPN"));
    }

    @Test
    public void emptyTargetMarkerStaysNeutral() {

        assertArrayEquals(
                new boolean[] {
                        false
                },
                SortingSuggestionComparison
                        .changedTargetLines(
                                "\u2212 Home Assistant",
                                "\u2014"));
    }
}
