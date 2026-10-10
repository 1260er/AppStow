package de.pritcloud.appstow;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class AutomaticSortingSnapshotStoreTest {

    private Context context;
    private AutomaticSortingSnapshotStore store;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        context.getSharedPreferences(
                "sorting_settings",
                Context.MODE_PRIVATE)
                .edit().clear().commit();

        store = new AutomaticSortingSnapshotStore(context);
    }

    private static UsageStatisticsStore.SortingSnapshot sample(
            int score) {

        UsageStatisticsStore.Snapshot overall =
                new UsageStatisticsStore.Snapshot(
                        Map.of("app.example", score),
                        Map.of(),
                        Map.of());

        UsageStatisticsStore.Snapshot empty =
                new UsageStatisticsStore.Snapshot(
                        Map.of(), Map.of(), Map.of());

        return new UsageStatisticsStore.SortingSnapshot(
                overall, empty, empty,
                Map.of("favorites", score),
                Map.of(), Map.of());
    }

    private static int score(
            UsageStatisticsStore.SortingSnapshot snapshot) {

        return snapshot.getOverallWeighted()
                .getAppCounts()
                .get("app.example");
    }

    @Test
    public void semiAutomaticFreezesItemAndSectionScoresForSevenDays() {

        SortingSettingsStore settings =
                new SortingSettingsStore(context);

        settings.save(
                new SortingSettingsStore.Settings(
                        SortingSettingsStore.Mode.SEMI_AUTOMATIC,
                        true, true, true, true,
                        false, 8, 21, 10, false));

        long start = 1000000000L;

        UsageStatisticsStore.SortingSnapshot initial =
                store.getOrCreateAt(
                        null, sample(10), start);

        assertEquals(10, score(initial));
        assertEquals(10,
                initial.getOverallSectionCounts()
                        .get("favorites").intValue());

        UsageStatisticsStore.SortingSnapshot unchanged =
                store.getOrCreateAt(
                        null,
                        sample(90),
                        start + 3L * 24L * 60L * 60L * 1000L);

        assertEquals(10, score(unchanged));
        assertEquals(10,
                unchanged.getOverallSectionCounts()
                        .get("favorites").intValue());

        UsageStatisticsStore.SortingSnapshot refreshed =
                store.getOrCreateAt(
                        null,
                        sample(90),
                        start + AutomaticSortingSnapshotStore
                                .REFRESH_INTERVAL_MILLIS);

        assertEquals(90, score(refreshed));
        assertEquals(90,
                refreshed.getOverallSectionCounts()
                        .get("favorites").intValue());
    }

    @Test
    public void scoresRemainFrozenForSevenDays() {
        long start = 1000000000L;

        assertEquals(10, score(store.getOrCreateAt(
                null, sample(10), start)));

        assertEquals(10, score(store.getOrCreateAt(
                null, sample(50),
                start + AutomaticSortingSnapshotStore
                        .REFRESH_INTERVAL_MILLIS - 1L)));

        assertEquals(50, score(store.getOrCreateAt(
                null, sample(50),
                start + AutomaticSortingSnapshotStore
                        .REFRESH_INTERVAL_MILLIS)));
    }

    @Test
    public void frozenScoresSurviveStoreRecreation() {
        long start = 1000000000L;
        store.getOrCreateAt(null, sample(10), start);

        AutomaticSortingSnapshotStore reopened =
                new AutomaticSortingSnapshotStore(context);

        assertEquals(10, score(reopened.getOrCreateAt(
                null, sample(99), start + 1000L)));
    }

    @Test
    public void dayAndEveningRemainIndependent() {
        long start = 1000000000L;

        store.getOrCreateAt(
                UsageStatisticsStore.TimeProfile.DAY,
                sample(10), start);

        assertEquals(20, score(store.getOrCreateAt(
                UsageStatisticsStore.TimeProfile.EVENING,
                sample(20), start + 1000L)));

        assertEquals(10, score(store.getOrCreateAt(
                UsageStatisticsStore.TimeProfile.DAY,
                sample(99), start + 2000L)));
    }

    @Test
    public void thirtyDayIntervalFreezesScores() {
        long start = 1000000000L;

        store.getOrCreateAt(null, sample(10), start, 30);

        assertEquals(10, score(store.getOrCreateAt(
                null, sample(90),
                start + 8L * AutomaticSortingSnapshotStore.DAY_MILLIS,
                30)));

        assertEquals(90, score(store.getOrCreateAt(
                null, sample(90),
                start + 30L * AutomaticSortingSnapshotStore.DAY_MILLIS,
                30)));
    }

    @Test
    public void reducingIntervalUsesOriginalTimestamp() {
        long start = 1000000000L;

        store.getOrCreateAt(null, sample(10), start, 30);

        assertEquals(90, score(store.getOrCreateAt(
                null, sample(90),
                start + 8L * AutomaticSortingSnapshotStore.DAY_MILLIS,
                7)));
    }

    @Test
    public void firstRealLaunchReplacesEmptySnapshot() {
        long start = 1000000000L;

        store.getOrCreateAt(null, sample(0), start, 30);

        assertEquals(4, score(store.getOrCreateAt(
                null, sample(4),
                start + AutomaticSortingSnapshotStore.DAY_MILLIS,
                30)));
    }

    @Test
    public void changingSortModeClearsOldSnapshot() {
        long start = 1000000000L;

        SortingSettingsStore settings =
                new SortingSettingsStore(context);

        SortingSettingsStore.Settings automatic =
                new SortingSettingsStore.Settings(
                        SortingSettingsStore.Mode.AUTOMATIC,
                        true, true, true, true,
                        false, 8, 21, 10, false);

        settings.save(automatic);

        store.getOrCreateAt(null, sample(10), start);

        settings.save(SortingSettingsStore.Settings.defaults());
        settings.save(automatic);

        assertEquals(77, score(store.getOrCreateAt(
                null, sample(77), start + 1000L)));
    }

    @Test
    public void changingTimeProfileSettingsClearsSnapshot() {
        long start = 1000000000L;

        SortingSettingsStore settings =
                new SortingSettingsStore(context);

        settings.save(new SortingSettingsStore.Settings(
                SortingSettingsStore.Mode.AUTOMATIC,
                true, true, true, true,
                false, 8, 21, 10, false));

        store.getOrCreateAt(null, sample(10), start);

        settings.save(new SortingSettingsStore.Settings(
                SortingSettingsStore.Mode.AUTOMATIC,
                true, true, true, true,
                true, 8, 21, 10, false));

        assertEquals(60, score(store.getOrCreateAt(
                null, sample(60), start + 1000L)));
    }
}
