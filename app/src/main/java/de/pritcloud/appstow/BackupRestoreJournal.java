package de.pritcloud.appstow;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AtomicFile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A durable, process-death-safe pre-restore snapshot.
 * Never exported, and excluded from Android system backup.
 */
final class BackupRestoreJournal {

    private static final String FILE_NAME =
            "appstow-restore-journal.json";
    private static final int VERSION = 1;
    private static final int MAX_JOURNAL_BYTES =
            64 * 1024 * 1024;

    private static final String[] PREF_NAMES = {
            "categories",
            "favorites",
            "shortcuts",
            "overview_order",
            "section_item_order",
            "overview_display",
            BackupV3Configuration.SORTING_PREFS,
            BackupV3Configuration.UI_PREFS,
            BackupV3Configuration.STATISTICS_DISPLAY_PREFS,
            BackupV3Configuration.USAGE_STATISTICS_PREFS
    };

    private BackupRestoreJournal() {
    }

    private static AtomicFile file(Context context) {
        return new AtomicFile(
                new File(context.getFilesDir(), FILE_NAME));
    }

    static void prepare(Context context) throws IOException {
        AtomicFile journal = file(context);

        if (journal.getBaseFile().exists()) {
            throw new IOException(
                    "Unabgeschlossene Wiederherstellung vorhanden.");
        }

        JSONObject snapshot = new JSONObject();
        JSONObject stores = new JSONObject();

        try {
            snapshot.put("version", VERSION);
            snapshot.put(
                    "locale",
                    UiLocaleController.getLanguageMode(context)
                            .name());

            for (String name : PREF_NAMES) {
                JSONObject entries = new JSONObject();
                SharedPreferences preferences =
                        context.getSharedPreferences(
                                name,
                                Context.MODE_PRIVATE);

                for (Map.Entry<String, ?> entry :
                        preferences.getAll().entrySet()) {
                    entries.put(
                            entry.getKey(),
                            encode(entry.getValue()));
                }

                stores.put(name, entries);
            }

            snapshot.put("stores", stores);

        } catch (JSONException | RuntimeException exception) {
            throw new IOException(
                    "Sicherungsjournal konnte nicht erstellt werden.",
                    exception);
        }

        byte[] data = snapshot.toString()
                .getBytes(StandardCharsets.UTF_8);

        if (data.length > MAX_JOURNAL_BYTES) {
            throw new IOException(
                    "Sicherungsjournal überschreitet das Größenlimit.");
        }

        FileOutputStream output = null;

        try {
            output = journal.startWrite();
            output.write(data);
            journal.finishWrite(output);

        } catch (IOException | RuntimeException exception) {
            if (output != null) {
                journal.failWrite(output);
            }

            throw new IOException(
                    "Sicherungsjournal konnte nicht geschrieben werden.",
                    exception);
        }
    }

