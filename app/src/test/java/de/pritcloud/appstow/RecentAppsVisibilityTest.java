package de.pritcloud.appstow;

import android.content.Context;
import android.widget.CheckBox;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class RecentAppsVisibilityTest {
    private Context context;

    @Before
    public void resetSettings() {
        context = ApplicationProvider.getApplicationContext();
        context.deleteSharedPreferences("ui_settings");
        context.deleteSharedPreferences("sorting_settings");
    }

    @Test
    public void previousInstallationsRemainVisibleByDefault() {
        assertTrue(new UiSettingsStore(context).load().showInRecents);
    }

    @Test
    public void settingPersistsAndOldConstructorsPreserveDefault() {
        UiSettingsStore store = new UiSettingsStore(context);
        store.save(new UiSettingsStore.Settings(
                UiSettingsStore.StartBehavior.NEVER,
                UiSettingsStore.ControlSide.RIGHT,
                UiSettingsStore.ThemeMode.SYSTEM,
                UiSettingsStore.StartView.HOME,
                false));
        assertFalse(new UiSettingsStore(context).load().showInRecents);

        store.save(new UiSettingsStore.Settings(
                UiSettingsStore.StartBehavior.NEVER));
        assertTrue(new UiSettingsStore(context).load().showInRecents);
    }

    @Test
    public void manualBackupRoundtripAndOlderV3Default() throws JSONException {
        UiSettingsStore store = new UiSettingsStore(context);
        store.save(new UiSettingsStore.Settings(
                UiSettingsStore.StartBehavior.NEVER,
                UiSettingsStore.ControlSide.RIGHT,
                UiSettingsStore.ThemeMode.SYSTEM,
                UiSettingsStore.StartView.FAVORITES,
                false));
        JSONObject content = BackupV3Configuration.create(context);
        JSONObject wrapped = new JSONObject().put("settings", content);
        assertFalse(content.getJSONObject("ui").getBoolean("showInRecents"));
        BackupV3Configuration.State restored =
                BackupV3Configuration.parse(wrapped);
        assertFalse(restored.ui.showInRecents);
        assertTrue(BackupV3Configuration.restorePreferences(context, restored));
        assertFalse(new UiSettingsStore(context).load().showInRecents);

        JSONObject older = new JSONObject(wrapped.toString());
        older.getJSONObject("settings").getJSONObject("ui")
                .remove("showInRecents");
        assertTrue(BackupV3Configuration.parse(older).ui.showInRecents);
    }

    @Test
    public void malformedV3ValueIsRejected() throws JSONException {
        JSONObject content = BackupV3Configuration.create(context);
        content.getJSONObject("ui").put("showInRecents", "not-a-boolean");
        JSONObject wrapped = new JSONObject().put("settings", content);
        try {
            BackupV3Configuration.parse(wrapped);
            fail("Expected invalid backup setting to be rejected");
        } catch (JSONException expected) {
            // Invalid booleans may not be silently coerced.
        }
    }

    @Test
    public void visibilitySettingIsIndependentFromOffStartMode() {
        UiSettingsStore store = new UiSettingsStore(context);
        store.save(new UiSettingsStore.Settings(
                UiSettingsStore.StartBehavior.NEVER,
                UiSettingsStore.ControlSide.RIGHT,
                UiSettingsStore.ThemeMode.SYSTEM,
                UiSettingsStore.StartView.FAVORITES,
                true));
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                activity.findViewById(R.id.navUiSettings).performClick();
                CheckBox visible = activity.findViewById(R.id.uiShowInRecents);
                assertTrue(visible.isChecked());
                visible.performClick();
                assertFalse(store.load().showInRecents);
                assertEquals(UiSettingsStore.StartBehavior.NEVER,
                        store.load().startBehavior);
                visible.performClick();
                assertTrue(store.load().showInRecents);
                assertEquals(UiSettingsStore.StartBehavior.NEVER,
                        store.load().startBehavior);
            });
        }
    }

    @Test
    public void uiTogglePersistsWithoutChangingOtherSettings() {
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                activity.findViewById(R.id.navUiSettings).performClick();
                CheckBox box = activity.findViewById(R.id.uiShowInRecents);
                assertTrue(box.isChecked());
                box.performClick();
                UiSettingsStore.Settings settings =
                        new UiSettingsStore(activity).load();
                assertFalse(settings.showInRecents);
                assertEquals(UiSettingsStore.StartBehavior.NEVER,
                        settings.startBehavior);
                assertEquals(UiSettingsStore.ThemeMode.SYSTEM,
                        settings.themeMode);
                box.performClick();
                assertTrue(new UiSettingsStore(activity).load().showInRecents);
            });
        }
    }
}
