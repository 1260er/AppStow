package de.pritcloud.appstow;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class UsageStatisticsStoreTest {

    private Context context;

    @Before
    public void setUp() {
        context =
                RuntimeEnvironment
                        .getApplication();

        context.getSharedPreferences(
                        "usage_statistics",
                        Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit();
    }

    @Test
    public void recordsAppsShortcutsAndCategories() {
        UsageStatisticsStore store =
                storeAt(
                        "2026-10-05T12:00:00Z");

        store.recordAppLaunch(
                "com.example.mail",
                "work");

        store.recordAppLaunch(
                "com.example.mail",
                "work");

        store.recordShortcutLaunch(
                "shortcut-weather",
                "tools");

        UsageStatisticsStore.Snapshot snapshot =
                store.getSnapshot(
                        UsageStatisticsStore.Period.ONE_MONTH);

        assertEquals(
                2,
                snapshot.getAppCounts()
                        .get(
                                "com.example.mail")
                        .intValue());

        assertEquals(
                1,
                snapshot.getShortcutCounts()
                        .get(
                                "shortcut-weather")
                        .intValue());

        assertEquals(
                2,
                snapshot.getCategoryCounts()
                        .get("work")
                        .intValue());

        assertEquals(
                1,
                snapshot.getCategoryCounts()
                        .get("tools")
                        .intValue());

        assertEquals(
                3,
                snapshot.getTotalLaunches());
    }

    @Test
    public void periodFiltersUseRollingCalendarMonths() {
        storeAt(
                "2026-03-01T12:00:00Z")
                .recordAppLaunch(
                        "app.old",
                        null);

        storeAt(
                "2026-08-01T12:00:00Z")
                .recordAppLaunch(
                        "app.summer",
                        null);

        storeAt(
                "2026-09-20T12:00:00Z")
                .recordAppLaunch(
                        "app.recent",
                        null);

        UsageStatisticsStore current =
                storeAt(
                        "2026-10-05T12:00:00Z");

        UsageStatisticsStore.Snapshot oneMonth =
                current.getSnapshot(
                        UsageStatisticsStore.Period.ONE_MONTH);

        assertTrue(
                oneMonth.getAppCounts()
                        .containsKey(
                                "app.recent"));

        assertFalse(
                oneMonth.getAppCounts()
                        .containsKey(
                                "app.summer"));

        UsageStatisticsStore.Snapshot threeMonths =
                current.getSnapshot(
                        UsageStatisticsStore.Period.THREE_MONTHS);

        assertTrue(
                threeMonths.getAppCounts()
                        .containsKey(
                                "app.summer"));

        assertFalse(
                threeMonths.getAppCounts()
                        .containsKey(
                                "app.old"));

        UsageStatisticsStore.Snapshot oneYear =
                current.getSnapshot(
                        UsageStatisticsStore.Period.ONE_YEAR);

        assertTrue(
                oneYear.getAppCounts()
                        .containsKey(
                                "app.old"));
    }

    @Test
    public void dataOlderThanOneYearIsRemoved() {
        storeAt(
                "2025-09-01T12:00:00Z")
                .recordAppLaunch(
                        "app.too-old",
                        null);

        UsageStatisticsStore current =
                storeAt(
                        "2026-10-05T12:00:00Z");

        UsageStatisticsStore.Snapshot snapshot =
                current.getSnapshot(
                        UsageStatisticsStore.Period.ONE_YEAR);

        assertFalse(
                snapshot.getAppCounts()
                        .containsKey(
                                "app.too-old"));

        assertTrue(
                context.getSharedPreferences(
                                "usage_statistics",
                                Context.MODE_PRIVATE)
                        .getAll()
                        .isEmpty());
    }

    @Test
    public void exactMonthBoundaryIsIncluded() {
        storeAt(
                "2026-09-05T12:00:00Z")
                .recordAppLaunch(
                        "app.boundary",
                        null);

        UsageStatisticsStore.Snapshot snapshot =
                storeAt(
                        "2026-10-05T12:00:00Z")
                        .getSnapshot(
                                UsageStatisticsStore.Period.ONE_MONTH);

        assertTrue(
                snapshot.getAppCounts()
                        .containsKey(
                                "app.boundary"));
    }

    @Test
    public void exactYearBoundaryIsRetained() {
        storeAt(
                "2025-10-05T12:00:00Z")
                .recordAppLaunch(
                        "app.one-year",
                        null);

        UsageStatisticsStore.Snapshot snapshot =
                storeAt(
                        "2026-10-05T12:00:00Z")
                        .getSnapshot(
                                UsageStatisticsStore.Period.ONE_YEAR);

        assertTrue(
                snapshot.getAppCounts()
                        .containsKey(
                                "app.one-year"));
    }

    @Test
    public void malformedStoredDayIsIgnored() {
        context.getSharedPreferences(
                        "usage_statistics",
                        Context.MODE_PRIVATE)
                .edit()
                .putString(
                        "day_20731",
                        "{broken")
                .commit();

        UsageStatisticsStore.Snapshot snapshot =
                storeAt(
                        "2026-10-05T12:00:00Z")
                        .getSnapshot(
                                UsageStatisticsStore.Period.ONE_YEAR);

        assertTrue(
                snapshot.getAppCounts()
                        .isEmpty());

        assertTrue(
                snapshot.getShortcutCounts()
                        .isEmpty());

        assertTrue(
                snapshot.getCategoryCounts()
                        .isEmpty());
    }

    @Test
    public void futureStoredDayIsIgnored() {
        storeAt(
                "2026-10-06T12:00:00Z")
                .recordAppLaunch(
                        "app.future",
                        null);

        UsageStatisticsStore.Snapshot snapshot =
                storeAt(
                        "2026-10-05T12:00:00Z")
                        .getSnapshot(
                                UsageStatisticsStore.Period.ONE_YEAR);

        assertFalse(
                snapshot.getAppCounts()
                        .containsKey(
                                "app.future"));
    }

    @Test
    public void clearRemovesAllStatistics() {
        UsageStatisticsStore store =
                storeAt(
                        "2026-10-05T12:00:00Z");

        store.recordAppLaunch(
                "com.example.app",
                null);

        store.clear();

        assertTrue(
                store.getSnapshot(
                                UsageStatisticsStore.Period.ONE_YEAR)
                        .getAppCounts()
                        .isEmpty());
    }

    private UsageStatisticsStore storeAt(
            String instant) {

        return new UsageStatisticsStore(
                context,
                Clock.fixed(
                        Instant.parse(
                                instant),
                        ZoneOffset.UTC));
    }
}
