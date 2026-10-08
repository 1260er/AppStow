package de.pritcloud.appstow;

import android.content.Context;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
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

                        View timeProfileRow =
                                activity.findViewById(
                                        R.id.sortingTimeProfileRow);

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
                                        timeProfileRow)
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
    public void timeProfileHelpOpensCorrectSectionWithoutToggling() {

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

                        ViewGroup group =
                                activity.findViewById(
                                        R.id.sortingTimeSettings);

                        TextView brief =
                                activity.findViewById(
                                        R.id.sortingTimeProfileBrief);

                        LinearLayout row =
                                activity.findViewById(
                                        R.id.sortingTimeProfileRow);

                        CheckBox option =
                                activity.findViewById(
                                        R.id.sortingTimeProfileEnabled);

                        ImageButton help =
                                activity.findViewById(
                                        R.id.sortingTimeProfileHelp);

                        assertEquals(4, group.getChildCount());
                        assertEquals(0, group.indexOfChild(brief));
                        assertEquals(1, group.indexOfChild(row));
                        assertEquals(2, row.getChildCount());

                        assertEquals(
                                activity.getResources()
                                        .getDimensionPixelSize(
                                                R.dimen.icon_button_size),
                                row.getLayoutParams().height);

                        assertFalse(option.isChecked());
                        assertTrue(help.performClick());
                        assertFalse(option.isChecked());

                        assertEquals(
                                activity.getString(R.string.nav_help),
                                ((TextView) activity.findViewById(
                                        R.id.pageTitle))
                                        .getText().toString());

                        TextView heading =
                                activity.findViewById(
                                        R.id.helpTimeProfileSection);

                        assertNotNull(heading);
                        assertEquals(
                                activity.getString(
                                        R.string.help_sort_time_profile_title),
                                heading.getText().toString());

                        assertEquals(
                                View.VISIBLE,
                                activity.findViewById(
                                        R.id.helpManagement)
                                        .getVisibility());
                    });

            Shadows.shadowOf(Looper.getMainLooper()).idle();

            scenario.onActivity(
                    activity -> {
                        ScrollView help =
                                activity.findViewById(
                                        R.id.helpManagement);

                        assertTrue(
                                "Zeitprofil-Hilfe wurde nicht angesteuert.",
                                help.getScrollY() > 0);
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
    public void automaticModePersistsSortingOptions() {

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

                        SortingSettingsStore.Settings settings =
                                new SortingSettingsStore(
                                        activity)
                                        .load();

                        assertEquals(
                                SortingSettingsStore.Mode.AUTOMATIC,
                                settings.mode);

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
