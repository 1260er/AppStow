package de.pritcloud.appstow;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Only user-selected sorting configuration is mirrored here for Android
 * Auto Backup (Seedvault / Google). NEVER copy the raw sorting_settings
 * preferences: they also contain usage-derived automatic snapshots.
 */
final class SystemBackupSortingStore {

    static final String PREFS_NAME = "system_backup_sorting";
    private static final String KEY_CONFIGURATION = "configuration_v1";

    private SystemBackupSortingStore() {
    }

    static void save(
            Context context,
            SortingSettingsStore.Settings settings) {

        try {
            JSONObject config = new JSONObject()
                    .put("version", 1)
                    .put("mode", settings.mode.name())
                    .put("semiFavorites", settings.semiFavorites)
                    .put("semiCategories", settings.semiCategories)
                    .put("semiApps", settings.semiApps)
                    .put("semiShortcuts", settings.semiShortcuts)
                    .put("timeProfileEnabled", settings.timeProfileEnabled)
                    .put("dayStartHour", settings.dayStartHour)
                    .put("eveningStartHour", settings.eveningStartHour)
                    .put("automaticFavoriteCount", settings.automaticFavoriteCount)
                    .put("alwaysStartFavorites", settings.alwaysStartFavorites)
                    .put("suggestionsEnabled", settings.suggestionsEnabled)
                    .put("suggestionIntervalDays", settings.suggestionIntervalDays);

            SharedPreferences preferences = context.getSharedPreferences(
                    PREFS_NAME, Context.MODE_PRIVATE);

            String payload = config.toString();
            if (!payload.equals(preferences.getString(KEY_CONFIGURATION, null))) {
                if (!preferences.edit()
                        .putString(KEY_CONFIGURATION, payload)
                        .commit()) {
                    throw new IllegalStateException(
                            "Sorting system-backup mirror could not be saved.");
                }
            }

        } catch (JSONException exception) {
            throw new IllegalStateException(
                    "Sorting backup configuration cannot be serialized.",
                    exception);
        }
    }

    static SortingSettingsStore.Settings load(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFS_NAME, Context.MODE_PRIVATE);

        try {
            String value = preferences.getString(KEY_CONFIGURATION, null);
            if (value == null) {
                return null;
            }

            JSONObject config = new JSONObject(value);
            if (config.getInt("version") != 1) {
                return null;
            }

            SortingSettingsStore.Settings settings =
                    new SortingSettingsStore.Settings(
                            SortingSettingsStore.Mode.valueOf(
                                    config.getString("mode")),
                            config.getBoolean("semiFavorites"),
                            config.getBoolean("semiCategories"),
                            config.getBoolean("semiApps"),
                            config.getBoolean("semiShortcuts"),
                            config.getBoolean("timeProfileEnabled"),
                            config.getInt("dayStartHour"),
                            config.getInt("eveningStartHour"),
                            config.getInt("automaticFavoriteCount"),
                            config.getBoolean("alwaysStartFavorites"),
                            config.getBoolean("suggestionsEnabled"),
                            config.getInt("suggestionIntervalDays"));

            if (settings.dayStartHour < 0 || settings.dayStartHour > 23
                    || settings.eveningStartHour < 0
                    || settings.eveningStartHour > 23
                    || settings.dayStartHour == settings.eveningStartHour
                    || settings.automaticFavoriteCount < 3
                    || settings.automaticFavoriteCount > 30
                    || (settings.suggestionIntervalDays != 7
                    && settings.suggestionIntervalDays != 14
                    && settings.suggestionIntervalDays != 30
                    && settings.suggestionIntervalDays != 60
                    && settings.suggestionIntervalDays != 90)) {
                return null;
            }

            return settings;

        } catch (JSONException | IllegalArgumentException
                 | ClassCastException exception) {
            return null;
        }
    }
}
