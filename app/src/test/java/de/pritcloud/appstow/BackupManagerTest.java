package de.pritcloud.appstow;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class BackupManagerTest {

    private Context context;

    @Before
    public void setUp() {
        context =
                RuntimeEnvironment
                        .getApplication();

        clearPreferences(
                "categories",
                "favorites",
                "shortcuts",
                "overview_order",
                "section_item_order",
                "overview_display");
    }

    @Test
    public void validBackupRestoresAllStores()
            throws Exception {

        BackupManager.restoreBackup(
                context,
                validBackup());

        CategoryStore categoryStore =
                new CategoryStore(context);

        List<CategoryEntry> categories =
                categoryStore.getCategories();

        assertEquals(
                1,
                categories.size());

        assertEquals(
                "cat-work",
                categories.get(0).id);

        assertEquals(
                "Work",
                categories.get(0).name);

        assertEquals(
                "💼",
                categories.get(0).symbol);

        assertTrue(
                categoryStore.areSymbolsEnabled());

        assertTrue(
                categoryStore
                        .getAssignedCategoryIds(
                                "com.example.app")
                        .contains(
                                "cat-work"));

        FavoritesStore favoritesStore =
                new FavoritesStore(context);

        assertTrue(
                favoritesStore.isFavorite(
                        "com.example.app"));

        ShortcutStore shortcutStore =
                new ShortcutStore(context);

        assertEquals(
                4,
                shortcutStore
                        .getShortcuts()
                        .size());

        OverviewOrderStore orderStore =
                new OverviewOrderStore(
                        context);

        assertEquals(
                Arrays.asList(
                        "favorites",
                        "category:cat-work",
                        "shortcuts"),
                orderStore.getOrder());

        SectionItemOrderStore itemOrderStore =
                new SectionItemOrderStore(
                        context);

        assertEquals(
                Arrays.asList(
                        "app:com.example.app",
                        "shortcut:site",
                        "app:new"),
                itemOrderStore.getOrderedIds(
                        "favorites",
                        Arrays.asList(
                                "shortcut:site",
                                "app:com.example.app",
                                "app:new")));
    }

    @Test
    public void categorySymbolsDisabledRestoresAsDisabled()
            throws Exception {

        JSONObject backup =
                validBackup();

        backup.put(
                "categorySymbolsEnabled",
                false);

        BackupManager.restoreBackup(
                context,
                backup);

        CategoryStore categoryStore =
                new CategoryStore(
                        context);

        assertTrue(
                !categoryStore.areSymbolsEnabled());
    }

    @Test
    public void writtenBackupCanBeReadImmediatelyAndKeepsSymbolSetting()
            throws Exception {

        CategoryStore categoryStore =
                new CategoryStore(
                        context);

        assertTrue(
                categoryStore.addCategory(
                        "Work",
                        "💼"));

        categoryStore.setSymbolsEnabled(
                true);

        File backupFile =
                File.createTempFile(
                        "appstow-backup-",
                        ".json",
                        context.getCacheDir());

        try {
            Uri uri =
                    Uri.fromFile(
                            backupFile);

            BackupManager.writeBackup(
                    context,
                    uri);

            JSONObject backup =
                    BackupManager.readBackup(
                            context,
                            uri);

            assertTrue(
                    backup.getBoolean(
                            "categorySymbolsEnabled"));

        } finally {
            backupFile.delete();
        }
    }

    @Test
    public void legacyBackupWithoutSymbolsStillRestores()
            throws Exception {

        JSONObject backup =
                validBackup();

        backup.remove(
                "categorySymbolsEnabled");

        backup.getJSONArray(
                        "categories")
                .getJSONObject(0)
                .remove(
                        "symbol");

        BackupManager.restoreBackup(
                context,
                backup);

        CategoryStore categoryStore =
                new CategoryStore(
                        context);

        CategoryEntry category =
                categoryStore.getCategories()
                        .get(0);

        assertEquals(
                CategoryStore.DEFAULT_SYMBOL,
                category.symbol);

        assertTrue(
                !categoryStore.areSymbolsEnabled());
    }

    @Test
    public void rejectsBlankCategorySymbol()
            throws Exception {

        JSONObject backup =
                validBackup();

        backup.getJSONArray(
                        "categories")
                .getJSONObject(0)
                .put(
                        "symbol",
                        "   ");

        assertInvalid(
                backup);
    }

    @Test
    public void rejectsHttpWebApp()
            throws Exception {

        JSONObject backup =
                validBackup();

        backup.getJSONArray(
                        "shortcuts")
                .getJSONObject(1)
                .put(
                        "target",
                        "http://example.com/app");

        assertInvalid(
                backup);
    }

    @Test
    public void rejectsUnknownShortcutType()
            throws Exception {

        JSONObject backup =
                validBackup();

        backup.getJSONArray(
                        "shortcuts")
                .getJSONObject(0)
                .put(
                        "type",
                        "unknown");

        assertInvalid(
                backup);
    }

    @Test
    public void rejectsDuplicateFavorite()
            throws Exception {

        JSONObject backup =
                validBackup();

        backup.getJSONArray(
                        "favoritePackages")
                .put(
                        "com.example.app");

        assertInvalid(
                backup);
    }

    @Test
    public void rejectsUnknownCategoryReference()
            throws Exception {

        JSONObject backup =
                validBackup();

        backup.getJSONObject(
                        "categoryAssignments")
                .put(
                        "com.other.app",
                        new JSONArray()
                                .put(
                                        "missing-category"));

        assertInvalid(
                backup);
    }

    @Test
    public void rejectsDuplicateOverviewOrder()
            throws Exception {

        JSONObject backup =
                validBackup();

        backup.getJSONArray(
                        "overviewOrder")
                .put(
                        "favorites");

        assertInvalid(
                backup);
    }

    @Test
    public void rejectsDuplicateSectionItemOrder()
            throws Exception {

        JSONObject backup =
                validBackup();

        backup.getJSONObject(
                        "sectionItemOrder")
                .getJSONArray(
                        "favorites")
                .put(
                        "app:com.example.app");

        assertInvalid(
                backup);
    }

    @Test
    public void rejectsWhitespaceInStoredValues()
            throws Exception {

        JSONObject backup =
                validBackup();

        backup.getJSONArray(
                        "shortcuts")
                .getJSONObject(0)
                .put(
                        "target",
                        " http://example.com ");

        assertInvalid(
                backup);
    }

    @Test
    public void rejectsDuplicateCategoryNameIgnoringCase()
            throws Exception {

        JSONObject backup =
                validBackup();

        backup.getJSONArray(
                        "categories")
                .put(
                        new JSONObject()
                                .put(
                                        "id",
                                        "cat-work-2")
                                .put(
                                        "name",
                                        "work"));

        assertInvalid(
                backup);
    }

    @Test
    public void paypalWebAppBackupRestores()
            throws Exception {

        JSONObject backup =
                validBackup();

        backup.getJSONArray(
                        "shortcuts")
                .put(
                        shortcut(
                                "paypal",
                                "PayPal",
                                ShortcutEntry.TYPE_WEB_APP,
                                "https://www.paypal.com/de/home/",
                                new JSONArray()
                                        .put(
                                                "cat-work")));

        backup.getJSONObject(
                        "sectionItemOrder")
                .put(
                        "shortcuts",
                        new JSONArray()
                                .put(
                                        "shortcut:site")
                                .put(
                                        "shortcut:webapp")
                                .put(
                                        "shortcut:paypal"))
                .put(
                        "category:cat-work",
                        new JSONArray()
                                .put(
                                        "app:com.example.app")
                                .put(
                                        "shortcut:site")
                                .put(
                                        "shortcut:paypal"));

        BackupManager.restoreBackup(
                context,
                backup);

        ShortcutStore shortcutStore =
                new ShortcutStore(
                        context);

        List<ShortcutEntry> shortcuts =
                shortcutStore.getShortcuts();

        assertEquals(
                5,
                shortcuts.size());

        ShortcutEntry paypal =
                null;

        for (ShortcutEntry shortcut :
                shortcuts) {

            if ("paypal".equals(
                    shortcut.id)) {

                paypal =
                        shortcut;
                break;
            }
        }

        assertTrue(
                paypal != null);

        assertEquals(
                ShortcutEntry.TYPE_WEB_APP,
                paypal.type);

        assertEquals(
                "https://www.paypal.com/de/home/",
                paypal.target);

        assertTrue(
                paypal.categoryIds.contains(
                        "cat-work"));

        SectionItemOrderStore orderStore =
                new SectionItemOrderStore(
                        context);

        assertEquals(
                Arrays.asList(
                        "shortcut:site",
                        "shortcut:webapp",
                        "shortcut:paypal"),
                orderStore.getOrderedIds(
                        "shortcuts",
                        Arrays.asList(
                                "shortcut:site",
                                "shortcut:webapp",
                                "shortcut:paypal")));
    }

    @Test
    public void displaySettingsSurviveBackupAndRestore()
            throws Exception {

        CategoryStore categoryStore =
                new CategoryStore(context);

        assertTrue(
                categoryStore.addCategory(
                        "Work",
                        "💼"));

        String categoryId =
                categoryStore.getCategories()
                        .get(0).id;

        SharedPreferences display =
                context.getSharedPreferences(
                        "overview_display",
                        Context.MODE_PRIVATE);

        assertTrue(
                display.edit()
                        .putBoolean(
                                "grid_mode",
                                true)
                        .putInt(
                                "grid_columns",
                                3)
                        .putInt(
                                "section_grid_columns_favorites",
                                5)
                        .putInt(
                                "section_grid_columns_shortcuts",
                                4)
                        .putInt(
                                "section_grid_columns_category:"
                                        + categoryId,
                                4)
                        .putInt(
                                "section_grid_columns_category:deleted",
                                5)
                        .commit());

        File file =
                File.createTempFile(
                        "appstow-display-",
                        ".json",
                        context.getCacheDir());

        try {
            Uri uri =
                    Uri.fromFile(file);

            BackupManager.writeBackup(
                    context,
                    uri);

            JSONObject backup =
                    BackupManager.readBackup(
                            context,
                            uri);

            JSONObject savedDisplay =
                    backup.getJSONObject(
                            "overviewDisplay");

            assertTrue(
                    savedDisplay.getBoolean(
                            "gridMode"));

            assertEquals(
                    3,
                    savedDisplay.getInt(
                            "gridColumns"));

            JSONObject savedSections =
                    savedDisplay.getJSONObject(
                            "sectionColumns");

            assertEquals(
                    5,
                    savedSections.getInt(
                            "favorites"));

            assertEquals(
                    4,
                    savedSections.getInt(
                            "shortcuts"));

            assertEquals(
                    4,
                    savedSections.getInt(
                            "category:" + categoryId));

            assertTrue(
                    !savedSections.has(
                            "category:deleted"));

            assertTrue(
                    display.edit()
                            .clear()
                            .commit());

            BackupManager.restoreBackup(
                    context,
                    backup);

            assertTrue(
                    display.getBoolean(
                            "grid_mode",
                            false));

            assertEquals(
                    3,
                    display.getInt(
                            "grid_columns",
                            -1));

            assertEquals(
                    5,
                    display.getInt(
                            "section_grid_columns_favorites",
                            -1));

            assertEquals(
                    4,
                    display.getInt(
                            "section_grid_columns_shortcuts",
                            -1));

            assertEquals(
                    4,
                    display.getInt(
                            "section_grid_columns_category:"
                                    + categoryId,
                            -1));

            assertTrue(
                    !display.contains(
                            "section_grid_columns_category:deleted"));

        } finally {
            file.delete();
        }
    }

    @Test
    public void legacyBackupResetsDisplayToDefaults()
            throws Exception {

        SharedPreferences display =
                context.getSharedPreferences(
                        "overview_display",
                        Context.MODE_PRIVATE);

        assertTrue(
                display.edit()
                        .putBoolean(
                                "grid_mode",
                                true)
                        .putInt(
                                "grid_columns",
                                5)
                        .putInt(
                                "section_grid_columns_favorites",
                                3)
                        .putInt(
                                "section_grid_columns_category:cat-work",
                                4)
                        .commit());

        BackupManager.restoreBackup(
                context,
                validBackup());

        assertTrue(
                !display.getBoolean(
                        "grid_mode",
                        true));

        assertEquals(
                4,
                display.getInt(
                        "grid_columns",
                        -1));

        assertTrue(
                !display.contains(
                        "section_grid_columns_favorites"));

        assertTrue(
                !display.contains(
                        "section_grid_columns_category:cat-work"));

        assertEquals(
                1,
                new CategoryStore(context)
                        .getCategories()
                        .size());
    }

    @Test
    public void modernBackupReplacesPreviousDisplaySettings()
            throws Exception {

        SharedPreferences display =
                context.getSharedPreferences(
                        "overview_display",
                        Context.MODE_PRIVATE);

        assertTrue(
                display.edit()
                        .putInt(
                                "grid_columns",
                                5)
                        .putInt(
                                "section_grid_columns_category:old",
                                3)
                        .commit());

        JSONObject sectionColumns =
                new JSONObject()
                        .put(
                                "favorites",
                                5)
                        .put(
                                "category:cat-work",
                                3);

        JSONObject settings =
                new JSONObject()
                        .put(
                                "gridMode",
                                true)
                        .put(
                                "gridColumns",
                                4)
                        .put(
                                "sectionColumns",
                                sectionColumns);

        JSONObject backup =
                validBackup()
                        .put(
                                "overviewDisplay",
                                settings);

        BackupManager.restoreBackup(
                context,
                backup);

        assertTrue(
                display.getBoolean(
                        "grid_mode",
                        false));

        assertEquals(
                4,
                display.getInt(
                        "grid_columns",
                        -1));

        assertEquals(
                5,
                display.getInt(
                        "section_grid_columns_favorites",
                        -1));

        assertEquals(
                3,
                display.getInt(
                        "section_grid_columns_category:cat-work",
                        -1));

        assertTrue(
                !display.contains(
                        "section_grid_columns_category:old"));

        assertTrue(
                !display.contains(
                        "section_grid_columns_shortcuts"));
    }

    @Test
    public void invalidDisplaySettingsCannotChangeStoredData()
            throws Exception {

        SharedPreferences display =
                context.getSharedPreferences(
                        "overview_display",
                        Context.MODE_PRIVATE);

        assertTrue(
                display.edit()
                        .putBoolean(
                                "grid_mode",
                                true)
                        .putInt(
                                "grid_columns",
                                5)
                        .commit());

        JSONObject invalidColumns =
                new JSONObject()
                        .put(
                                "gridMode",
                                true)
                        .put(
                                "gridColumns",
                                2)
                        .put(
                                "sectionColumns",
                                new JSONObject());

        JSONObject unknownCategory =
                new JSONObject()
                        .put(
                                "gridMode",
                                true)
                        .put(
                                "gridColumns",
                                4)
                        .put(
                                "sectionColumns",
                                new JSONObject()
                                        .put(
                                                "category:unknown",
                                                3));

        JSONObject invalidSectionColumns =
                new JSONObject()
                        .put(
                                "gridMode",
                                true)
                        .put(
                                "gridColumns",
                                4)
                        .put(
                                "sectionColumns",
                                new JSONObject()
                                        .put(
                                                "favorites",
                                                6));

        JSONObject invalidMode =
                new JSONObject()
                        .put(
                                "gridMode",
                                "true")
                        .put(
                                "gridColumns",
                                4)
                        .put(
                                "sectionColumns",
                                new JSONObject());

        JSONObject[] invalidSettings = {
                invalidColumns,
                unknownCategory,
                invalidSectionColumns,
                invalidMode
        };

        for (JSONObject settings :
                invalidSettings) {

            JSONObject backup =
                    validBackup()
                            .put(
                                    "overviewDisplay",
                                    settings);

            assertInvalid(
                    backup);

            assertTrue(
                    display.getBoolean(
                            "grid_mode",
                            false));

            assertEquals(
                    5,
                    display.getInt(
                            "grid_columns",
                            -1));

            assertEquals(
                    "[]",
                    context.getSharedPreferences(
                                    "categories",
                                    Context.MODE_PRIVATE)
                            .getString(
                                    "category_list",
                                    "[]"));
        }
    }

    @Test
    public void invalidLocalShortcutCannotProduceBrokenBackup()
            throws Exception {

        JSONObject invalidShortcut =
                new JSONObject()
                        .put(
                                "id",
                                "invalid-webapp")
                        .put(
                                "name",
                                "Invalid web app")
                        .put(
                                "type",
                                ShortcutEntry.TYPE_WEB_APP)
                        .put(
                                "target",
                                "http://example.com/app")
                        .put(
                                "favorite",
                                false)
                        .put(
                                "categories",
                                new JSONArray());

        assertTrue(
                context.getSharedPreferences(
                                "shortcuts",
                                Context.MODE_PRIVATE)
                        .edit()
                        .putString(
                                "shortcut_list",
                                new JSONArray()
                                        .put(invalidShortcut)
                                        .toString())
                        .commit());

        File file =
                File.createTempFile(
                        "appstow-invalid-",
                        ".json",
                        context.getCacheDir());

        try {
            BackupManager.writeBackup(
                    context,
                    Uri.fromFile(file));

            fail(
                    "Ungültiger Shortcut wurde gesichert.");

        } catch (JSONException expected) {
            // Die Sicherung muss abgelehnt werden.

        } finally {
            file.delete();
        }
    }

    private void assertInvalid(
            JSONObject backup)
            throws Exception {

        try {
            BackupManager.restoreBackup(
                    context,
                    backup);

            fail(
                    "Ungültiges Backup wurde akzeptiert.");

        } catch (JSONException expected) {
            // Erwartet.
        }
    }

    private JSONObject validBackup()
            throws JSONException {

        JSONArray categories =
                new JSONArray()
                        .put(
                                new JSONObject()
                                        .put(
                                                "id",
                                                "cat-work")
                                        .put(
                                                "name",
                                                "Work")
                                        .put(
                                                "symbol",
                                                "💼"));

        JSONObject assignments =
                new JSONObject()
                        .put(
                                "com.example.app",
                                new JSONArray()
                                        .put(
                                                "cat-work"));

        JSONArray favorites =
                new JSONArray()
                        .put(
                                "com.example.app");

        JSONArray shortcuts =
                new JSONArray()
                        .put(
                                shortcut(
                                        "site",
                                        "Website",
                                        ShortcutEntry.TYPE_WEBSITE,
                                        "http://example.com",
                                        new JSONArray()
                                                .put(
                                                        "cat-work")))
                        .put(
                                shortcut(
                                        "webapp",
                                        "Web App",
                                        ShortcutEntry.TYPE_WEB_APP,
                                        "https://example.com/app",
                                        new JSONArray()))
                        .put(
                                shortcut(
                                        "deep",
                                        "Deep Link",
                                        ShortcutEntry.TYPE_DEEP_LINK,
                                        "mailto:test@example.com",
                                        new JSONArray()))
                        .put(
                                shortcut(
                                        "settings",
                                        "Settings",
                                        ShortcutEntry.TYPE_APP_SETTINGS,
                                        "com.example.app",
                                        new JSONArray()));

        JSONObject sectionItemOrder =
                new JSONObject()
                        .put(
                                "favorites",
                                new JSONArray()
                                        .put(
                                                "app:com.example.app")
                                        .put(
                                                "shortcut:site"))
                        .put(
                                "category:cat-work",
                                new JSONArray()
                                        .put(
                                                "app:com.example.app"));

        return new JSONObject()
                .put(
                        "format",
                        "appstow-backup")
                .put(
                        "formatVersion",
                        2)
                .put(
                        "createdAt",
                        1L)
                .put(
                        "categories",
                        categories)
                .put(
                        "categoryAssignments",
                        assignments)
                .put(
                        "categorySymbolsEnabled",
                        true)
                .put(
                        "favoritePackages",
                        favorites)
                .put(
                        "shortcuts",
                        shortcuts)
                .put(
                        "overviewOrder",
                        new JSONArray()
                                .put(
                                        "favorites")
                                .put(
                                        "category:cat-work")
                                .put(
                                        "shortcuts"))
                .put(
                        "sectionItemOrder",
                        sectionItemOrder);
    }

    private JSONObject shortcut(
            String id,
            String name,
            String type,
            String target,
            JSONArray categories)
            throws JSONException {

        return new JSONObject()
                .put(
                        "id",
                        id)
                .put(
                        "name",
                        name)
                .put(
                        "type",
                        type)
                .put(
                        "target",
                        target)
                .put(
                        "favorite",
                        false)
                .put(
                        "categories",
                        categories);
    }

    private void clearPreferences(
            String... names) {

        for (String name : names) {
            context.getSharedPreferences(
                            name,
                            Context.MODE_PRIVATE)
                    .edit()
                    .clear()
                    .commit();
        }
    }
}
