package de.pritcloud.appstow;

import android.content.Context;
import android.content.res.Configuration;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class UiThemeControllerTest {

    @Test
    public void systemThemeLeavesContextUntouched() {

        Context context =
                ApplicationProvider
                        .getApplicationContext();

        assertSame(
                context,
                UiThemeController.wrap(
                        context,
                        UiSettingsStore.ThemeMode.SYSTEM));
    }

    @Test
    public void lightAndDarkOverrideOnlyNightQualifier() {

        Context context =
                ApplicationProvider
                        .getApplicationContext();

        int originalType =
                context.getResources()
                        .getConfiguration()
                        .uiMode
                        & Configuration.UI_MODE_TYPE_MASK;

        Context lightContext =
                UiThemeController.wrap(
                        context,
                        UiSettingsStore.ThemeMode.LIGHT);

        assertEquals(
                Configuration.UI_MODE_NIGHT_NO,
                lightContext.getResources()
                        .getConfiguration()
                        .uiMode
                        & Configuration.UI_MODE_NIGHT_MASK);

        assertEquals(
                originalType,
                lightContext.getResources()
                        .getConfiguration()
                        .uiMode
                        & Configuration.UI_MODE_TYPE_MASK);

        Context darkContext =
                UiThemeController.wrap(
                        context,
                        UiSettingsStore.ThemeMode.DARK);

        assertEquals(
                Configuration.UI_MODE_NIGHT_YES,
                darkContext.getResources()
                        .getConfiguration()
                        .uiMode
                        & Configuration.UI_MODE_NIGHT_MASK);

        assertEquals(
                originalType,
                darkContext.getResources()
                        .getConfiguration()
                        .uiMode
                        & Configuration.UI_MODE_TYPE_MASK);
    }
}
