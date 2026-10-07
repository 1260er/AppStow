package de.pritcloud.appstow;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class UiHandednessTest {

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
    public void rightSideRemainsCurrentDefault() {

        try (ActivityScenario<MainActivity> scenario =
                     ActivityScenario.launch(
                             MainActivity.class)) {

            scenario.onActivity(
                    activity -> {

                        assertEquals(
                                View.LAYOUT_DIRECTION_LTR,
                                activity.findViewById(
                                                R.id.mainHeader)
                                        .getLayoutDirection());

                        DrawerLayout.LayoutParams drawerParams =
                                (DrawerLayout.LayoutParams)
                                        activity.findViewById(
                                                        R.id.navigationDrawer)
                                                .getLayoutParams();

                        assertEquals(
                                GravityCompat.END,
                                drawerParams.gravity);

                        FrameLayout.LayoutParams clearParams =
                                (FrameLayout.LayoutParams)
                                        activity.findViewById(
                                                        R.id.appSearchClear)
                                                .getLayoutParams();

                        assertTrue(
                                (clearParams.gravity
                                        & Gravity.END)
                                        == Gravity.END);
                    });
        }
    }

    @Test
    public void leftSideMirrorsReachableMainControls() {

        new UiSettingsStore(
                context)
                .save(
                        new UiSettingsStore.Settings(
                                UiSettingsStore.StartBehavior.NEVER,
                                UiSettingsStore.ControlSide.LEFT));

        try (ActivityScenario<MainActivity> scenario =
                     ActivityScenario.launch(
                             MainActivity.class)) {

            scenario.onActivity(
                    activity -> {

                        assertEquals(
                                View.LAYOUT_DIRECTION_RTL,
                                activity.findViewById(
                                                R.id.mainHeader)
                                        .getLayoutDirection());

                        DrawerLayout.LayoutParams drawerParams =
                                (DrawerLayout.LayoutParams)
                                        activity.findViewById(
                                                        R.id.navigationDrawer)
                                                .getLayoutParams();

                        assertEquals(
                                GravityCompat.START,
                                drawerParams.gravity);

                        FrameLayout.LayoutParams clearParams =
                                (FrameLayout.LayoutParams)
                                        activity.findViewById(
                                                        R.id.appSearchClear)
                                                .getLayoutParams();

                        assertTrue(
                                (clearParams.gravity
                                        & Gravity.START)
                                        == Gravity.START);
                    });
        }
    }

    @Test
    public void mirroredContainerUsesStoredControlSide() {

        View view =
                new View(
                        context);

        UiHandedness.applyContainer(
                context,
                view);

        assertEquals(
                View.LAYOUT_DIRECTION_LTR,
                view.getLayoutDirection());

        new UiSettingsStore(
                context)
                .save(
                        new UiSettingsStore.Settings(
                                UiSettingsStore.StartBehavior.NEVER,
                                UiSettingsStore.ControlSide.LEFT));

        UiHandedness.applyContainer(
                context,
                view);

        assertEquals(
                View.LAYOUT_DIRECTION_RTL,
                view.getLayoutDirection());
    }
}
