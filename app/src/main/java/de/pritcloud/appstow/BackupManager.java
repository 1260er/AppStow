package de.pritcloud.appstow;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

final class BackupManager {

    private static final String FORMAT_ID =
            "appstow-backup";

    private static final int FORMAT_VERSION = 2;

    private BackupManager() {
    }

    static void writeBackup(
            Context context,
            Uri uri)
            throws IOException, JSONException {

        JSONObject backup =
                createBackup(context);

        OutputStream output =
                context.getContentResolver()
                        .openOutputStream(
                                uri,
                                "w");

        if (output == null) {
            throw new IOException(
                    "Backup-Datei konnte nicht geöffnet werden.");
        }

        try (OutputStream stream = output) {
            stream.write(
                    backup.toString(2)
                            .getBytes(
                                    StandardCharsets.UTF_8));
        }
    }

    static JSONObject readBackup(
            Context context,
            Uri uri)
            throws IOException, JSONException {

        InputStream input =
                context.getContentResolver()
                        .openInputStream(uri);

        if (input == null) {
            throw new IOException(
                    "Backup-Datei konnte nicht geöffnet werden.");
        }

        ByteArrayOutputStream output =
                new ByteArrayOutputStream();

        try (InputStream stream = input) {
            byte[] buffer = new byte[8192];
            int count;

            while ((count = stream.read(buffer)) != -1) {
                output.write(
                        buffer,
                        0,
                        count);
            }
        }

        JSONObject backup =
                new JSONObject(
                        output.toString(
                                StandardCharsets.UTF_8.name()));

        validateBackup(backup);

        return backup;
    }

    static void restoreBackup(
            Context context,
            JSONObject backup)
            throws JSONException, IOException {

        validateBackup(backup);

        JSONArray categories =
                backup.getJSONArray(
                        "categories");

        JSONObject assignments =
                backup.getJSONObject(
                        "categoryAssignments");

        JSONArray favoritePackages =
                backup.getJSONArray(
                        "favoritePackages");

        JSONArray shortcuts =
                backup.getJSONArray(
                        "shortcuts");

        JSONArray overviewOrder =
                backup.getJSONArray(
                        "overviewOrder");

        JSONObject sectionItemOrder =
                backup.getJSONObject(
                        "sectionItemOrder");

        Set<String> favorites =
                new HashSet<>();

        for (int i = 0;
             i < favoritePackages.length();
             i++) {

            favorites.add(
                    favoritePackages.getString(i));
        }

        boolean categoriesSaved =
                context.getSharedPreferences(
                                "categories",
                                Context.MODE_PRIVATE)
                        .edit()
                        .clear()
                        .putString(
                                "category_list",
                                categories.toString())
                        .putString(
                                "category_assignments",
                                assignments.toString())
                        .commit();

        boolean favoritesSaved =
                context.getSharedPreferences(
                                "favorites",
                                Context.MODE_PRIVATE)
                        .edit()
                        .clear()
                        .putStringSet(
                                "packages",
                                favorites)
                        .commit();

        boolean shortcutsSaved =
                context.getSharedPreferences(
                                "shortcuts",
                                Context.MODE_PRIVATE)
                        .edit()
                        .clear()
                        .putString(
                                "shortcut_list",
                                shortcuts.toString())
                        .commit();

        boolean orderSaved =
                context.getSharedPreferences(
                                "overview_order",
                                Context.MODE_PRIVATE)
                        .edit()
                        .clear()
                        .putString(
                                "section_order",
                                overviewOrder.toString())
                        .commit();

        boolean sectionItemOrderSaved =
                context.getSharedPreferences(
                                "section_item_order",
                                Context.MODE_PRIVATE)
                        .edit()
                        .clear()
                        .putString(
                                "orders",
                                sectionItemOrder.toString())
                        .commit();

        if (!categoriesSaved
                || !favoritesSaved
                || !shortcutsSaved
                || !orderSaved
                || !sectionItemOrderSaved) {

            throw new IOException(
                    "Backup konnte nicht vollständig wiederhergestellt werden.");
        }
    }

    private static JSONObject createBackup(
            Context context)
            throws JSONException {

        SharedPreferences categoryPrefs =
                context.getSharedPreferences(
                        "categories",
                        Context.MODE_PRIVATE);

        SharedPreferences favoritePrefs =
                context.getSharedPreferences(
                        "favorites",
                        Context.MODE_PRIVATE);

        SharedPreferences shortcutPrefs =
                context.getSharedPreferences(
                        "shortcuts",
                        Context.MODE_PRIVATE);

        SharedPreferences orderPrefs =
                context.getSharedPreferences(
                        "overview_order",
                        Context.MODE_PRIVATE);

        SharedPreferences sectionItemOrderPrefs =
                context.getSharedPreferences(
                        "section_item_order",
                        Context.MODE_PRIVATE);

        JSONArray categories =
                new JSONArray(
                        categoryPrefs.getString(
                                "category_list",
                                "[]"));

        JSONObject assignments =
                new JSONObject(
                        categoryPrefs.getString(
                                "category_assignments",
                                "{}"));

        List<String> favoriteList =
                new ArrayList<>(
                        favoritePrefs.getStringSet(
                                "packages",
                                Collections.emptySet()));

        Collections.sort(favoriteList);

        JSONArray favoritePackages =
                new JSONArray();

        for (String packageName :
                favoriteList) {

            favoritePackages.put(
                    packageName);
        }

        JSONArray shortcuts =
                new JSONArray(
                        shortcutPrefs.getString(
                                "shortcut_list",
                                "[]"));

        JSONArray overviewOrder =
                new JSONArray(
                        orderPrefs.getString(
                                "section_order",
                                "[]"));

        JSONObject sectionItemOrder =
                new JSONObject(
                        sectionItemOrderPrefs.getString(
                                "orders",
                                "{}"));

        JSONObject backup =
                new JSONObject();

        backup.put(
                "format",
                FORMAT_ID);

        backup.put(
                "formatVersion",
                FORMAT_VERSION);

        backup.put(
                "createdAt",
                System.currentTimeMillis());

        backup.put(
                "categories",
                categories);

        backup.put(
                "categoryAssignments",
                assignments);

        backup.put(
                "favoritePackages",
                favoritePackages);

        backup.put(
                "shortcuts",
                shortcuts);

        backup.put(
                "overviewOrder",
                overviewOrder);

        backup.put(
                "sectionItemOrder",
                sectionItemOrder);

        validateBackup(backup);

        return backup;
    }

