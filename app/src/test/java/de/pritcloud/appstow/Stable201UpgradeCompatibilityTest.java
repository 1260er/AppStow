package de.pritcloud.appstow;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class Stable201UpgradeCompatibilityTest {

    private Context context;

    private SharedPreferences prefs(String name) {
        return context.getSharedPreferences(
                name, Context.MODE_PRIVATE);
    }

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();

        for (String name : Arrays.asList(
                "categories", "favorites", "shortcuts",
                "overview_order", "section_item_order",
                "overview_display", "sorting_settings",
                "ui_settings")) {

            assertTrue(prefs(name).edit().clear().commit());
        }
    }

    @Test
    public void stable201DataRemainsReadable() throws Exception {

        JSONObject category = new JSONObject()
                .put("id", "cat-v2")
                .put("name", "Work")
                .put("symbol", "💼");

        JSONObject assignments = new JSONObject()
                .put("com.example.old",
                        new JSONArray().put("cat-v2"));

        assertTrue(prefs("categories").edit()
                .putString("category_list",
                        new JSONArray().put(category).toString())
                .putString("category_assignments",
                        assignments.toString())
                .putBoolean("category_symbols_enabled", true)
                .commit());

        assertTrue(prefs("favorites").edit()
                .putStringSet("packages",
                        Set.of("com.example.old"))
                .commit());

        JSONObject shortcut = new JSONObject()
                .put("id", "shortcut-v2")
                .put("name", "Website")
                .put("type", "website")
                .put("target", "https://example.org")
                .put("favorite", true)
                .put("categories",
                        new JSONArray().put("cat-v2"));

        assertTrue(prefs("shortcuts").edit()
                .putString("shortcut_list",
                        new JSONArray().put(shortcut).toString())
                .commit());

        assertTrue(prefs("overview_order").edit()
                .putString("section_order",
                        new JSONArray()
                                .put("shortcuts")
                                .put("favorites").toString())
                .commit());

        JSONObject itemOrders = new JSONObject()
                .put("favorites", new JSONArray()
                        .put("app:com.example.old")
                        .put("shortcut:shortcut-v2"));

        assertTrue(prefs("section_item_order").edit()
                .putString("orders", itemOrders.toString())
                .commit());

        assertTrue(prefs("overview_display").edit()
                .putBoolean("grid_mode", true)
                .putInt("grid_columns", 5)
                .commit());

        CategoryStore categories = new CategoryStore(context);

        assertEquals(1, categories.getCategories().size());
        assertEquals("cat-v2",
                categories.getCategories().get(0).id);
        assertEquals("💼",
                categories.getCategories().get(0).symbol);
        assertTrue(categories.areSymbolsEnabled());
        assertEquals(Set.of("cat-v2"),
                categories.getAssignedCategoryIds("com.example.old"));

        assertTrue(new FavoritesStore(context)
                .isFavorite("com.example.old"));

        List<ShortcutEntry> shortcuts =
                new ShortcutStore(context).getShortcuts();

        assertEquals(1, shortcuts.size());
        assertEquals("shortcut-v2", shortcuts.get(0).id);
        assertEquals("https://example.org",
                shortcuts.get(0).target);
        assertTrue(shortcuts.get(0).favorite);
        assertEquals(Set.of("cat-v2"),
                shortcuts.get(0).categoryIds);

        assertEquals(Arrays.asList("shortcuts", "favorites"),
                new OverviewOrderStore(context).getOrder());

        assertEquals(Arrays.asList(
                "app:com.example.old", "shortcut:shortcut-v2"),
                new SectionItemOrderStore(context).getOrderedIds(
                        "favorites",
                        Arrays.asList(
                                "shortcut:shortcut-v2",
                                "app:com.example.old")));

        assertTrue(prefs("overview_display")
                .getBoolean("grid_mode", false));
        assertEquals(5, prefs("overview_display")
                .getInt("grid_columns", 4));
    }

    @Test
    public void newFeaturesKeepSafeDefaults() {

        SortingSettingsStore.Settings sorting =
                new SortingSettingsStore(context).load();

        assertEquals(SortingSettingsStore.Mode.MANUAL,
                sorting.mode);
        assertFalse(sorting.timeProfileEnabled);

        UiSettingsStore.Settings ui =
                new UiSettingsStore(context).load();

        assertEquals(UiSettingsStore.StartBehavior.NEVER,
                ui.startBehavior);
        assertEquals(UiSettingsStore.ControlSide.RIGHT,
                ui.controlSide);
        assertEquals(UiSettingsStore.ThemeMode.SYSTEM,
                ui.themeMode);
    }
}
