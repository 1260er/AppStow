package de.pritcloud.appstow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class BackupAutomaticFavoritesTest {

    private Context context;

    @Before
    public void setUp() throws Exception {

        context = RuntimeEnvironment.getApplication();

        for (String name : Arrays.asList(
                "categories", "favorites", "shortcuts",
                "overview_order", "section_item_order",
                "overview_display", "sorting_settings",
                "ui_settings", "statistics_display",
                "usage_statistics")) {

            context.getSharedPreferences(
                            name,
                            Context.MODE_PRIVATE)
                    .edit().clear().commit();
        }

        new SortingSettingsStore(context).save(
                new SortingSettingsStore.Settings(
                        SortingSettingsStore.Mode.AUTOMATIC,
                        true, true, true, true,
                        false, 8, 21, 5, false));

        context.getSharedPreferences(
                        "favorites",
                        Context.MODE_PRIVATE)
                .edit()
                .putStringSet(
                        "packages",
                        new HashSet<>(
                                Set.of("com.manual.old")))
                .commit();

        JSONArray shortcuts = new JSONArray();
        shortcuts.put(shortcut("site", "Website", false));
        shortcuts.put(shortcut("old", "Old website", true));

        context.getSharedPreferences(
                        "shortcuts",
                        Context.MODE_PRIVATE)
                .edit()
                .putString("shortcut_list", shortcuts.toString())
                .commit();

        new SectionItemOrderStore(context).saveOrder(
                "favorites",
                Arrays.asList(
                        "app:com.manual.old",
                        "shortcut:old"));
    }

    private static JSONObject shortcut(
            String id,
            String name,
            boolean favorite) throws Exception {

        return new JSONObject()
                .put("id", id)
                .put("name", name)
                .put("type", "website")
                .put("target", "https://example.org")
                .put("favorite", favorite)
                .put("categories", new JSONArray());
    }

    private JSONObject createAndReadBackup(
            List<String> visible) throws Exception {

        File file = File.createTempFile(
                "appstow-auto-favorites-",
                ".json",
                context.getCacheDir());

        try {
            Uri uri = Uri.fromFile(file);
            BackupManager.writeBackup(context, uri, null, visible);
            return BackupManager.readBackup(context, uri);
        } finally {
            file.delete();
        }
    }

    @Test
    public void automaticBackupCapturesExactlyVisibleMixedFavorites()
            throws Exception {

        List<String> visible = Arrays.asList(
                "shortcut:site",
                "app:com.saved.app");

        JSONObject backup = createAndReadBackup(visible);

        assertEquals(
                "AUTOMATIC",
                backup.getJSONObject("settings")
                        .getJSONObject("sorting")
                        .getString("mode"));

        JSONArray apps = backup.getJSONArray("favoritePackages");
        assertEquals(1, apps.length());
        assertEquals("com.saved.app", apps.getString(0));

        JSONArray shortcuts = backup.getJSONArray("shortcuts");
        assertTrue(shortcuts.getJSONObject(0)
                .getBoolean("favorite"));
        assertFalse(shortcuts.getJSONObject(1)
                .getBoolean("favorite"));

        JSONArray order = backup.getJSONObject("sectionItemOrder")
                .getJSONArray("favorites");
        assertEquals(2, order.length());
        assertEquals(visible.get(0), order.getString(0));
        assertEquals(visible.get(1), order.getString(1));

        // Sichern darf den manuell gespeicherten Zustand NICHT veraendern.
        assertTrue(new FavoritesStore(context)
                .isFavorite("com.manual.old"));
        assertFalse(new FavoritesStore(context)
                .isFavorite("com.saved.app"));

        List<ShortcutEntry> liveShortcuts =
                new ShortcutStore(context).getShortcuts();
        assertFalse(liveShortcuts.get(0).favorite);
        assertTrue(liveShortcuts.get(1).favorite);

        assertEquals(
                Arrays.asList("app:com.manual.old", "shortcut:old"),
                new SectionItemOrderStore(context).getOrderedIds(
                        "favorites",
                        Arrays.asList("app:com.manual.old", "shortcut:old")));

        SharedPreferences statistics = context.getSharedPreferences(
                "usage_statistics", Context.MODE_PRIVATE);
        assertTrue(statistics.edit()
                .putString("day_1", "{}")
                .commit());

        BackupManager.restoreBackup(
                context,
                backup,
                Set.of("com.saved.app"));

        assertTrue(statistics.getAll().isEmpty());

        FavoritesStore restoredFavorites = new FavoritesStore(context);
        assertTrue(restoredFavorites.isFavorite("com.saved.app"));
        assertFalse(restoredFavorites.isFavorite("com.manual.old"));

        List<ShortcutEntry> restoredShortcuts =
                new ShortcutStore(context).getShortcuts();
        assertTrue(restoredShortcuts.get(0).favorite);
        assertFalse(restoredShortcuts.get(1).favorite);

        assertEquals(
                visible,
                new SectionItemOrderStore(context)
                        .getOrderedIds("favorites", visible));

        SortingSettingsStore sorting = new SortingSettingsStore(context);
        assertEquals(SortingSettingsStore.Mode.MANUAL,
                sorting.load().mode);
        assertFalse(sorting.hasRestoredAutomaticFavorites());

        sorting.save(sorting.load());
        assertFalse(sorting.hasRestoredAutomaticFavorites());

        assertEquals(
                visible,
                new SectionItemOrderStore(context)
                        .getOrderedIds("favorites", visible));
    }

    @Test
    public void fullAutomaticBackupPreservesVisibleOrderInManualMode()
            throws Exception {

        org.junit.Assert.assertTrue(
                new CategoryStore(context).addCategory("Work", "W"));
        String category = "category:" +
                new CategoryStore(context).getCategories().get(0).id;

        List<String> savedSections = Arrays.asList(
                "favorites", category, "shortcuts");
        List<String> visibleSections = Arrays.asList(
                "shortcuts", "favorites", category);
        List<String> visibleFavorites = Arrays.asList(
                "shortcut:site", "app:com.saved.app");
        List<String> visibleShortcuts = Arrays.asList(
                "shortcut:site", "shortcut:old");

        new OverviewOrderStore(context).saveOrderIds(savedSections);
        new SectionItemOrderStore(context).saveOrder(
                "shortcuts", Arrays.asList(
                        "shortcut:old", "shortcut:site"));

        java.util.Map<String, List<String>> visibleItems =
                new java.util.HashMap<>();
        visibleItems.put("favorites", visibleFavorites);
        visibleItems.put("shortcuts", visibleShortcuts);
        visibleItems.put(category, Collections.emptyList());

        File file = File.createTempFile(
                "appstow-visible-orders-", ".json", context.getCacheDir());
        JSONObject backup;
        try {
            Uri uri = Uri.fromFile(file);
            BackupManager.writeBackup(context, uri, null,
                    visibleFavorites, visibleSections, visibleItems);
            backup = BackupManager.readBackup(context, uri);
        } finally {
            file.delete();
        }

        // Eine Sicherung selbst veraendert NICHT die aktiven Stores.
        assertEquals(savedSections,
                new OverviewOrderStore(context).getOrder());
        assertEquals(Arrays.asList("shortcut:old", "shortcut:site"),
                new SectionItemOrderStore(context).getOrderedIds(
                        "shortcuts", visibleShortcuts));
        assertTrue(new FavoritesStore(context).isFavorite("com.manual.old"));

        JSONArray sections = backup.getJSONArray("overviewOrder");
        for (int i = 0; i < visibleSections.size(); i++) {
            assertEquals(visibleSections.get(i), sections.getString(i));
        }
        JSONArray shortcuts = backup.getJSONObject("sectionItemOrder")
                .getJSONArray("shortcuts");
        assertEquals("shortcut:site", shortcuts.getString(0));
        assertEquals("shortcut:old", shortcuts.getString(1));

        BackupManager.restoreBackup(context, backup, Set.of("com.saved.app"));

        assertEquals(SortingSettingsStore.Mode.MANUAL,
                new SortingSettingsStore(context).load().mode);
        assertEquals(visibleSections,
                new OverviewOrderStore(context).getOrder());
        assertEquals(visibleFavorites,
                new SectionItemOrderStore(context).getOrderedIds(
                        "favorites", visibleFavorites));
        assertEquals(visibleShortcuts,
                new SectionItemOrderStore(context).getOrderedIds(
                        "shortcuts", visibleShortcuts));
        assertTrue(new FavoritesStore(context).isFavorite("com.saved.app"));
    }

    @Test
    public void emptyAutomaticSnapshotDoesNotRestoreOldFavorites()
            throws Exception {

        JSONObject backup = createAndReadBackup(Collections.emptyList());
        assertEquals(0,
                backup.getJSONArray("favoritePackages").length());

        JSONArray shortcuts = backup.getJSONArray("shortcuts");
        assertFalse(shortcuts.getJSONObject(0).getBoolean("favorite"));
        assertFalse(shortcuts.getJSONObject(1).getBoolean("favorite"));
        assertEquals(0, backup.getJSONObject("sectionItemOrder")
                .getJSONArray("favorites").length());
    }

    @Test
    public void manualModeDoesNotUseAutomaticSnapshot()
            throws Exception {

        new SortingSettingsStore(context).save(
                SortingSettingsStore.Settings.defaults());

        JSONObject backup = createAndReadBackup(
                Arrays.asList("shortcut:site", "app:com.saved.app"));

        assertEquals(1, backup.getJSONArray("favoritePackages").length());
        assertEquals("com.manual.old",
                backup.getJSONArray("favoritePackages").getString(0));

        assertFalse(backup.getJSONArray("shortcuts")
                .getJSONObject(0).getBoolean("favorite"));
        assertTrue(backup.getJSONArray("shortcuts")
                .getJSONObject(1).getBoolean("favorite"));
    }
}
