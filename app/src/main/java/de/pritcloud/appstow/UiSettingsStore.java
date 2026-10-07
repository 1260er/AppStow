package de.pritcloud.appstow;

import android.content.Context;
import android.content.SharedPreferences;

final class UiSettingsStore {

    private static final String PREFS_NAME =
            "ui_settings";

    private static final String KEY_START_BEHAVIOR =
            "start_behavior";

    private static final String KEY_CONTROL_SIDE =
            "control_side";

    private static final String LEGACY_SORTING_PREFS =
            "sorting_settings";

    private static final String LEGACY_ALWAYS_START_FAVORITES =
            "always_start_favorites";

    enum StartBehavior {
        NEVER,
        START_ONLY,
        ALWAYS_FOREGROUND
    }

    enum ControlSide {
        RIGHT,
        LEFT
    }

    static final class Settings {

        final StartBehavior startBehavior;
        final ControlSide controlSide;

        Settings(
                StartBehavior startBehavior) {

            this(
                    startBehavior,
                    ControlSide.RIGHT);
        }

        Settings(
                StartBehavior startBehavior,
                ControlSide controlSide) {

            if (startBehavior == null
                    || controlSide == null) {

                throw new IllegalArgumentException(
                        "UI settings are incomplete.");
            }

            this.startBehavior =
                    startBehavior;

            this.controlSide =
                    controlSide;
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

        String storedControlSide =
                preferences.getString(
                        KEY_CONTROL_SIDE,
                        null);

        return new Settings(
                parseStartBehavior(
                        stored,
                        fallback),
                parseControlSide(
                        storedControlSide));
    }

    void save(
            Settings settings) {

        if (settings == null
                || settings.startBehavior == null
                || settings.controlSide == null) {

            throw new IllegalArgumentException(
                    "UI settings are incomplete.");
        }

        preferences.edit()
                .putString(
                        KEY_START_BEHAVIOR,
                        settings.startBehavior.name())
                .putString(
                        KEY_CONTROL_SIDE,
                        settings.controlSide.name())
                .apply();
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
