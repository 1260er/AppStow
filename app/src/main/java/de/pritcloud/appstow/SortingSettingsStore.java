package de.pritcloud.appstow;

import android.content.Context;
import android.content.SharedPreferences;

final class SortingSettingsStore {

    private static final String PREFS_NAME =
            "sorting_settings";

    private static final String KEY_MODE =
            "mode";

    private static final String KEY_RESTORED_AUTO_FAVORITES =
            "restored_auto_favorites_pending";
    private static final String KEY_SEMI_FAVORITES =
            "semi_favorites";
    private static final String KEY_SEMI_CATEGORIES =
            "semi_categories";
    private static final String KEY_SEMI_APPS =
            "semi_apps";
    private static final String KEY_SEMI_SHORTCUTS =
            "semi_shortcuts";
    private static final String KEY_TIME_PROFILE_ENABLED =
            "time_profile_enabled";
    private static final String KEY_DAY_START_HOUR =
            "day_start_hour";
    private static final String KEY_EVENING_START_HOUR =
            "evening_start_hour";
    private static final String KEY_AUTOMATIC_FAVORITE_COUNT =
            "automatic_favorite_count";
    private static final String KEY_ALWAYS_START_FAVORITES =
            "always_start_favorites";
    private static final String KEY_SUGGESTIONS_ENABLED =
            "suggestions_enabled";
    private static final String KEY_SUGGESTION_INTERVAL_DAYS =
            "suggestion_interval_days";
    private static final String KEY_LAST_SUGGESTION_HANDLED_AT =
            "last_suggestion_handled_at";

    static final int DEFAULT_DAY_START_HOUR = 8;
    static final int DEFAULT_EVENING_START_HOUR = 21;
    static final int DEFAULT_AUTOMATIC_FAVORITE_COUNT = 10;
    static final int DEFAULT_SUGGESTION_INTERVAL_DAYS = 30;

    enum Mode {
        MANUAL,
        SEMI_AUTOMATIC,
        AUTOMATIC
    }

    static final class Settings {

        final Mode mode;
        final boolean semiFavorites;
        final boolean semiCategories;
        final boolean semiApps;
        final boolean semiShortcuts;
        final boolean timeProfileEnabled;
        final int dayStartHour;
        final int eveningStartHour;
        final int automaticFavoriteCount;
        final boolean alwaysStartFavorites;
        final boolean suggestionsEnabled;
        final int suggestionIntervalDays;

        Settings(
                Mode mode,
                boolean semiFavorites,
                boolean semiCategories,
                boolean semiApps,
                boolean semiShortcuts,
                boolean timeProfileEnabled,
                int dayStartHour,
                int eveningStartHour,
                int automaticFavoriteCount,
                boolean alwaysStartFavorites) {

            this(
                    mode,
                    semiFavorites,
                    semiCategories,
                    semiApps,
                    semiShortcuts,
                    timeProfileEnabled,
                    dayStartHour,
                    eveningStartHour,
                    automaticFavoriteCount,
                    alwaysStartFavorites,
                    false,
                    DEFAULT_SUGGESTION_INTERVAL_DAYS);
        }

        Settings(
                Mode mode,
                boolean semiFavorites,
                boolean semiCategories,
                boolean semiApps,
                boolean semiShortcuts,
                boolean timeProfileEnabled,
                int dayStartHour,
                int eveningStartHour,
                int automaticFavoriteCount,
                boolean alwaysStartFavorites,
                boolean suggestionsEnabled,
                int suggestionIntervalDays) {

            this.mode = mode;
            this.semiFavorites = semiFavorites;
            this.semiCategories = semiCategories;
            this.semiApps = semiApps;
            this.semiShortcuts = semiShortcuts;
            this.timeProfileEnabled = timeProfileEnabled;
            this.dayStartHour = dayStartHour;
            this.eveningStartHour = eveningStartHour;
            this.automaticFavoriteCount =
                    automaticFavoriteCount;
            this.alwaysStartFavorites =
                    alwaysStartFavorites;
            this.suggestionsEnabled =
                    suggestionsEnabled;
            this.suggestionIntervalDays =
                    suggestionIntervalDays;
        }