    private static void validateBackup(
            JSONObject backup)
            throws JSONException {

        if (!FORMAT_ID.equals(
                backup.optString(
                        "format",
                        ""))) {

            throw new JSONException(
                    "Unbekanntes Backup-Format.");
        }

        if (backup.optInt(
                "formatVersion",
                -1) != FORMAT_VERSION) {

            throw new JSONException(
                    "Diese Backup-Version wird nicht unterstützt.");
        }

        JSONArray categories =
                backup.getJSONArray(
                        "categories");

        JSONObject assignments =
                backup.getJSONObject(
                        "categoryAssignments");

        JSONArray favoritePackages =
                backup.getJSONArray(
                        "favoritePackages");

        JSONArray shortcuts =
                backup.getJSONArray(
                        "shortcuts");

        JSONArray overviewOrder =
                backup.getJSONArray(
                        "overviewOrder");

        JSONObject sectionItemOrder =
                backup.getJSONObject(
                        "sectionItemOrder");

        Set<String> categoryIds =
                new HashSet<>();

        for (int i = 0;
             i < categories.length();
             i++) {

            JSONObject category =
                    categories.getJSONObject(i);

            String id =
                    category.getString("id")
                            .trim();

            String name =
                    category.getString("name")
                            .trim();

            if (id.isEmpty()
                    || name.isEmpty()
                    || !categoryIds.add(id)) {

                throw new JSONException(
                        "Ungültige Kategorien im Backup.");
            }
        }

        Iterator<String> assignmentKeys =
                assignments.keys();

        while (assignmentKeys.hasNext()) {
            String packageName =
                    assignmentKeys.next();

            if (packageName.trim().isEmpty()) {
                throw new JSONException(
                        "Ungültige App-Zuweisung im Backup.");
            }

            JSONArray ids =
                    assignments.getJSONArray(
                            packageName);

            for (int i = 0;
                 i < ids.length();
                 i++) {

                String categoryId =
                        ids.getString(i);

                if (!categoryIds.contains(
                        categoryId)) {

                    throw new JSONException(
                            "App-Zuweisung verweist auf eine unbekannte Kategorie.");
                }
            }
        }

        for (int i = 0;
             i < favoritePackages.length();
             i++) {

            if (favoritePackages
                    .getString(i)
                    .trim()
                    .isEmpty()) {

                throw new JSONException(
                        "Ungültiger Favorit im Backup.");
            }
        }

        Set<String> shortcutIds =
                new HashSet<>();

        for (int i = 0;
             i < shortcuts.length();
             i++) {

            JSONObject shortcut =
                    shortcuts.getJSONObject(i);

            String id =
                    shortcut.getString("id")
                            .trim();

            String name =
                    shortcut.getString("name")
                            .trim();

            String type =
                    shortcut.getString("type")
                            .trim();

            String target =
                    shortcut.getString("target")
                            .trim();

            if (id.isEmpty()
                    || name.isEmpty()
                    || type.isEmpty()
                    || target.isEmpty()
                    || !shortcutIds.add(id)) {

                throw new JSONException(
                        "Ungültiger Shortcut im Backup.");
            }

            JSONArray shortcutCategories =
                    shortcut.optJSONArray(
                            "categories");

            if (shortcutCategories == null) {
                continue;
            }

            for (int j = 0;
                 j < shortcutCategories.length();
                 j++) {

                String categoryId =
                        shortcutCategories.getString(j);

                if (!categoryIds.contains(
                        categoryId)) {

                    throw new JSONException(
                            "Shortcut verweist auf eine unbekannte Kategorie.");
                }
            }
        }

        for (int i = 0;
             i < overviewOrder.length();
             i++) {

            if (overviewOrder
                    .getString(i)
                    .trim()
                    .isEmpty()) {

                throw new JSONException(
                        "Ungültige Sortierung im Backup.");
            }
        }

        Iterator<String> orderKeys =
                sectionItemOrder.keys();

        while (orderKeys.hasNext()) {
            String sectionId =
                    orderKeys.next();

            if (sectionId.trim().isEmpty()) {
                throw new JSONException(
                        "Ungültige Bereichssortierung im Backup.");
            }

            JSONArray itemIds =
                    sectionItemOrder.getJSONArray(
                            sectionId);

            Set<String> seenIds =
                    new HashSet<>();

            for (int i = 0;
                 i < itemIds.length();
                 i++) {

                String itemId =
                        itemIds.getString(i)
                                .trim();

                if (itemId.isEmpty()
                        || !seenIds.add(itemId)) {

                    throw new JSONException(
                            "Ungültige Bereichssortierung im Backup.");
                }
            }
        }
    }
}
