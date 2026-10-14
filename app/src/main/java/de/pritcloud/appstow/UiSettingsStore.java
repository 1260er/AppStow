package de.pritcloud.appstow;

import android.content.Context;
import android.content.SharedPreferences;

final class UiSettingsStore {

    private static final String PREFS_NAME =
            "ui_settings";

    private static final String KEY_START_BEHAVIOR =
            "start_behavior";

    private static final String KEY_START_VIEW =
            "start_view";

    private static final String KEY_LAST_CATEGORY_ID =
            "last_category_id";

    private static final String KEY_CONTROL_SIDE =
            "control_side";

    private static final String KEY_THEME_MODE =
            "theme_mode";

    private static final String KEY_SHOW_IN_RECENTS =
            "show_in_recents";

    private static final String LEGACY_SORTING_PREFS =
            "sorting_settings";

    private static final String LEGACY_ALWAYS_START_FAVORITES =
            "always_start_favorites";

    enum StartBehavior {
        NEVER,
        START_ONLY,
        ALWAYS_FOREGROUND
    }

    enum StartView {
        LAST_CATEGORY,
        FAVORITES,
        HOME
    }

    enum ControlSide {
        RIGHT,
        LEFT
    }

    enum ThemeMode {
        SYSTEM,
        LIGHT,
        DARK
    }

    static final class Settings {

        final StartBehavior startBehavior;
        final StartView startView;
        final ControlSide controlSide;
        final ThemeMode themeMode;
        final boolean showInRecents;

        Settings(
                StartBehavior startBehavior) {

            this(
                    startBehavior,
                    ControlSide.RIGHT,
                    ThemeMode.SYSTEM,
                    StartView.FAVORITES);
        }

        Settings(
                StartBehavior startBehavior,
                ControlSide controlSide) {

            this(
                    startBehavior,
                    controlSide,
                    ThemeMode.SYSTEM,
                    StartView.FAVORITES);
        }

        Settings(
                StartBehavior startBehavior,
                ControlSide controlSide,
                ThemeMode themeMode) {
            this(startBehavior, controlSide, themeMode, StartView.FAVORITES);
        }

        Settings(
                StartBehavior startBehavior,
                ControlSide controlSide,
                ThemeMode themeMode,
                StartView startView) {
            this(startBehavior, controlSide, themeMode, startView, true);
        }

        Settings(
                StartBehavior startBehavior,
                ControlSide controlSide,
                ThemeMode themeMode,
                StartView startView,
                boolean showInRecents) {

            if (startBehavior == null
                    || controlSide == null
                    || themeMode == null
                    || startView == null) {

                throw new IllegalArgumentException(
                        "UI settings are incomplete.");
            }

            this.startBehavior =
                    startBehavior;

            this.startView =
                    startView;

            this.controlSide =
                    controlSide;

            this.themeMode =
                    themeMode;

            this.showInRecents =
                    showInRecents;
        }
    }

    private final SharedPreferences preferences;
    private final SharedPreferences legacySortingPreferences;

    UiSettingsStore(
            Context context) {

        preferences =
                context.getSharedPreferences(
                        PREFS_NAME,
                        Context.MODE_PRIVATE);

        legacySortingPreferences =
                context.getSharedPreferences(
                        LEGACY_SORTING_PREFS,
                        Context.MODE_PRIVATE);
    }

    Settings load() {

        StartBehavior fallback =
                legacySortingPreferences.getBoolean(
                        LEGACY_ALWAYS_START_FAVORITES,
                        false)
                        ? StartBehavior.START_ONLY
                        : StartBehavior.NEVER;

        String stored =
                preferences.getString(
                        KEY_START_BEHAVIOR,
                        null);

        String storedStartView =
                preferences.getString(
                        KEY_START_VIEW,
                        null);

        String storedControlSide =
                preferences.getString(
                        KEY_CONTROL_SIDE,
                        null);

        String storedThemeMode =
                preferences.getString(
                        KEY_THEME_MODE,
                        null);

        return new Settings(
                parseStartBehavior(
                        stored,
                        fallback),
                parseControlSide(
                        storedControlSide),
                parseThemeMode(
                        storedThemeMode),
                parseStartView(
                        storedStartView),
                preferences.getBoolean(
                        KEY_SHOW_IN_RECENTS,
                        true));
    }

    void save(
            Settings settings) {

        if (settings == null
                || settings.startBehavior == null
                || settings.startView == null
                || settings.controlSide == null
                || settings.themeMode == null) {

            throw new IllegalArgumentException(
                    "UI settings are incomplete.");
        }

        preferences.edit()
                .putString(
                        KEY_START_BEHAVIOR,
                        settings.startBehavior.name())
                .putString(
                        KEY_START_VIEW,
                        settings.startView.name())
                .putString(
                        KEY_CONTROL_SIDE,
                        settings.controlSide.name())
                .putString(
                        KEY_THEME_MODE,
                        settings.themeMode.name())
                .putBoolean(
                        KEY_SHOW_IN_RECENTS,
                        settings.showInRecents)
                .apply();
    }

    void rememberLastCategory(String sectionId) {
        if (sectionId != null
                && sectionId.startsWith("category:")
                && sectionId.length() > "category:".length()) {
            preferences.edit()
                    .putString(KEY_LAST_CATEGORY_ID, sectionId)
                    .apply();
        }
    }

    String getLastCategoryId() {
        String stored = preferences.getString(KEY_LAST_CATEGORY_ID, null);
        return stored != null
                && stored.startsWith("category:")
                && stored.length() > "category:".length()
                ? stored : null;
    }

    private static StartView parseStartView(String stored) {
        if (stored == null) {
            // Older installs used Favorites for START_ONLY and ALWAYS_FOREGROUND.
            return StartView.FAVORITES;
        }
        try {
            return StartView.valueOf(stored);
        } catch (IllegalArgumentException exception) {
            return StartView.FAVORITES;
        }
    }

    private static ThemeMode parseThemeMode(
            String value) {

        if (value == null) {
            return ThemeMode.SYSTEM;
        }

        try {
            return ThemeMode.valueOf(
                    value);

        } catch (IllegalArgumentException exception) {

            return ThemeMode.SYSTEM;
        }
    }

    private static ControlSide parseControlSide(
            String value) {

        if (value == null) {
            return ControlSide.RIGHT;
        }

        try {
            return ControlSide.valueOf(
                    value);

        } catch (IllegalArgumentException exception) {

            return ControlSide.RIGHT;
        }
    }

    private static StartBehavior parseStartBehavior(
            String value,
            StartBehavior fallback) {

        if (value == null) {
            return fallback;
        }

        try {
            return StartBehavior.valueOf(
                    value);

        } catch (IllegalArgumentException exception) {

            return fallback;
        }
    }
}
