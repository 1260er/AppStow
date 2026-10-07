package de.pritcloud.appstow;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SyncFailedException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class BackupManager {

    private static final String FORMAT_ID =
            BackupCrypto.FORMAT_ID;

    private static final int FORMAT_VERSION =
            BackupCrypto.FORMAT_VERSION;

    private static final String DISPLAY_PREFS =
            "overview_display";
    private static final String DISPLAY_SECTION_PREFIX =
            "section_grid_columns_";

    private static final int MAX_BACKUP_BYTES =
            5 * 1024 * 1024;

    private static final int READ_ATTEMPTS =
            15;

    private static final long READ_RETRY_SHORT_MS =
            250L;

    private static final long READ_RETRY_MEDIUM_MS =
            750L;

    private static final long READ_RETRY_LONG_MS =
            1500L;

    private static final long READ_RETRY_FINAL_MS =
            2500L;

    private BackupManager() {
    }

    static void writeBackup(
            Context context,
            Uri uri)
            throws IOException, JSONException {

        JSONObject backup =
                createBackup(context);

        JSONObject envelope =
                BackupCrypto.encrypt(
                        backup);

        byte[] data =
                envelope.toString(2)
                        .getBytes(
                                StandardCharsets.UTF_8);

        ParcelFileDescriptor descriptor =
                context.getContentResolver()
                        .openFileDescriptor(
                                uri,
                                "w");

        if (descriptor == null) {
            throw new IOException(
                    "Backup-Datei konnte nicht geöffnet werden.");
        }

        try (ParcelFileDescriptor.AutoCloseOutputStream stream =
                     new ParcelFileDescriptor.AutoCloseOutputStream(
                             descriptor)) {

            stream.write(
                    data);

            stream.flush();

            try {
                descriptor.getFileDescriptor()
                        .sync();

            } catch (SyncFailedException ignored) {
            }
        }

        verifyWrittenBackup(
                context,
                uri,
                data);
    }

    private static void verifyWrittenBackup(
            Context context,
            Uri uri,
            byte[] expectedData)
            throws IOException {

        IOException lastException =
                null;

        for (int attempt = 0;
             attempt < READ_ATTEMPTS;
             attempt++) {

            try {
                byte[] actualData =
                        readBackupData(
                                context,
                                uri);

                if (Arrays.equals(
                        expectedData,
                        actualData)) {

                    return;
                }

                if (actualData.length == 0) {

                    lastException =
                            new IOException(
                                    "Der Speicheranbieter liefert die neue Backup-Datei noch leer zurück.");

                } else {

                    lastException =
                            new IOException(
                                    "Der Speicheranbieter liefert noch nicht den vollständig geschriebenen Backup-Inhalt zurück.");
                }

            } catch (IOException exception) {

                lastException =
                        exception;
            }

            waitForProvider(
                    attempt);
        }

        throw new IOException(
                "Backup wurde geschrieben, konnte über den gewählten Speicheranbieter aber nicht zuverlässig zurückgelesen werden.",
                lastException);
    }

    private static byte[] readBackupData(
            Context context,
            Uri uri)
            throws IOException {

        ByteArrayOutputStream output =
                new ByteArrayOutputStream();

        try (InputStream stream =
                     context.getContentResolver()
                             .openInputStream(
                                     uri)) {

            if (stream == null) {
                throw new IOException(
                        "Backup-Datei konnte nicht geöffnet werden.");
            }

            byte[] buffer =
                    new byte[8192];

            int count;

            while ((count =
                    stream.read(
                            buffer)) != -1) {

                if (output.size()
                        > MAX_BACKUP_BYTES
                        - count) {

                    throw new IOException(
                            "Backup-Datei ist zu groß.");
                }

                output.write(
                        buffer,
                        0,
                        count);
            }
        }

        return output.toByteArray();
    }

    static JSONObject readBackup(
            Context context,
            Uri uri)
            throws IOException, JSONException {

        /*
         * Cloud-/DocumentsProvider können kurzzeitig einen leeren,
         * unvollständigen oder noch gecachten Stand liefern.
         *
         * Deshalb wird adaptiv erneut gelesen. Eindeutig falsche
         * AppStow-Formatversionen werden dagegen sofort abgelehnt.
         * Sobald ein Payload erfolgreich entschlüsselt wurde, gilt
         * ein Validierungsfehler ebenfalls als endgültig.
         */
        Exception lastReadException =
                null;

        for (int attempt = 0;
             attempt < READ_ATTEMPTS;
             attempt++) {

            byte[] data;

            try {
                data =
                        readBackupData(
                                context,
                                uri);

            } catch (IOException exception) {

                lastReadException =
                        exception;

                waitForProvider(
                        attempt);

                continue;
            }

            if (data.length == 0) {

                lastReadException =
                        new IOException(
                                "Backup-Datei ist noch leer. "
                                        + "Der Speicheranbieter hat sie möglicherweise "
                                        + "noch nicht vollständig bereitgestellt.");

                waitForProvider(
                        attempt);

                continue;
            }

            JSONObject envelope;

            try {
                envelope =
                        new JSONObject(
                                new String(
                                        data,
                                        StandardCharsets.UTF_8));

            } catch (JSONException exception) {

                lastReadException =
                        exception;

                waitForProvider(
                        attempt);

                continue;
            }

            if (!FORMAT_ID.equals(
                    envelope.optString(
                            "format",
                            ""))
                    || envelope.optInt(
                            "formatVersion",
                            -1)
                    != FORMAT_VERSION) {

                throw new JSONException(
                        "Diese Backup-Datei gehört nicht zum unterstützten AppStow-Backupformat v3.");
            }

            JSONObject backup;

            try {
                backup =
                        BackupCrypto.decrypt(
                                envelope);

            } catch (IOException
                     | JSONException exception) {

                lastReadException =
                        exception;

                waitForProvider(
                        attempt);

                continue;
            }

            validateBackup(
                    backup,
                    true);

            return backup;
        }

        if (lastReadException
                instanceof JSONException) {

            throw (JSONException)
                    lastReadException;
        }

        if (lastReadException
                instanceof IOException) {

            throw (IOException)
                    lastReadException;
        }

        throw new IOException(
                "Backup-Datei konnte nicht gelesen werden.");
    }

    private static void waitForProvider(
            int completedAttempt)
            throws IOException {

        if (completedAttempt
                + 1
                >= READ_ATTEMPTS) {

            return;
        }

        long delayMillis;

        if (completedAttempt < 4) {

            delayMillis =
                    READ_RETRY_SHORT_MS;

        } else if (completedAttempt < 8) {

            delayMillis =
                    READ_RETRY_MEDIUM_MS;

        } else if (completedAttempt < 12) {

            delayMillis =
                    READ_RETRY_LONG_MS;

        } else {

            delayMillis =
                    READ_RETRY_FINAL_MS;
        }

        try {
            Thread.sleep(
                    delayMillis);

        } catch (InterruptedException exception) {

            Thread.currentThread()
                    .interrupt();

            throw new IOException(
                    "Backup-Lesevorgang wurde unterbrochen.",
                    exception);
        }
    }

    static void restoreBackup(
            Context context,
            JSONObject backup)
            throws JSONException, IOException {

        Set<String> installedLauncherPackages =
                BackupRestoreFilter
                        .queryInstalledLauncherPackages(
                                context);

        restoreBackup(
                context,
                backup,
                installedLauncherPackages);
    }

    static void restoreBackup(
            Context context,
            JSONObject backup,
            Set<String> installedLauncherPackages)
            throws JSONException, IOException {

        validateBackup(
                backup,
                true);

        BackupV3Configuration.State configuration =
                BackupV3Configuration.parse(
                        backup);

        JSONArray categories =
                backup.getJSONArray(
                        "categories");

        JSONObject assignments =
                BackupRestoreFilter.filterAssignments(
                        backup.getJSONObject(
                                "categoryAssignments"),
                        installedLauncherPackages);

        boolean categorySymbolsEnabled =
                backup.optBoolean(
                        "categorySymbolsEnabled",
                        false);

        Set<String> favorites =
                BackupRestoreFilter.filterFavorites(
                        backup.getJSONArray(
                                "favoritePackages"),
                        installedLauncherPackages);

        JSONArray shortcuts =
                backup.getJSONArray(
                        "shortcuts");

        JSONArray overviewOrder =
                backup.getJSONArray(
                        "overviewOrder");

        JSONObject sectionItemOrder =
                BackupRestoreFilter.filterSectionItemOrder(
                        backup.getJSONObject(
                                "sectionItemOrder"),
                        installedLauncherPackages);

        JSONObject display =
                backup.optJSONObject(
                        "overviewDisplay");

        boolean gridMode =
                display != null
                        && display.getBoolean(
                                "gridMode");

        int gridColumns =
                display == null
                        ? 4
                        : display.getInt(
                                "gridColumns");

        JSONObject sectionColumns =
                display == null
                        ? new JSONObject()
                        : display.getJSONObject(
                                "sectionColumns");

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

        SharedPreferences displayPrefs =
                context.getSharedPreferences(
                        DISPLAY_PREFS,
                        Context.MODE_PRIVATE);

        SharedPreferences sortingPrefs =
                context.getSharedPreferences(
                        BackupV3Configuration.SORTING_PREFS,
                        Context.MODE_PRIVATE);

        SharedPreferences uiPrefs =
                context.getSharedPreferences(
                        BackupV3Configuration.UI_PREFS,
                        Context.MODE_PRIVATE);

        SharedPreferences statisticsDisplayPrefs =
                context.getSharedPreferences(
                        BackupV3Configuration.STATISTICS_DISPLAY_PREFS,
                        Context.MODE_PRIVATE);

        SharedPreferences usageStatisticsPrefs =
                context.getSharedPreferences(
                        BackupV3Configuration.USAGE_STATISTICS_PREFS,
                        Context.MODE_PRIVATE);

        Map<String, Object> categorySnapshot =
                snapshotPreferences(
                        categoryPrefs);

        Map<String, Object> favoriteSnapshot =
                snapshotPreferences(
                        favoritePrefs);

        Map<String, Object> shortcutSnapshot =
                snapshotPreferences(
                        shortcutPrefs);

        Map<String, Object> orderSnapshot =
                snapshotPreferences(
                        orderPrefs);

        Map<String, Object> sectionItemOrderSnapshot =
                snapshotPreferences(
                        sectionItemOrderPrefs);

        Map<String, Object> displaySnapshot =
                snapshotPreferences(
                        displayPrefs);

        Map<String, Object> sortingSnapshot =
                snapshotPreferences(
                        sortingPrefs);

        Map<String, Object> uiSnapshot =
                snapshotPreferences(
                        uiPrefs);

        Map<String, Object> statisticsDisplaySnapshot =
                snapshotPreferences(
                        statisticsDisplayPrefs);

        Map<String, Object> usageStatisticsSnapshot =
                snapshotPreferences(
                        usageStatisticsPrefs);

        UiLocaleController.LanguageMode currentLanguage =
                UiLocaleController.getLanguageMode(
                        context);

        boolean languageChanged =
                currentLanguage
                        != configuration.language;

        try {
            boolean categoriesSaved =
                    categoryPrefs
                            .edit()
                            .clear()
                            .putString(
                                    "category_list",
                                    categories.toString())
                            .putString(
                                    "category_assignments",
                                    assignments.toString())
                            .putBoolean(
                                    "category_symbols_enabled",
                                    categorySymbolsEnabled)
                            .commit();

            boolean favoritesSaved =
                    favoritePrefs
                            .edit()
                            .clear()
                            .putStringSet(
                                    "packages",
                                    favorites)
                            .commit();

            boolean shortcutsSaved =
                    shortcutPrefs
                            .edit()
                            .clear()
                            .putString(
                                    "shortcut_list",
                                    shortcuts.toString())
                            .commit();

            boolean orderSaved =
                    orderPrefs
                            .edit()
                            .clear()
                            .putString(
                                    "section_order",
                                    overviewOrder.toString())
                            .commit();

            boolean sectionItemOrderSaved =
                    sectionItemOrderPrefs
                            .edit()
                            .clear()
                            .putString(
                                    "orders",
                                    sectionItemOrder.toString())
                            .commit();

            SharedPreferences.Editor displayEditor =
                    displayPrefs.edit()
                            .clear()
                            .putBoolean(
                                    "grid_mode",
                                    gridMode)
                            .putInt(
                                    "grid_columns",
                                    gridColumns);

            Iterator<String> displayKeys =
                    sectionColumns.keys();

            while (displayKeys.hasNext()) {
                String sectionId =
                        displayKeys.next();

                displayEditor.putInt(
                        DISPLAY_SECTION_PREFIX
                                + sectionId,
                        sectionColumns.getInt(
                                sectionId));
            }

            boolean displaySaved =
                    displayEditor.commit();

            boolean configurationSaved =
                    BackupV3Configuration
                            .restorePreferences(
                                    context,
                                    configuration);

            boolean statisticsReset =
                    usageStatisticsPrefs
                            .edit()
                            .clear()
                            .commit();

            if (!categoriesSaved
                    || !favoritesSaved
                    || !shortcutsSaved
                    || !orderSaved
                    || !sectionItemOrderSaved
                    || !displaySaved
                    || !configurationSaved
                    || !statisticsReset) {

                throw new IOException(
                        "Backup konnte nicht vollständig wiederhergestellt werden.");
            }

            if (languageChanged) {

                UiLocaleController.apply(
                        context,
                        configuration.language);
            }

        } catch (IOException | RuntimeException exception) {

            boolean rollbackSaved =
                    restorePreferences(
                            categoryPrefs,
                            categorySnapshot);

            rollbackSaved &=
                    restorePreferences(
                            favoritePrefs,
                            favoriteSnapshot);

            rollbackSaved &=
                    restorePreferences(
                            shortcutPrefs,
                            shortcutSnapshot);

            rollbackSaved &=
                    restorePreferences(
                            orderPrefs,
                            orderSnapshot);

            rollbackSaved &=
                    restorePreferences(
                            sectionItemOrderPrefs,
                            sectionItemOrderSnapshot);

            rollbackSaved &=
                    restorePreferences(
                            displayPrefs,
                            displaySnapshot);

            rollbackSaved &=
                    restorePreferences(
                            sortingPrefs,
                            sortingSnapshot);

            rollbackSaved &=
                    restorePreferences(
                            uiPrefs,
                            uiSnapshot);

            rollbackSaved &=
                    restorePreferences(
                            statisticsDisplayPrefs,
                            statisticsDisplaySnapshot);

            rollbackSaved &=
                    restorePreferences(
                            usageStatisticsPrefs,
                            usageStatisticsSnapshot);

            boolean languageRollbackSaved =
                    true;

            if (languageChanged) {

                try {
                    UiLocaleController.apply(
                            context,
                            currentLanguage);

                } catch (RuntimeException rollbackException) {

                    languageRollbackSaved =
                            false;
                }
            }

            if (!rollbackSaved
                    || !languageRollbackSaved) {

                throw new IOException(
                        "Wiederherstellung und Rollback sind fehlgeschlagen.",
                        exception);
            }

            if (exception instanceof IOException) {
                throw (IOException) exception;
            }

            throw new IOException(
                    "Backup konnte nicht vollständig wiederhergestellt werden.",
                    exception);
        }
    }

    private static Map<String, Object> snapshotPreferences(
            SharedPreferences preferences) {

        Map<String, Object> snapshot =
                new HashMap<>();

        for (Map.Entry<String, ?> entry :
                preferences.getAll()
                        .entrySet()) {

            Object value =
                    entry.getValue();

            if (value instanceof Set<?>) {
                value =
                        new HashSet<>(
                                (Set<?>) value);
            }

            snapshot.put(
                    entry.getKey(),
                    value);
        }

        return snapshot;
    }

    private static boolean restorePreferences(
            SharedPreferences preferences,
            Map<String, Object> snapshot) {

        try {
            SharedPreferences.Editor editor =
                    preferences.edit()
                            .clear();

            for (Map.Entry<String, Object> entry :
                    snapshot.entrySet()) {

                String key =
                        entry.getKey();

                Object value =
                        entry.getValue();

                if (value instanceof String) {
                    editor.putString(
                            key,
                            (String) value);

                } else if (value instanceof Set<?>) {
                    Set<String> strings =
                            new HashSet<>();

                    for (Object item :
                            (Set<?>) value) {

                        if (!(item instanceof String)) {
                            return false;
                        }

                        strings.add(
                                (String) item);
                    }

                    editor.putStringSet(
                            key,
                            strings);

                } else if (value instanceof Integer) {
                    editor.putInt(
                            key,
                            (Integer) value);

                } else if (value instanceof Long) {
                    editor.putLong(
                            key,
                            (Long) value);

                } else if (value instanceof Float) {
                    editor.putFloat(
                            key,
                            (Float) value);

                } else if (value instanceof Boolean) {
                    editor.putBoolean(
                            key,
                            (Boolean) value);

                } else {
                    return false;
                }
            }

            return editor.commit();

        } catch (RuntimeException exception) {
            return false;
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

        SharedPreferences displayPrefs =
                context.getSharedPreferences(
                        DISPLAY_PREFS,
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

        boolean categorySymbolsEnabled =
                categoryPrefs.getBoolean(
                        "category_symbols_enabled",
                        false);

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

        JSONObject display =
                new JSONObject();

        display.put(
                "gridMode",
                displayPrefs.getBoolean(
                        "grid_mode",
                        false));

        display.put(
                "gridColumns",
                displayPrefs.getInt(
                        "grid_columns",
                        4));

        Set<String> validSectionIds =
                new HashSet<>();

        validSectionIds.add("favorites");
        validSectionIds.add("shortcuts");

        for (int i = 0;
             i < categories.length();
             i++) {

            validSectionIds.add(
                    "category:"
                            + categories.getJSONObject(i)
                                    .getString("id"));
        }

        JSONObject sectionColumns =
                new JSONObject();

        for (Map.Entry<String, ?> entry :
                displayPrefs.getAll().entrySet()) {

            String key =
                    entry.getKey();

            if (!key.startsWith(
                    DISPLAY_SECTION_PREFIX)) {

                continue;
            }

            String sectionId =
                    key.substring(
                            DISPLAY_SECTION_PREFIX.length());

            if (validSectionIds.contains(
                    sectionId)) {

                sectionColumns.put(
                        sectionId,
                        entry.getValue());
            }
        }

        display.put(
                "sectionColumns",
                sectionColumns);

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
                "categorySymbolsEnabled",
                categorySymbolsEnabled);

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

        backup.put(
                "overviewDisplay",
                display);

        backup.put(
                "settings",
                BackupV3Configuration.create(
                        context));

        validateBackup(
                backup,
                true);

        return backup;
    }

    private static boolean isKnownShortcutType(
            String type) {

        return ShortcutEntry.TYPE_WEBSITE.equals(type)
                || ShortcutEntry.TYPE_WEB_APP.equals(type)
                || ShortcutEntry.TYPE_DEEP_LINK.equals(type)
                || ShortcutEntry.TYPE_APP_SETTINGS.equals(type);
    }

    private static boolean isValidShortcutTarget(
            String type,
            String target) {

        try {
            if (ShortcutEntry.TYPE_APP_SETTINGS.equals(
                    type)) {

                String packageName =
                        target.startsWith(
                                "package:")
                                ? target.substring(
                                        "package:".length())
                                : target;

                return !packageName
                        .trim()
                        .isEmpty();
            }

            Uri uri =
                    Uri.parse(target);

            String scheme =
                    uri.getScheme();

            if (ShortcutEntry.TYPE_DEEP_LINK.equals(
                    type)) {

                return scheme != null
                        && !scheme.trim()
                                .isEmpty();
            }

            String host =
                    uri.getHost();

            if (scheme == null
                    || host == null
                    || host.isEmpty()) {

                return false;
            }

            if (ShortcutEntry.TYPE_WEB_APP.equals(
                    type)) {

                return "https".equalsIgnoreCase(
                        scheme);
            }

            return "http".equalsIgnoreCase(
                    scheme)
                    || "https".equalsIgnoreCase(
                            scheme);

        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static String requireCanonicalNonEmpty(
            String value,
            String errorMessage)
            throws JSONException {

        if (value.isEmpty()
                || !value.equals(
                        value.trim())) {

            throw new JSONException(
                    errorMessage);
        }

        return value;
    }

    private static int requireGridColumns(
            Object value)
            throws JSONException {

        if (!(value instanceof Number)) {
            throw new JSONException(
                    "Ungültige Spaltenzahl im Backup.");
        }

        Number number =
                (Number) value;

        int columns =
                number.intValue();

        if (columns < 3
                || columns > 5
                || number.doubleValue()
                        != columns) {

            throw new JSONException(
                    "Ungültige Spaltenzahl im Backup.");
        }

        return columns;
    }

    private static void validateBackup(
            JSONObject backup,
            boolean strictShortcutValidation)
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

        BackupV3Configuration.validate(
                backup);

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

        if (backup.has(
                "categorySymbolsEnabled")
                && !(backup.get(
                        "categorySymbolsEnabled")
                instanceof Boolean)) {

            throw new JSONException(
                    "Ungültige Symbol-Einstellung im Backup.");
        }

        Set<String> categoryIds =
                new HashSet<>();

        Set<String> categoryNames =
                new HashSet<>();

        for (int i = 0;
             i < categories.length();
             i++) {

            JSONObject category =
                    categories.getJSONObject(i);

            String id =
                    requireCanonicalNonEmpty(
                            category.getString("id"),
                            "Ungültige Kategorien im Backup.");

            String name =
                    requireCanonicalNonEmpty(
                            category.getString("name"),
                            "Ungültige Kategorien im Backup.");

            if (category.has("symbol")) {
                requireCanonicalNonEmpty(
                        category.getString("symbol"),
                        "Ungültige Kategorien im Backup.");
            }

            if (!categoryIds.add(id)
                    || !categoryNames.add(
                            name.toLowerCase(
                                    Locale.ROOT))) {

                throw new JSONException(
                        "Ungültige Kategorien im Backup.");
            }
        }

        if (backup.has(
                "overviewDisplay")) {

            JSONObject display =
                    backup.getJSONObject(
                            "overviewDisplay");

            if (!display.has("gridMode")
                    || !(display.get("gridMode")
                    instanceof Boolean)) {

                throw new JSONException(
                        "Ungültige Ansicht im Backup.");
            }

            requireGridColumns(
                    display.get("gridColumns"));

            JSONObject sectionColumns =
                    display.getJSONObject(
                            "sectionColumns");

            Iterator<String> displayKeys =
                    sectionColumns.keys();

            while (displayKeys.hasNext()) {
                String sectionId =
                        displayKeys.next();

                boolean knownSection =
                        "favorites".equals(
                                sectionId)
                                || "shortcuts".equals(
                                        sectionId)
                                || (sectionId.startsWith(
                                        "category:")
                                && categoryIds.contains(
                                        sectionId.substring(
                                                "category:".length())));

                if (!knownSection) {
                    throw new JSONException(
                            "Unbekannte Kategorie in der Anzeigeeinstellung.");
                }

                requireGridColumns(
                        sectionColumns.get(
                                sectionId));
            }
        }

        Iterator<String> assignmentKeys =
                assignments.keys();

        while (assignmentKeys.hasNext()) {
            String packageName =
                    requireCanonicalNonEmpty(
                            assignmentKeys.next(),
                            "Ungültige App-Zuweisung im Backup.");

            JSONArray ids =
                    assignments.getJSONArray(
                            packageName);

            for (int i = 0;
                 i < ids.length();
                 i++) {

                String categoryId =
                        requireCanonicalNonEmpty(
                                ids.getString(i),
                                "Ungültige App-Zuweisung im Backup.");

                if (!categoryIds.contains(
                        categoryId)) {

                    throw new JSONException(
                            "App-Zuweisung verweist auf eine unbekannte Kategorie.");
                }
            }
        }

        Set<String> favoritePackageIds =
                new HashSet<>();

        for (int i = 0;
             i < favoritePackages.length();
             i++) {

            String packageName =
                    requireCanonicalNonEmpty(
                            favoritePackages.getString(i),
                            "Ungültiger oder doppelter Favorit im Backup.");

            if (!favoritePackageIds.add(
                    packageName)) {

                throw new JSONException(
                        "Ungültiger oder doppelter Favorit im Backup.");
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
                    requireCanonicalNonEmpty(
                            shortcut.getString("id"),
                            "Ungültiger Shortcut im Backup.");

            String name =
                    requireCanonicalNonEmpty(
                            shortcut.getString("name"),
                            "Ungültiger Shortcut im Backup.");

            String type =
                    requireCanonicalNonEmpty(
                            shortcut.getString("type"),
                            "Ungültiger Shortcut im Backup.");

            String target =
                    requireCanonicalNonEmpty(
                            shortcut.getString("target"),
                            "Ungültiger Shortcut im Backup.");

            if (!shortcutIds.add(id)
                    || (strictShortcutValidation
                    && (!isKnownShortcutType(type)
                    || !isValidShortcutTarget(
                            type,
                            target)))) {

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
                        requireCanonicalNonEmpty(
                                shortcutCategories.getString(j),
                                "Shortcut verweist auf eine ungültige Kategorie.");

                if (!categoryIds.contains(
                        categoryId)) {

                    throw new JSONException(
                            "Shortcut verweist auf eine unbekannte Kategorie.");
                }
            }
        }

        Set<String> overviewIds =
                new HashSet<>();

        for (int i = 0;
             i < overviewOrder.length();
             i++) {

            String sectionId =
                    requireCanonicalNonEmpty(
                            overviewOrder.getString(i),
                            "Ungültige oder doppelte Sortierung im Backup.");

            if (!overviewIds.add(
                    sectionId)) {

                throw new JSONException(
                        "Ungültige oder doppelte Sortierung im Backup.");
            }
        }

        Iterator<String> orderKeys =
                sectionItemOrder.keys();

        while (orderKeys.hasNext()) {
            String sectionId =
                    requireCanonicalNonEmpty(
                            orderKeys.next(),
                            "Ungültige Bereichssortierung im Backup.");

            JSONArray itemIds =
                    sectionItemOrder.getJSONArray(
                            sectionId);

            Set<String> seenIds =
                    new HashSet<>();

            for (int i = 0;
                 i < itemIds.length();
                 i++) {

                String itemId =
                        requireCanonicalNonEmpty(
                                itemIds.getString(i),
                                "Ungültige Bereichssortierung im Backup.");

                if (!seenIds.add(itemId)) {

                    throw new JSONException(
                            "Ungültige Bereichssortierung im Backup.");
                }
            }
        }
    }
}