        static Settings defaults() {

            return new Settings(
                    Mode.MANUAL,
                    true,
                    true,
                    true,
                    true,
                    false,
                    DEFAULT_DAY_START_HOUR,
                    DEFAULT_EVENING_START_HOUR,
                    DEFAULT_AUTOMATIC_FAVORITE_COUNT,
                    false,
                    false,
                    DEFAULT_SUGGESTION_INTERVAL_DAYS);
        }
    }

    private final SharedPreferences preferences;

    SortingSettingsStore(
            Context context) {

        preferences =
                context.getSharedPreferences(
                        PREFS_NAME,
                        Context.MODE_PRIVATE);
    }

    Settings load() {

        Settings defaults =
                Settings.defaults();

        Mode mode =
                parseMode(
                        preferences.getString(
                                KEY_MODE,
                                defaults.mode.name()));

        int dayStartHour =
                validHourOrDefault(
                        preferences.getInt(
                                KEY_DAY_START_HOUR,
                                defaults.dayStartHour),
                        defaults.dayStartHour);

        int eveningStartHour =
                validHourOrDefault(
                        preferences.getInt(
                                KEY_EVENING_START_HOUR,
                                defaults.eveningStartHour),
                        defaults.eveningStartHour);

        if (dayStartHour
                == eveningStartHour) {

            dayStartHour =
                    defaults.dayStartHour;

            eveningStartHour =
                    defaults.eveningStartHour;
        }

        int favoriteCount =
                preferences.getInt(
                        KEY_AUTOMATIC_FAVORITE_COUNT,
                        defaults.automaticFavoriteCount);

        if (favoriteCount < 3
                || favoriteCount > 30) {

            favoriteCount =
                    defaults.automaticFavoriteCount;
        }

        int suggestionInterval =
                preferences.getInt(
                        KEY_SUGGESTION_INTERVAL_DAYS,
                        defaults.suggestionIntervalDays);

        if (!isValidSuggestionInterval(
                suggestionInterval)) {

            suggestionInterval =
                    defaults.suggestionIntervalDays;
        }

        return new Settings(
                mode,
                preferences.getBoolean(
                        KEY_SEMI_FAVORITES,
                        defaults.semiFavorites),
                preferences.getBoolean(
                        KEY_SEMI_CATEGORIES,
                        defaults.semiCategories),
                preferences.getBoolean(
                        KEY_SEMI_APPS,
                        defaults.semiApps),
                preferences.getBoolean(
                        KEY_SEMI_SHORTCUTS,
                        defaults.semiShortcuts),
                preferences.getBoolean(
                        KEY_TIME_PROFILE_ENABLED,
                        defaults.timeProfileEnabled),
                dayStartHour,
                eveningStartHour,
                favoriteCount,
                preferences.getBoolean(
                        KEY_ALWAYS_START_FAVORITES,
                        defaults.alwaysStartFavorites),
                preferences.getBoolean(
                        KEY_SUGGESTIONS_ENABLED,
                        defaults.suggestionsEnabled),
                suggestionInterval);
    }

