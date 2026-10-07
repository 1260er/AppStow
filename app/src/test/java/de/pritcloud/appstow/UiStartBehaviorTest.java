package de.pritcloud.appstow;

import android.content.Context;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class UiStartBehaviorTest {

    private Context context;

    @Before
    public void clearState() {

        context =
                ApplicationProvider
                        .getApplicationContext();

        String[] preferences = {
                "ui_settings",
                "sorting_settings",
                "usage_statistics",
                "categories",
                "shortcuts",
                "overview_display",
                "statistics_display"
        };

        for (String name :
                preferences) {

            context.deleteSharedPreferences(
                    name);
        }
    }

    @Test
    public void neverDoesNotOpenFavoritesAutomatically() {

        save(
                UiSettingsStore.StartBehavior.NEVER);

        try (ActivityScenario<MainActivity> scenario =
                     ActivityScenario.launch(
                             MainActivity.class)) {

            assertEquals(
                    0,
                    favoriteAccessCount(
                            scenario));
        }
    }

    @Test
    public void startOnlyCountsInitialStartButNotRecreation() {

        save(
                UiSettingsStore.StartBehavior.START_ONLY);

        try (ActivityScenario<MainActivity> scenario =
                     ActivityScenario.launch(
                             MainActivity.class)) {

            assertEquals(
                    1,
                    favoriteAccessCount(
                            scenario));

            scenario.recreate();

            assertEquals(
                    1,
                    favoriteAccessCount(
                            scenario));
        }
    }

    @Test
    public void alwaysForegroundCountsReturnButNotRecreation() {

        save(
                UiSettingsStore.StartBehavior.ALWAYS_FOREGROUND);

        try (ActivityScenario<MainActivity> scenario =
                     ActivityScenario.launch(
                             MainActivity.class)) {

            assertEquals(
                    1,
                    favoriteAccessCount(
                            scenario));

            scenario.moveToState(
                    Lifecycle.State.CREATED);

            scenario.moveToState(
                    Lifecycle.State.RESUMED);

            assertEquals(
                    2,
                    favoriteAccessCount(
                            scenario));

            scenario.recreate();

            assertEquals(
                    2,
                    favoriteAccessCount(
                            scenario));
        }
    }

    private void save(
            UiSettingsStore.StartBehavior behavior) {

        new UiSettingsStore(
                context)
                .save(
                        new UiSettingsStore.Settings(
                                behavior));
    }

    private int favoriteAccessCount(
            ActivityScenario<MainActivity> scenario) {

        int[] result = {
                -1
        };

        scenario.onActivity(
                activity -> {

                    UsageStatisticsStore store =
                            privateField(
                                    activity,
                                    "usageStatisticsStore",
                                    UsageStatisticsStore.class);

                    UsageStatisticsStore.SortingSnapshot snapshot =
                            store.getSortingSnapshot(
                                    store.getCurrentTimeProfile(
                                            8,
                                            21),
                                    8,
                                    21);

                    result[0] =
                            snapshot.getProfileSectionLaunches()
                                    .getOrDefault(
                                            "favorites",
                                            0);
                });

        return result[0];
    }

    private static <T> T privateField(
            Object target,
            String name,
            Class<T> type) {

        try {

            java.lang.reflect.Field field =
                    target.getClass()
                            .getDeclaredField(
                                    name);

            field.setAccessible(
                    true);

            return type.cast(
                    field.get(
                            target));

        } catch (ReflectiveOperationException exception) {

            throw new AssertionError(
                    exception);
        }
    }
}