    static boolean recover(Context context) throws IOException {
        AtomicFile journal = file(context);

        if (!journal.getBaseFile().exists()) {
            return false;
        }

        // Decode and validate the ENTIRE journal before
        // touching a single preference store.
        final JSONObject snapshot;

        try (InputStream input = journal.openRead()) {
            ByteArrayOutputStream output =
                    new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int count;

            while ((count = input.read(buffer)) != -1) {
                if (output.size() > MAX_JOURNAL_BYTES - count) {
                    throw new IOException(
                            "Sicherungsjournal ist zu groß.");
                }
                output.write(buffer, 0, count);
            }

            snapshot = new JSONObject(
                    output.toString(
                            StandardCharsets.UTF_8.name()));

        } catch (JSONException exception) {
            throw new IOException(
                    "Sicherungsjournal enthält ungültige Daten.",
                    exception);
        }

        UiLocaleController.LanguageMode previousLocale;
        List<Map<String, Object>> recovered =
                new ArrayList<>();

        try {
            if (snapshot.getInt("version") != VERSION) {
                throw new JSONException("Unbekannte Journalversion.");
            }

            previousLocale = UiLocaleController.LanguageMode.valueOf(
                    snapshot.getString("locale"));

            JSONObject stores = snapshot.getJSONObject("stores");

            if (stores.length() != PREF_NAMES.length) {
                throw new JSONException("Unvollständiges Journal.");
            }

            for (String name : PREF_NAMES) {
                JSONObject entries = stores.getJSONObject(name);
                Map<String, Object> values = new HashMap<>();
                Iterator<String> keys = entries.keys();

                while (keys.hasNext()) {
                    String key = keys.next();
                    values.put(
                            key,
                            decode(entries.getJSONObject(key)));
                }

                recovered.add(values);
            }

        } catch (JSONException | IllegalArgumentException exception) {
            throw new IOException(
                    "Sicherungsjournal konnte nicht validiert werden.",
                    exception);
        }

        for (int index = 0; index < PREF_NAMES.length; index++) {
            SharedPreferences preferences =
                    context.getSharedPreferences(
                            PREF_NAMES[index],
                            Context.MODE_PRIVATE);

            try {
                SharedPreferences.Editor editor =
                        preferences.edit().clear();

                for (Map.Entry<String, Object> entry :
                        recovered.get(index).entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();

                    if (value instanceof String) {
                        editor.putString(key, (String) value);
                    } else if (value instanceof Integer) {
                        editor.putInt(key, (Integer) value);
                    } else if (value instanceof Long) {
                        editor.putLong(key, (Long) value);
                    } else if (value instanceof Float) {
                        editor.putFloat(key, (Float) value);
                    } else if (value instanceof Boolean) {
                        editor.putBoolean(key, (Boolean) value);
                    } else if (value instanceof Set<?>) {
                        @SuppressWarnings("unchecked")
                        Set<String> values = (Set<String>) value;
                        editor.putStringSet(key, values);
                    }
                }

                if (!editor.commit()) {
                    throw new IOException(
                            "Rücksicherung konnte nicht gespeichert werden: "
                                    + PREF_NAMES[index]);
                }

            } catch (RuntimeException exception) {
                throw new IOException(
                        "Rücksicherung fehlgeschlagen: "
                                + PREF_NAMES[index],
                        exception);
            }
        }

        try {
            UiLocaleController.apply(context, previousLocale);
        } catch (RuntimeException exception) {
            throw new IOException(
                    "Rücksicherung der Sprache fehlgeschlagen.",
                    exception);
        }

        complete(context);
        return true;
    }

    static void complete(Context context) throws IOException {
        AtomicFile journal = file(context);
        journal.delete();

        if (journal.getBaseFile().exists()) {
            throw new IOException(
                    "Sicherungsjournal konnte nicht entfernt werden.");
        }
    }

    private static JSONObject encode(Object value)
            throws JSONException {
        JSONObject item = new JSONObject();

        if (value instanceof String) {
            return item.put("type", "string")
                    .put("value", value);
        }
        if (value instanceof Boolean) {
            return item.put("type", "boolean")
                    .put("value", value);
        }
        if (value instanceof Integer) {
            return item.put("type", "integer")
                    .put("value", value.toString());
        }
        if (value instanceof Long) {
            return item.put("type", "long")
                    .put("value", value.toString());
        }
        if (value instanceof Float) {
            return item.put("type", "float")
                    .put("value", value.toString());
        }
        if (value instanceof Set<?>) {
            JSONArray array = new JSONArray();
            for (Object element : (Set<?>) value) {
                if (!(element instanceof String)) {
                    throw new JSONException(
                            "Ungültiger String-Set-Wert im Journal.");
                }
                array.put(element);
            }
            return item.put("type", "string_set")
                    .put("value", array);
        }
        throw new JSONException("Unbekannter Preference-Typ.");
    }

    private static Object decode(JSONObject item)
            throws JSONException {
        String type = item.getString("type");
        Object value = item.get("value");

        try {
            switch (type) {
                case "string":
                    if (!(value instanceof String)) break;
                    return value;
                case "boolean":
                    if (!(value instanceof Boolean)) break;
                    return value;
                case "integer":
                    if (!(value instanceof String)) break;
                    return Integer.parseInt((String) value);
                case "long":
                    if (!(value instanceof String)) break;
                    return Long.parseLong((String) value);
                case "float":
                    if (!(value instanceof String)) break;
                    return Float.parseFloat((String) value);
                case "string_set":
                    if (!(value instanceof JSONArray)) break;
                    JSONArray array = (JSONArray) value;
                    Set<String> strings = new HashSet<>();
                    for (int i = 0; i < array.length(); i++) {
                        Object element = array.get(i);
                        if (!(element instanceof String)
                                || !strings.add((String) element)) {
                            throw new JSONException(
                                    "Ungültiges String-Set im Journal.");
                        }
                    }
                    return strings;
                default:
                    break;
            }
        } catch (NumberFormatException exception) {
            throw new JSONException(
                    "Ungültiger Zahlenwert im Journal.");
        }
        throw new JSONException("Ungültiger Journalwert.");
    }
}
