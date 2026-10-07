package de.pritcloud.appstow;

import android.app.AlertDialog;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
    public void appearancePageUsesCenteredRadioGroupsAndLanguageSelection() {

        try (ActivityScenario<MainActivity> scenario =
                     ActivityScenario.launch(
                             MainActivity.class)) {

            scenario.onActivity(
                    activity -> {

                        activity.findViewById(
                                        R.id.navUiSettings)
                                .performClick();

                        RadioGroup themeGroup =
                                activity.findViewById(
                                        R.id.uiThemeGroup);

                        RadioGroup startBehaviorGroup =
                                activity.findViewById(
                                        R.id.uiStartBehaviorGroup);

                        RadioGroup controlSideGroup =
                                activity.findViewById(
                                        R.id.uiControlSideGroup);

                        TextView languageSelection =
                                activity.findViewById(
                                        R.id.uiLanguageSelection);

                        assertEquals(
                                View.VISIBLE,
                                themeGroup.getVisibility());

                        assertEquals(
                                View.VISIBLE,
                                startBehaviorGroup.getVisibility());

                        assertEquals(
                                View.VISIBLE,
                                controlSideGroup.getVisibility());

                        assertEquals(
                                View.VISIBLE,
                                languageSelection.getVisibility());

                        assertEquals(
                                RadioGroup.HORIZONTAL,
                                themeGroup.getOrientation());

                        assertEquals(
                                RadioGroup.HORIZONTAL,
                                startBehaviorGroup.getOrientation());

                        assertEquals(
                                RadioGroup.HORIZONTAL,
                                controlSideGroup.getOrientation());

                        assertEquals(
                                Gravity.CENTER_HORIZONTAL,
                                themeGroup.getGravity()
                                        & Gravity.HORIZONTAL_GRAVITY_MASK);

                        assertEquals(
                                Gravity.CENTER_HORIZONTAL,
                                startBehaviorGroup.getGravity()
                                        & Gravity.HORIZONTAL_GRAVITY_MASK);

                        assertEquals(
                                Gravity.CENTER_HORIZONTAL,
                                controlSideGroup.getGravity()
                                        & Gravity.HORIZONTAL_GRAVITY_MASK);

                        assertEquals(
                                3,
                                themeGroup.getChildCount());

                        assertEquals(
                                3,
                                startBehaviorGroup.getChildCount());

                        assertEquals(
                                2,
                                controlSideGroup.getChildCount());

                        assertEquals(
                                R.id.uiControlSideLeft,
                                controlSideGroup
                                        .getChildAt(0)
                                        .getId());

                        assertEquals(
                                R.id.uiControlSideRight,
                                controlSideGroup
                                        .getChildAt(1)
                                        .getId());

                        assertTrue(
                                languageSelection.length()
                                        > 0);

                        languageSelection.performClick();

                        AlertDialog dialog =
                                ShadowAlertDialog
                                        .getLatestAlertDialog();

                        assertNotNull(
                                dialog);

                        assertTrue(
                                dialog.isShowing());

                        assertNotNull(
                                dialog.getListView());

                        assertEquals(
                                3,
                                dialog.getListView()
                                        .getCount());

                        dialog.dismiss();
                    });
        }
    }
}
