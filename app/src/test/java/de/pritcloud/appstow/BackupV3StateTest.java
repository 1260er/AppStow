package de.pritcloud.appstow;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.File;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class BackupV3StateTest {

    private Context context;

    @Before
    public void setUp() {

        context =
                RuntimeEnvironment
                        .getApplication();

        clearPreferences(
                "categories",
                "favorites",
                "shortcuts",
                "overview_order",
                "section_item_order",
                "overview_display",
                "sorting_settings",
                "ui_settings",
                "statistics_display",
                "usage_statistics");
    }

    @Test
    public void fullSettingsRestoreAndUsageStatisticsReset()
            throws Exception {

        SortingSettingsStore sortingStore =
                new SortingSettingsStore(
                        context);

        sortingStore.save(
                new SortingSettingsStore.Settings(
                        SortingSettingsStore.Mode.AUTOMATIC,
                        false,
                        true,
                        false,
                        true,
                        true,
                        7,
                        22,
                        12,
                        false,
                        true,
                        14));

        sortingStore.setLastSuggestionHandledAt(
                123456L);

        UiSettingsStore uiStore =
                new UiSettingsStore(
                        context);

        uiStore.save(
                new UiSettingsStore.Settings(
                        UiSettingsStore.StartBehavior.ALWAYS_FOREGROUND,
                        UiSettingsStore.ControlSide.LEFT,
                        UiSettingsStore.ThemeMode.DARK));

        SharedPreferences statisticsDisplay =
                context.getSharedPreferences(
                        "statistics_display",
                        Context.MODE_PRIVATE);

        assertTrue(
                statisticsDisplay.edit()
                        .putString(
                                "statistics_period",
                                UsageStatisticsStore.Period.ONE_YEAR.name())
                        .putInt(
                                "statistics_top_limit",
                                -1)
                        .putString(
                                "statistics_card_order_v2",
                                "total,top_apps,top_categories")
                        .commit());

        SharedPreferences usageStatistics =
                context.getSharedPreferences(
                        "usage_statistics",
                        Context.MODE_PRIVATE);

        assertTrue(
                usageStatistics.edit()
                        .putString(
                                "day_1",
                                "{\"apps\":{\"com.example.app\":4}}")
                        .commit());

        File file =
                File.createTempFile(
                        "appstow-v3-state-",
                        ".json",
                        context.getCacheDir());

        try {
            Uri uri =
                    Uri.fromFile(
                            file);

            BackupManager.writeBackup(
                    context,
                    uri);

            JSONObject backup =
                    BackupManager.readBackup(
                            context,
                            uri);

            JSONObject settings =
                    backup.getJSONObject(
                            "settings");

            assertEquals(
                    "AUTOMATIC",
                    settings.getJSONObject(
                                    "sorting")
                            .getString(
                                    "mode"));

            assertEquals(
                    "ALWAYS_FOREGROUND",
                    settings.getJSONObject(
                                    "ui")
                            .getString(
                                    "startBehavior"));

            assertEquals(
                    UiLocaleController
                            .getLanguageMode(
                                    context)
                            .name(),
                    settings.getJSONObject(
                                    "ui")
                            .getString(
                                    "language"));

            assertEquals(
                    "ONE_YEAR",
                    settings.getJSONObject(
                                    "statisticsDisplay")
                            .getString(
                                    "period"));

            assertTrue(
                    !backup.has(
                            "usageStatistics"));

            sortingStore.save(
                    SortingSettingsStore.Settings.defaults());

            sortingStore.setLastSuggestionHandledAt(
                    999999L);

            uiStore.save(
                    new UiSettingsStore.Settings(
                            UiSettingsStore.StartBehavior.NEVER,
                            UiSettingsStore.ControlSide.RIGHT,
                            UiSettingsStore.ThemeMode.LIGHT));

            assertTrue(
                    statisticsDisplay.edit()
                            .clear()
                            .putString(
                                    "statistics_period",
                                    UsageStatisticsStore.Period.ONE_MONTH.name())
                            .putInt(
                                    "statistics_top_limit",
                                    5)
                            .commit());

            assertTrue(
                    usageStatistics.edit()
                            .clear()
                            .putString(
                                    "day_2",
                                    "{\"apps\":{\"com.other.app\":9}}")
                            .commit());

            BackupManager.restoreBackup(
                    context,
                    backup,
                    Collections.emptySet());

            SortingSettingsStore.Settings restoredSorting =
                    sortingStore.load();

            assertEquals(
                    SortingSettingsStore.Mode.AUTOMATIC,
                    restoredSorting.mode);

            assertTrue(
                    !restoredSorting.semiFavorites);

            assertTrue(
                    restoredSorting.semiCategories);

            assertTrue(
                    !restoredSorting.semiApps);

            assertTrue(
                    restoredSorting.semiShortcuts);

            assertTrue(
                    restoredSorting.timeProfileEnabled);

            assertEquals(
                    7,
                    restoredSorting.dayStartHour);

            assertEquals(
                    22,
                    restoredSorting.eveningStartHour);

            assertEquals(
                    12,
                    restoredSorting.automaticFavoriteCount);

            assertTrue(
                    restoredSorting.suggestionsEnabled);

            assertEquals(
                    14,
                    restoredSorting.suggestionIntervalDays);

            assertEquals(
                    0L,
                    sortingStore.getLastSuggestionHandledAt());

            UiSettingsStore.Settings restoredUi =
                    uiStore.load();

            assertEquals(
                    UiSettingsStore.StartBehavior.ALWAYS_FOREGROUND,
                    restoredUi.startBehavior);

            assertEquals(
                    UiSettingsStore.ControlSide.LEFT,
                    restoredUi.controlSide);

            assertEquals(
                    UiSettingsStore.ThemeMode.DARK,
                    restoredUi.themeMode);

            assertEquals(
                    UsageStatisticsStore.Period.ONE_YEAR.name(),
                    statisticsDisplay.getString(
                            "statistics_period",
                            null));

            assertEquals(
                    -1,
                    statisticsDisplay.getInt(
                            "statistics_top_limit",
                            0));

            assertEquals(
                    "total,top_apps,top_categories",
                    statisticsDisplay.getString(
                            "statistics_card_order_v2",
                            null));

            assertTrue(
                    usageStatistics.getAll()
                            .isEmpty());

        } finally {
            file.delete();
        }
    }

    private void clearPreferences(
            String... names) {

        for (String name :
                names) {

            context.getSharedPreferences(
                            name,
                            Context.MODE_PRIVATE)
                    .edit()
                    .clear()
                    .commit();
        }
    }
}
