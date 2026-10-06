package de.pritcloud.appstow;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class SortingSettingsStoreTest {

    private Context context;
    private SortingSettingsStore store;

    @Before
    public void setUp() {
        context =
                RuntimeEnvironment.getApplication();

        context.getSharedPreferences(
                        "sorting_settings",
                        Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit();

        store =
                new SortingSettingsStore(context);
    }

    @Test
    public void defaultsAreSafe() {
        SortingSettingsStore.Settings settings =
                store.load();

        assertEquals(
                SortingSettingsStore.Mode.MANUAL,
                settings.mode);

        assertTrue(settings.semiFavorites);
        assertTrue(settings.semiCategories);
        assertTrue(settings.semiApps);
        assertTrue(settings.semiShortcuts);

        assertFalse(settings.timeProfileEnabled);

        assertEquals(
                8,
                settings.dayStartHour);

        assertEquals(
                21,
                settings.eveningStartHour);

        assertEquals(
                10,
                settings.automaticFavoriteCount);

        assertFalse(
                settings.alwaysStartFavorites);
    }

    @Test
    public void settingsPersistTogether() {
        store.save(
                new SortingSettingsStore.Settings(
                        SortingSettingsStore.Mode.SEMI_AUTOMATIC,
                        true,
                        false,
                        true,
                        false,
                        true,
                        7,
                        20,
                        12,
                        true));

        SortingSettingsStore.Settings settings =
                new SortingSettingsStore(context)
                        .load();

        assertEquals(
                SortingSettingsStore.Mode.SEMI_AUTOMATIC,
                settings.mode);

        assertTrue(settings.semiFavorites);
        assertFalse(settings.semiCategories);
        assertTrue(settings.semiApps);
        assertFalse(settings.semiShortcuts);
        assertTrue(settings.timeProfileEnabled);

        assertEquals(7, settings.dayStartHour);
        assertEquals(20, settings.eveningStartHour);
        assertEquals(12, settings.automaticFavoriteCount);

        assertTrue(settings.alwaysStartFavorites);
    }

    @Test
    public void corruptValuesFallBackSafely() {
        context.getSharedPreferences(
                        "sorting_settings",
                        Context.MODE_PRIVATE)
                .edit()
                .putString("mode", "UNKNOWN")
                .putInt("day_start_hour", 24)
                .putInt("evening_start_hour", -1)
                .putInt("automatic_favorite_count", 0)
                .commit();

        SortingSettingsStore.Settings settings =
                store.load();

        assertEquals(
                SortingSettingsStore.Mode.MANUAL,
                settings.mode);

        assertEquals(
                SortingSettingsStore.DEFAULT_DAY_START_HOUR,
                settings.dayStartHour);

        assertEquals(
                SortingSettingsStore.DEFAULT_EVENING_START_HOUR,
                settings.eveningStartHour);

        assertEquals(
                SortingSettingsStore.DEFAULT_AUTOMATIC_FAVORITE_COUNT,
                settings.automaticFavoriteCount);
    }

    @Test
    public void equalProfileStartsFallBackToDefaults() {
        context.getSharedPreferences(
                        "sorting_settings",
                        Context.MODE_PRIVATE)
                .edit()
                .putInt("day_start_hour", 10)
                .putInt("evening_start_hour", 10)
                .commit();

        SortingSettingsStore.Settings settings =
                store.load();

        assertEquals(
                SortingSettingsStore.DEFAULT_DAY_START_HOUR,
                settings.dayStartHour);

        assertEquals(
                SortingSettingsStore.DEFAULT_EVENING_START_HOUR,
                settings.eveningStartHour);
    }

    @Test
    public void saveRejectsEqualProfileStarts() {
        try {
            store.save(
                    new SortingSettingsStore.Settings(
                            SortingSettingsStore.Mode.AUTOMATIC,
                            true,
                            true,
                            true,
                            true,
                            true,
                            8,
                            8,
                            10,
                            false));

            fail("Equal profile starts were accepted.");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }

    @Test
    public void saveRejectsInvalidFavoriteCount() {
        try {
            store.save(
                    new SortingSettingsStore.Settings(
                            SortingSettingsStore.Mode.AUTOMATIC,
                            true,
                            true,
                            true,
                            true,
                            false,
                            8,
                            21,
                            0,
                            false));

            fail("Invalid favorite count was accepted.");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }
}
