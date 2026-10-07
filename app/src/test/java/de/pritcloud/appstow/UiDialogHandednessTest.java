package de.pritcloud.appstow;

import android.app.AlertDialog;
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
public class UiDialogHandednessTest {

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
    }

    @Test
    public void dialogButtonBarFollowsControlSide() {

        try (ActivityScenario<MainActivity> scenario =
                     ActivityScenario.launch(
                             MainActivity.class)) {

            scenario.onActivity(
                    activity -> {

                        AlertDialog rightDialog =
                                new AlertDialog.Builder(
                                        activity)
                                        .setPositiveButton(
                                                "OK",
                                                null)
                                        .setNegativeButton(
                                                "Cancel",
                                                null)
                                        .create();

                        rightDialog.show();

                        UiDialogHandedness.apply(
                                activity,
                                rightDialog);

                        View rightBar =
                                (View) rightDialog
                                        .getButton(
                                                AlertDialog.BUTTON_POSITIVE)
                                        .getParent();

                        assertEquals(
                                View.LAYOUT_DIRECTION_LTR,
                                rightBar.getLayoutDirection());

                        rightDialog.dismiss();

                        new UiSettingsStore(
                                activity)
                                .save(
                                        new UiSettingsStore.Settings(
                                                UiSettingsStore.StartBehavior.NEVER,
                                                UiSettingsStore.ControlSide.LEFT,
                                                UiSettingsStore.ThemeMode.SYSTEM));

                        AlertDialog leftDialog =
                                new AlertDialog.Builder(
                                        activity)
                                        .setPositiveButton(
                                                "OK",
                                                null)
                                        .setNegativeButton(
                                                "Cancel",
                                                null)
                                        .create();

                        leftDialog.show();

                        UiDialogHandedness.apply(
                                activity,
                                leftDialog);

                        View leftBar =
                                (View) leftDialog
                                        .getButton(
                                                AlertDialog.BUTTON_POSITIVE)
                                        .getParent();

                        assertEquals(
                                View.LAYOUT_DIRECTION_RTL,
                                leftBar.getLayoutDirection());

                        leftDialog.dismiss();
                    });
        }
    }
}
