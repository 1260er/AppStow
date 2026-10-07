package de.pritcloud.appstow;

import android.content.Context;
import android.view.View;

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
public class UiAppearancePageTest {

    private Context context;

    @Before
    public void clearState() {

        context =
                ApplicationProvider
                        .getApplicationContext();

        context.deleteSharedPreferences(
                "ui_settings");

        context.deleteSharedPreferences(
                "sorting_settings");

        context.deleteSharedPreferences(
                "usage_statistics");
    }

    @Test
    public void appearancePageContainsAllFourSettingGroups() {

        try (ActivityScenario<MainActivity> scenario =
                     ActivityScenario.launch(
                             MainActivity.class)) {

            scenario.onActivity(
                    activity -> {

                        activity.findViewById(
                                        R.id.navUiSettings)
                                .performClick();

                        assertEquals(
                                View.VISIBLE,
                                activity.findViewById(
                                                R.id.uiThemeGroup)
                                        .getVisibility());

                        assertEquals(
                                View.VISIBLE,
                                activity.findViewById(
                                                R.id.uiLanguageGroup)
                                        .getVisibility());

                        assertEquals(
                                View.VISIBLE,
                                activity.findViewById(
                                                R.id.uiControlSideGroup)
                                        .getVisibility());

                        assertEquals(
                                View.VISIBLE,
                                activity.findViewById(
                                                R.id.uiStartBehaviorGroup)
                                        .getVisibility());
                    });
        }
    }
}