    void save(
            Settings settings) {

        validate(
                settings);

        Settings previous = load();

        boolean resetAutomaticSnapshot =
                previous.mode != settings.mode
                        || previous.timeProfileEnabled
                        != settings.timeProfileEnabled
                        || previous.dayStartHour
                        != settings.dayStartHour
                        || previous.eveningStartHour
                        != settings.eveningStartHour;

        boolean restoredFallback =
                settings.mode == Mode.AUTOMATIC
                        && hasRestoredAutomaticFavorites();

        SharedPreferences.Editor editor = preferences.edit()
                .putString(
                        KEY_MODE,
                        settings.mode.name())
                .putBoolean(
                        KEY_RESTORED_AUTO_FAVORITES,
                        restoredFallback)
                .putBoolean(
                        KEY_SEMI_FAVORITES,
                        settings.semiFavorites)
                .putBoolean(
                        KEY_SEMI_CATEGORIES,
                        settings.semiCategories)
                .putBoolean(
                        KEY_SEMI_APPS,
                        settings.semiApps)
                .putBoolean(
                        KEY_SEMI_SHORTCUTS,
                        settings.semiShortcuts)
                .putBoolean(
                        KEY_TIME_PROFILE_ENABLED,
                        settings.timeProfileEnabled)
                .putInt(
                        KEY_DAY_START_HOUR,
                        settings.dayStartHour)
                .putInt(
                        KEY_EVENING_START_HOUR,
                        settings.eveningStartHour)
                .putInt(
                        KEY_AUTOMATIC_FAVORITE_COUNT,
                        settings.automaticFavoriteCount)
                .putBoolean(
                        KEY_ALWAYS_START_FAVORITES,
                        settings.alwaysStartFavorites)
                .putBoolean(
                        KEY_SUGGESTIONS_ENABLED,
                        settings.suggestionsEnabled)
                .putInt(
                        KEY_SUGGESTION_INTERVAL_DAYS,
                        settings.suggestionIntervalDays);

        if (resetAutomaticSnapshot) {
            AutomaticSortingSnapshotStore.invalidate(editor);
        }

        editor.apply();
    }

    boolean hasRestoredAutomaticFavorites() {

        return preferences.getBoolean(
                KEY_RESTORED_AUTO_FAVORITES,
                false);
    }

    void clearRestoredAutomaticFavorites() {

        preferences.edit()
                .remove(
                        KEY_RESTORED_AUTO_FAVORITES)
                .apply();
    }

    long getLastSuggestionHandledAt() {

        return preferences.getLong(
                KEY_LAST_SUGGESTION_HANDLED_AT,
                0L);
    }

    void markSuggestionHandledNow() {

        setLastSuggestionHandledAt(
                System.currentTimeMillis());
    }

    void setLastSuggestionHandledAt(
            long timestamp) {

        preferences.edit()
                .putLong(
                        KEY_LAST_SUGGESTION_HANDLED_AT,
                        Math.max(
                                0L,
                                timestamp))
                .apply();
    }

    boolean isSuggestionDue(
            long nowMillis,
            Settings settings) {

        if (settings == null
                || !settings.suggestionsEnabled) {

            return false;
        }

        long lastHandled =
                getLastSuggestionHandledAt();

        if (lastHandled <= 0L) {
            return true;
        }

        long intervalMillis =
                settings.suggestionIntervalDays
                        * 24L
                        * 60L
                        * 60L
                        * 1000L;

        return nowMillis >= lastHandled
                && nowMillis - lastHandled
                >= intervalMillis;
    }

    private static Mode parseMode(
            String value) {

        if (value == null) {
            return Mode.MANUAL;
        }

        try {
            return Mode.valueOf(
                    value);

        } catch (IllegalArgumentException exception) {

            return Mode.MANUAL;
        }
    }

    private static int validHourOrDefault(
            int value,
            int fallback) {

        return isValidHour(
                value)
                ? value
                : fallback;
    }

    private static void validate(
            Settings settings) {

        if (settings == null
                || settings.mode == null) {

            throw new IllegalArgumentException(
                    "Sorting settings are incomplete.");
        }

        if (!isValidHour(
                settings.dayStartHour)
                || !isValidHour(
                settings.eveningStartHour)
                || settings.dayStartHour
                == settings.eveningStartHour) {

            throw new IllegalArgumentException(
                    "Day and evening start must be distinct valid hours.");
        }

        if (settings.automaticFavoriteCount < 3
                || settings.automaticFavoriteCount > 30) {

            throw new IllegalArgumentException(
                    "Automatic favorite count must be between 3 and 30.");
        }

        if (!isValidSuggestionInterval(
                settings.suggestionIntervalDays)) {

            throw new IllegalArgumentException(
                    "Suggestion interval is invalid.");
        }
    }

    private static boolean isValidHour(
            int value) {

        return value >= 0
                && value <= 23;
    }

    private static boolean isValidSuggestionInterval(
            int value) {

        return value == 7
                || value == 14
                || value == 30
                || value == 60
                || value == 90;
    }
}
