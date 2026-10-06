package de.pritcloud.appstow;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class SortingPageTest {

    @Before
    public void clearSortingSettings() {

        Context context =
                ApplicationProvider
                        .getApplicationContext();

        context.deleteSharedPreferences(
                "sorting_settings");
    }

    @Test
    public void pageStartsInManualMode() {

        try (ActivityScenario<MainActivity> scenario =
                     ActivityScenario.launch(
                             MainActivity.class)) {

            scenario.onActivity(
                    activity -> {

                        activity.findViewById(
                                        R.id.navSorting)
                                .performClick();

                        TextView title =
                                activity.findViewById(
                                        R.id.pageTitle);

                        assertEquals(
                                activity.getString(
                                        R.string.nav_sorting),
                                title.getText()
                                        .toString());

                        RadioGroup modes =
                                activity.findViewById(
                                        R.id.sortingModeGroup);

                        assertEquals(
                                R.id.sortingModeManual,
                                modes.getCheckedRadioButtonId());

                        assertEquals(
                                View.VISIBLE,
                                activity.findViewById(
                                                R.id.sortingManualSettings)
                                        .getVisibility());

                        assertEquals(
                                View.GONE,
                                activity.findViewById(
                                                R.id.sortingSemiSettings)
                                        .getVisibility());

                        assertEquals(
                                View.GONE,
                                activity.findViewById(
                                                R.id.sortingAutomaticSettings)
                                        .getVisibility());

                        assertEquals(
                                View.GONE,
                                activity.findViewById(
                                                R.id.sortingTimeSettings)
                                        .getVisibility());
                    });
        }
    }

    @Test
    public void manualSuggestionControlsAreAvailableAndPersist() {

        try (ActivityScenario<MainActivity> scenario =
                     ActivityScenario.launch(
                             MainActivity.class)) {

            scenario.onActivity(
                    activity -> {

                        activity.findViewById(
                                        R.id.navSorting)
                                .performClick();

                        CheckBox suggestions =
                                activity.findViewById(
                                        R.id.sortingSuggestionsEnabled);

                        assertEquals(
                                View.VISIBLE,
                                suggestions.getVisibility());

                        suggestions.performClick();

                        SortingSettingsStore.Settings settings =
                                new SortingSettingsStore(
                                        activity)
                                        .load();

                        assertTrue(
                                settings.suggestionsEnabled);

                        assertEquals(
                                30,
                                settings.suggestionIntervalDays);

                        assertEquals(
                                View.VISIBLE,
                                activity.findViewById(
                                                R.id.sortingSuggestionInterval)
                                        .getVisibility());

                        assertEquals(
                                View.VISIBLE,
                                activity.findViewById(
                                                R.id.sortingSuggestionFavoriteCount)
                                        .getVisibility());

                        assertEquals(
                                View.VISIBLE,
                                activity.findViewById(
                                                R.id.sortingSuggestionCheckNow)
                                        .getVisibility());

                        assertEquals(
                                View.VISIBLE,
                                activity.findViewById(
                                                R.id.sortingAlwaysStartFavorites)
                                        .getVisibility());
                    });

            scenario.recreate();

            scenario.onActivity(
                    activity -> {

                        CheckBox suggestions =
                                activity.findViewById(
                                        R.id.sortingSuggestionsEnabled);

                        assertTrue(
                                suggestions.isChecked());
                    });
        }
    }

    @Test
    public void sortingControlsUseConsistentVisualOrder() {

        try (ActivityScenario<MainActivity> scenario =
                     ActivityScenario.launch(
                             MainActivity.class)) {

            scenario.onActivity(
                    activity -> {

                        activity.findViewById(
                                        R.id.navSorting)
                                .performClick();

                        ViewGroup manual =
                                activity.findViewById(
                                        R.id.sortingManualSettings);

                        View manualFavoriteCount =
                                activity.findViewById(
                                        R.id.sortingSuggestionFavoriteCount);

                        View interval =
                                activity.findViewById(
                                        R.id.sortingSuggestionInterval);

                        assertTrue(
                                manual.indexOfChild(
                                        manualFavoriteCount)
                                        < manual.indexOfChild(
                                        interval));

                        View automatic =
                                activity.findViewById(
                                        R.id.sortingAutomaticSettings);

                        View timeSettings =
                                activity.findViewById(
                                        R.id.sortingTimeSettings);

                        ViewGroup parent =
                                (ViewGroup)
                                        automatic.getParent();

                        assertTrue(
                                parent.indexOfChild(
                                        automatic)
                                        < parent.indexOfChild(
                                        timeSettings));
                    });
        }
    }

    @Test
    public void automaticControlsKeepSwitchBeforeValues() {

        try (ActivityScenario<MainActivity> scenario =
                     ActivityScenario.launch(
                             MainActivity.class)) {

            scenario.onActivity(
                    activity -> {

                        activity.findViewById(
                                        R.id.navSorting)
                                .performClick();

                        activity.findViewById(
                                        R.id.sortingModeAutomatic)
                                .performClick();

                        ViewGroup timeGroup =
                                activity.findViewById(
                                        R.id.sortingTimeSettings);

                        View timeProfile =
                                activity.findViewById(
                                        R.id.sortingTimeProfileEnabled);

                        View favoriteCount =
                                activity.findViewById(
                                        R.id.sortingFavoriteCount);

                        View timeDetails =
                                activity.findViewById(
                                        R.id.sortingTimeDetails);

                        assertEquals(
                                View.VISIBLE,
                                favoriteCount.getVisibility());

                        assertTrue(
                                timeGroup.indexOfChild(
                                        timeProfile)
                                        < timeGroup.indexOfChild(
                                        favoriteCount));

                        assertTrue(
                                timeGroup.indexOfChild(
                                        favoriteCount)
                                        < timeGroup.indexOfChild(
                                        timeDetails));

                        activity.findViewById(
                                        R.id.sortingModeSemi)
                                .performClick();

                        assertEquals(
                                View.GONE,
                                favoriteCount.getVisibility());
                    });
        }
    }

    @Test
    public void semiAutomaticControlsPersistAcrossRecreation() {

        try (ActivityScenario<MainActivity> scenario =
                     ActivityScenario.launch(
                             MainActivity.class)) {

            scenario.onActivity(
                    activity -> {

                        activity.findViewById(
                                        R.id.navSorting)
                                .performClick();

                        activity.findViewById(
                                        R.id.sortingModeSemi)
                                .performClick();

                        CheckBox categories =
                                activity.findViewById(
                                        R.id.sortingSemiCategories);

                        CheckBox timeProfile =
                                activity.findViewById(
                                        R.id.sortingTimeProfileEnabled);

                        categories.performClick();
                        timeProfile.performClick();

                        SortingSettingsStore.Settings settings =
                                new SortingSettingsStore(
                                        activity)
                                        .load();

                        assertEquals(
                                SortingSettingsStore.Mode.SEMI_AUTOMATIC,
                                settings.mode);

                        assertFalse(
                                settings.semiCategories);

                        assertTrue(
                                settings.timeProfileEnabled);
                    });

            scenario.recreate();

            scenario.onActivity(
                    activity -> {

                        RadioGroup modes =
                                activity.findViewById(
                                        R.id.sortingModeGroup);

                        assertEquals(
                                R.id.sortingModeSemi,
                                modes.getCheckedRadioButtonId());

                        assertFalse(
                                ((CheckBox) activity.findViewById(
                                                R.id.sortingSemiCategories))
                                        .isChecked());

                        assertTrue(
                                ((CheckBox) activity.findViewById(
                                                R.id.sortingTimeProfileEnabled))
                                        .isChecked());

                        assertEquals(
                                View.VISIBLE,
                                activity.findViewById(
                                                R.id.sortingSemiSettings)
                                        .getVisibility());

                        assertEquals(
                                View.VISIBLE,
                                activity.findViewById(
                                                R.id.sortingTimeDetails)
                                        .getVisibility());
                    });
        }
    }

    @Test
    public void automaticModePersistsAutomaticOptions() {

        try (ActivityScenario<MainActivity> scenario =
                     ActivityScenario.launch(
                             MainActivity.class)) {

            scenario.onActivity(
                    activity -> {

                        activity.findViewById(
                                        R.id.navSorting)
                                .performClick();

                        activity.findViewById(
                                        R.id.sortingModeAutomatic)
                                .performClick();

                        CheckBox alwaysStart =
                                activity.findViewById(
                                        R.id.sortingAlwaysStartFavorites);

                        alwaysStart.performClick();

                        SortingSettingsStore.Settings settings =
                                new SortingSettingsStore(
                                        activity)
                                        .load();

                        assertEquals(
                                SortingSettingsStore.Mode.AUTOMATIC,
                                settings.mode);

                        assertTrue(
                                settings.alwaysStartFavorites);

                        assertEquals(
                                View.VISIBLE,
                                activity.findViewById(
                                                R.id.sortingAutomaticSettings)
                                        .getVisibility());

                        assertEquals(
                                View.VISIBLE,
                                activity.findViewById(
                                                R.id.sortingTimeSettings)
                                        .getVisibility());
                    });
        }
    }
}
