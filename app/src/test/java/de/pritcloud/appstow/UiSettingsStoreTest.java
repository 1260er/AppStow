package de.pritcloud.appstow;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class UiSettingsStoreTest {

    private Context context;

    @Before
    public void setUp() {

        context =
                RuntimeEnvironment
                        .getApplication();

        context.deleteSharedPreferences(
                "ui_settings");

        context.deleteSharedPreferences(
                "sorting_settings");
    }

    @Test
    public void defaultPreservesPreviousDefaultBehavior() {

        UiSettingsStore.Settings settings =
                new UiSettingsStore(
                        context)
                        .load();

        assertEquals(
                UiSettingsStore.StartBehavior.NEVER,
                settings.startBehavior);

        assertEquals(
                UiSettingsStore.ControlSide.RIGHT,
                settings.controlSide);

        assertEquals(
                UiSettingsStore.ThemeMode.SYSTEM,
                settings.themeMode);
    }

    @Test
    public void legacyEnabledValueMigratesToStartOnly() {

        context.getSharedPreferences(
                        "sorting_settings",
                        Context.MODE_PRIVATE)
                .edit()
                .putBoolean(
                        "always_start_favorites",
                        true)
                .commit();

        assertEquals(
                UiSettingsStore.StartBehavior.START_ONLY,
                new UiSettingsStore(
                        context)
                        .load()
                        .startBehavior);
    }

    @Test
    public void explicitNewValueOverridesLegacyValue() {

        context.getSharedPreferences(
                        "sorting_settings",
                        Context.MODE_PRIVATE)
                .edit()
                .putBoolean(
                        "always_start_favorites",
                        true)
                .commit();

        UiSettingsStore store =
                new UiSettingsStore(
                        context);

        store.save(
                new UiSettingsStore.Settings(
                        UiSettingsStore.StartBehavior.ALWAYS_FOREGROUND));

        assertEquals(
                UiSettingsStore.StartBehavior.ALWAYS_FOREGROUND,
                new UiSettingsStore(
                        context)
                        .load()
                        .startBehavior);
    }

    @Test
    public void controlSidePersistsAndCorruptValueFallsBackRight() {

        UiSettingsStore store =
                new UiSettingsStore(
                        context);

        store.save(
                new UiSettingsStore.Settings(
                        UiSettingsStore.StartBehavior.START_ONLY,
                        UiSettingsStore.ControlSide.LEFT));

        UiSettingsStore.Settings saved =
                new UiSettingsStore(
                        context)
                        .load();

        assertEquals(
                UiSettingsStore.StartBehavior.START_ONLY,
                saved.startBehavior);

        assertEquals(
                UiSettingsStore.ControlSide.LEFT,
                saved.controlSide);

        context.getSharedPreferences(
                        "ui_settings",
                        Context.MODE_PRIVATE)
                .edit()
                .putString(
                        "control_side",
                        "INVALID")
                .commit();

        assertEquals(
                UiSettingsStore.ControlSide.RIGHT,
                new UiSettingsStore(
                        context)
                        .load()
                        .controlSide);
    }

    @Test
    public void themeModePersistsAndCorruptValueFallsBackSystem() {

        UiSettingsStore store =
                new UiSettingsStore(
                        context);

        store.save(
                new UiSettingsStore.Settings(
                        UiSettingsStore.StartBehavior.START_ONLY,
                        UiSettingsStore.ControlSide.LEFT,
                        UiSettingsStore.ThemeMode.DARK));

        UiSettingsStore.Settings saved =
                new UiSettingsStore(
                        context)
                        .load();

        assertEquals(
                UiSettingsStore.ThemeMode.DARK,
                saved.themeMode);

        assertEquals(
                UiSettingsStore.ControlSide.LEFT,
                saved.controlSide);

        assertEquals(
                UiSettingsStore.StartBehavior.START_ONLY,
                saved.startBehavior);

        context.getSharedPreferences(
                        "ui_settings",
                        Context.MODE_PRIVATE)
                .edit()
                .putString(
                        "theme_mode",
                        "INVALID")
                .commit();

        assertEquals(
                UiSettingsStore.ThemeMode.SYSTEM,
                new UiSettingsStore(
                        context)
                        .load()
                        .themeMode);
    }

    @Test
    public void corruptValueFallsBackSafely() {

        context.getSharedPreferences(
                        "ui_settings",
                        Context.MODE_PRIVATE)
                .edit()
                .putString(
                        "start_behavior",
                        "INVALID")
                .commit();

        assertEquals(
                UiSettingsStore.StartBehavior.NEVER,
                new UiSettingsStore(
                        context)
                        .load()
                        .startBehavior);
    }
}
