package de.pritcloud.appstow;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.Gravity;
import android.widget.RadioGroup;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class StartViewSelectionTest {

    private Context context;

    @Before
    public void clearState() {
        context = ApplicationProvider.getApplicationContext();
        for (String name : new String[]{
                "ui_settings", "sorting_settings", "categories", "shortcuts",
                "favorites", "overview_display", "usage_statistics",
                "statistics_display"}) {
            context.deleteSharedPreferences(name);
        }
    }

    @Test
    public void oldEnabledModeKeepsFavoritesByDefault() {
        SharedPreferences prefs = context.getSharedPreferences(
                "ui_settings", Context.MODE_PRIVATE);
        assertTrue(prefs.edit().putString("start_behavior", "ALWAYS_FOREGROUND").commit());
        UiSettingsStore.Settings settings = new UiSettingsStore(context).load();
        assertEquals(UiSettingsStore.StartBehavior.ALWAYS_FOREGROUND, settings.startBehavior);
        assertEquals(UiSettingsStore.StartView.FAVORITES, settings.startView);
    }

    @Test
    public void startViewPersistsAndInvalidValueFallsBackToFavorites() {
        UiSettingsStore store = new UiSettingsStore(context);
        store.save(new UiSettingsStore.Settings(
                UiSettingsStore.StartBehavior.START_ONLY,
                UiSettingsStore.ControlSide.LEFT,
                UiSettingsStore.ThemeMode.DARK,
                UiSettingsStore.StartView.LAST_CATEGORY));
        assertEquals(UiSettingsStore.StartView.LAST_CATEGORY,
                new UiSettingsStore(context).load().startView);
        assertTrue(context.getSharedPreferences("ui_settings", Context.MODE_PRIVATE)
                .edit().putString("start_view", "INVALID").commit());
        assertEquals(UiSettingsStore.StartView.FAVORITES,
                new UiSettingsStore(context).load().startView);
    }

    @Test
    public void onlyRealCategoryIdsAreRemembered() {
        UiSettingsStore store = new UiSettingsStore(context);
        store.rememberLastCategory("favorites");
        assertNull(store.getLastCategoryId());
        store.rememberLastCategory("category:123");
        store.rememberLastCategory("shortcuts");
        assertEquals("category:123", store.getLastCategoryId());
    }

    @Test
    public void radioRowsStayHorizontalCenteredAndOffDisablesSecondRow() {
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                activity.findViewById(R.id.navUiSettings).performClick();
                RadioGroup when = activity.findViewById(R.id.uiStartBehaviorGroup);
                RadioGroup target = activity.findViewById(R.id.uiStartViewGroup);
                assertEquals(RadioGroup.HORIZONTAL, when.getOrientation());
                assertEquals(RadioGroup.HORIZONTAL, target.getOrientation());
                assertTrue((target.getGravity() & Gravity.HORIZONTAL_GRAVITY_MASK)
                        == Gravity.CENTER_HORIZONTAL);
                assertFalse(target.getChildAt(0).isEnabled());
                activity.findViewById(R.id.uiStartOnce).performClick();
                assertTrue(target.getChildAt(0).isEnabled());
                activity.findViewById(R.id.uiStartViewHome).performClick();
                assertEquals(UiSettingsStore.StartView.HOME,
                        new UiSettingsStore(activity).load().startView);
                activity.findViewById(R.id.uiStartNever).performClick();
                assertFalse(target.getChildAt(0).isEnabled());
                assertEquals(UiSettingsStore.StartView.HOME,
                        new UiSettingsStore(activity).load().startView);
            });
        }
    }

    @Test
    public void backupRoundtripSupportsNewTargetAndLegacyV3() throws Exception {
        UiSettingsStore store = new UiSettingsStore(context);
        store.save(new UiSettingsStore.Settings(
                UiSettingsStore.StartBehavior.ALWAYS_FOREGROUND,
                UiSettingsStore.ControlSide.RIGHT,
                UiSettingsStore.ThemeMode.SYSTEM,
                UiSettingsStore.StartView.HOME));
        JSONObject backup = BackupV3Configuration.create(context);
        // The create() method returns the settings subtree, nested by BackupManager.
        JSONObject settings = new JSONObject().put("settings", backup);
        assertEquals(UiSettingsStore.StartView.HOME,
                BackupV3Configuration.parse(settings).ui.startView);
        JSONObject legacy = new JSONObject(settings.toString());
        legacy.getJSONObject("settings").getJSONObject("ui").remove("startView");
        assertEquals(UiSettingsStore.StartView.FAVORITES,
                BackupV3Configuration.parse(legacy).ui.startView);
    }

    @Test
    public void lastCategoryOpensInGridOnColdStart() {
        CategoryStore categories = new CategoryStore(context);
        assertTrue(categories.addCategory("Tools", "🔧"));
        String id = "category:" + categories.getCategories().get(0).id;
        UiSettingsStore store = new UiSettingsStore(context);
        store.save(new UiSettingsStore.Settings(
                UiSettingsStore.StartBehavior.START_ONLY,
                UiSettingsStore.ControlSide.RIGHT,
                UiSettingsStore.ThemeMode.SYSTEM,
                UiSettingsStore.StartView.LAST_CATEGORY));
        store.rememberLastCategory(id);
        assertTrue(context.getSharedPreferences("overview_display", Context.MODE_PRIVATE)
                .edit().putBoolean("grid_mode", true).commit());
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                try {
                    java.lang.reflect.Field field = MainActivity.class
                            .getDeclaredField("overviewAdapter");
                    field.setAccessible(true);
                    OverviewAdapter adapter = (OverviewAdapter) field.get(activity);
                    assertEquals(id, adapter.getGridOpenSectionId());
                } catch (ReflectiveOperationException exception) {
                    throw new AssertionError(exception);
                }
            });
        }
    }

    @Test
    public void alwaysStartPageReturnsToOverview() {
        UiSettingsStore store = new UiSettingsStore(context);
        store.save(new UiSettingsStore.Settings(
                UiSettingsStore.StartBehavior.ALWAYS_FOREGROUND,
                UiSettingsStore.ControlSide.RIGHT,
                UiSettingsStore.ThemeMode.SYSTEM,
                UiSettingsStore.StartView.HOME));
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity ->
                    activity.findViewById(R.id.navStatistics).performClick());
            scenario.moveToState(Lifecycle.State.CREATED);
            scenario.moveToState(Lifecycle.State.RESUMED);
            scenario.onActivity(activity -> assertEquals("overview", currentPage(activity)));
        }
    }

    @Test
    public void offKeepsCurrentPageOnForegroundReturn() {
        UiSettingsStore store = new UiSettingsStore(context);
        store.save(new UiSettingsStore.Settings(
                UiSettingsStore.StartBehavior.NEVER,
                UiSettingsStore.ControlSide.RIGHT,
                UiSettingsStore.ThemeMode.SYSTEM,
                UiSettingsStore.StartView.HOME));
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity ->
                    activity.findViewById(R.id.navStatistics).performClick());
            scenario.moveToState(Lifecycle.State.CREATED);
            scenario.moveToState(Lifecycle.State.RESUMED);
            scenario.onActivity(activity -> assertEquals("statistics", currentPage(activity)));
        }
    }

    private static String currentPage(MainActivity activity) {
        try {
            java.lang.reflect.Field field = MainActivity.class.getDeclaredField("currentPage");
            field.setAccessible(true);
            return (String) field.get(activity);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
