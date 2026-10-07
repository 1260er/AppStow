package de.pritcloud.appstow;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class BackupV3Configuration {

    static final String SORTING_PREFS =
            "sorting_settings";

    static final String UI_PREFS =
            "ui_settings";

    static final String STATISTICS_DISPLAY_PREFS =
            "statistics_display";

    static final String USAGE_STATISTICS_PREFS =
            "usage_statistics";

    private static final String KEY_SETTINGS =
            "settings";

    private static final String KEY_SORTING =
            "sorting";

    private static final String KEY_UI =
            "ui";

    private static final String KEY_STATISTICS_DISPLAY =
            "statisticsDisplay";

    private static final String KEY_STATISTICS_PERIOD =
            "statistics_period";

    private static final String KEY_STATISTICS_CARD_ORDER =
            "statistics_card_order_v2";

    private static final String KEY_STATISTICS_TOP_LIMIT =
            "statistics_top_limit";

    private static final int DEFAULT_STATISTICS_TOP_LIMIT =
            10;

    private static final int STATISTICS_TOP_LIMIT_ALL =
            -1;

    static final class State {

        final SortingSettingsStore.Settings sorting;
        final UiSettingsStore.Settings ui;
        final UiLocaleController.LanguageMode language;
        final UsageStatisticsStore.Period statisticsPeriod;
        final int statisticsTopLimit;
        final List<String> statisticsCardOrder;

        State(
                SortingSettingsStore.Settings sorting,
                UiSettingsStore.Settings ui,
                UiLocaleController.LanguageMode language,
                UsageStatisticsStore.Period statisticsPeriod,
                int statisticsTopLimit,
                List<String> statisticsCardOrder) {

            this.sorting =
                    sorting;

            this.ui =
                    ui;

            this.language =
                    language;

            this.statisticsPeriod =
                    statisticsPeriod;

            this.statisticsTopLimit =
                    statisticsTopLimit;

            this.statisticsCardOrder =
                    new ArrayList<>(
                            statisticsCardOrder);
        }
    }

    private BackupV3Configuration() {
    }

    static JSONObject create(
            Context context)
            throws JSONException {

        SortingSettingsStore.Settings sorting =
                new SortingSettingsStore(
                        context)
                        .load();

        UiSettingsStore.Settings ui =
                new UiSettingsStore(
                        context)
                        .load();

        UiLocaleController.LanguageMode language =
                UiLocaleController.getLanguageMode(
                        context);

        SharedPreferences statisticsDisplay =
                context.getSharedPreferences(
                        STATISTICS_DISPLAY_PREFS,
                        Context.MODE_PRIVATE);

        UsageStatisticsStore.Period period =
                parseStoredStatisticsPeriod(
                        statisticsDisplay.getString(
                                KEY_STATISTICS_PERIOD,
                                UsageStatisticsStore.Period
                                        .ONE_MONTH
                                        .name()));

        int topLimit =
                statisticsDisplay.getInt(
                        KEY_STATISTICS_TOP_LIMIT,
                        DEFAULT_STATISTICS_TOP_LIMIT);

        if (!isValidStatisticsTopLimit(
                topLimit)) {

            topLimit =
                    DEFAULT_STATISTICS_TOP_LIMIT;
        }

        List<String> cardOrder =
                parseStoredCardOrder(
                        statisticsDisplay.getString(
                                KEY_STATISTICS_CARD_ORDER,
                                ""));

        JSONObject sortingJson =
                new JSONObject()
                        .put(
                                "mode",
                                sorting.mode.name())
                        .put(
                                "semiFavorites",
                                sorting.semiFavorites)
                        .put(
                                "semiCategories",
                                sorting.semiCategories)
                        .put(
                                "semiApps",
                                sorting.semiApps)
                        .put(
                                "semiShortcuts",
                                sorting.semiShortcuts)
                        .put(
                                "timeProfileEnabled",
                                sorting.timeProfileEnabled)
                        .put(
                                "dayStartHour",
                                sorting.dayStartHour)
                        .put(
                                "eveningStartHour",
                                sorting.eveningStartHour)
                        .put(
                                "automaticFavoriteCount",
                                sorting.automaticFavoriteCount)
                        .put(
                                "suggestionsEnabled",
                                sorting.suggestionsEnabled)
                        .put(
                                "suggestionIntervalDays",
                                sorting.suggestionIntervalDays);

        JSONObject uiJson =
                new JSONObject()
                        .put(
                                "startBehavior",
                                ui.startBehavior.name())
                        .put(
                                "controlSide",
                                ui.controlSide.name())
                        .put(
                                "themeMode",
                                ui.themeMode.name())
                        .put(
                                "language",
                                language.name());

        JSONArray cardOrderJson =
                new JSONArray();

        for (String id :
                cardOrder) {

            cardOrderJson.put(
                    id);
        }

        JSONObject statisticsDisplayJson =
                new JSONObject()
                        .put(
                                "period",
                                period.name())
                        .put(
                                "topLimit",
                                topLimit)
                        .put(
                                "cardOrder",
                                cardOrderJson);

        return new JSONObject()
                .put(
                        KEY_SORTING,
                        sortingJson)
                .put(
                        KEY_UI,
                        uiJson)
                .put(
                        KEY_STATISTICS_DISPLAY,
                        statisticsDisplayJson);
    }

    static void validate(
            JSONObject backup)
            throws JSONException {

        parse(
                backup);
    }

    static State parse(
            JSONObject backup)
            throws JSONException {

        JSONObject settings =
                backup.getJSONObject(
                        KEY_SETTINGS);

        JSONObject sorting =
                settings.getJSONObject(
                        KEY_SORTING);

        SortingSettingsStore.Mode sortingMode =
                requireEnum(
                        SortingSettingsStore.Mode.class,
                        sorting,
                        "mode",
                        "Ungültiger Sortiermodus im Backup.");

        boolean semiFavorites =
                requireBoolean(
                        sorting,
                        "semiFavorites",
                        "Ungültige Sortiereinstellung im Backup.");

        boolean semiCategories =
                requireBoolean(
                        sorting,
                        "semiCategories",
                        "Ungültige Sortiereinstellung im Backup.");

        boolean semiApps =
                requireBoolean(
                        sorting,
                        "semiApps",
                        "Ungültige Sortiereinstellung im Backup.");

        boolean semiShortcuts =
                requireBoolean(
                        sorting,
                        "semiShortcuts",
                        "Ungültige Sortiereinstellung im Backup.");

        boolean timeProfileEnabled =
                requireBoolean(
                        sorting,
                        "timeProfileEnabled",
                        "Ungültige Sortiereinstellung im Backup.");

        int dayStartHour =
                requireInt(
                        sorting,
                        "dayStartHour",
                        "Ungültige Tageszeit im Backup.");

        int eveningStartHour =
                requireInt(
                        sorting,
                        "eveningStartHour",
                        "Ungültige Tageszeit im Backup.");

        int automaticFavoriteCount =
                requireInt(
                        sorting,
                        "automaticFavoriteCount",
                        "Ungültige Favoritenzahl im Backup.");

        boolean suggestionsEnabled =
                requireBoolean(
                        sorting,
                        "suggestionsEnabled",
                        "Ungültige Vorschlagseinstellung im Backup.");

        int suggestionIntervalDays =
                requireInt(
                        sorting,
                        "suggestionIntervalDays",
                        "Ungültiges Vorschlagsintervall im Backup.");

        if (!isValidHour(
                dayStartHour)
                || !isValidHour(
                        eveningStartHour)
                || dayStartHour
                == eveningStartHour) {

            throw new JSONException(
                    "Ungültige Tageszeit im Backup.");
        }

        if (automaticFavoriteCount < 3
                || automaticFavoriteCount > 30) {

            throw new JSONException(
                    "Ungültige Favoritenzahl im Backup.");
        }

        if (!isValidSuggestionInterval(
                suggestionIntervalDays)) {

            throw new JSONException(
                    "Ungültiges Vorschlagsintervall im Backup.");
        }

        SortingSettingsStore.Settings sortingSettings =
                new SortingSettingsStore.Settings(
                        sortingMode,
                        semiFavorites,
                        semiCategories,
                        semiApps,
                        semiShortcuts,
                        timeProfileEnabled,
                        dayStartHour,
                        eveningStartHour,
                        automaticFavoriteCount,
                        false,
                        suggestionsEnabled,
                        suggestionIntervalDays);

        JSONObject ui =
                settings.getJSONObject(
                        KEY_UI);

        UiSettingsStore.StartBehavior startBehavior =
                requireEnum(
                        UiSettingsStore.StartBehavior.class,
                        ui,
                        "startBehavior",
                        "Ungültiges Favoriten-Startverhalten im Backup.");

        UiSettingsStore.ControlSide controlSide =
                requireEnum(
                        UiSettingsStore.ControlSide.class,
                        ui,
                        "controlSide",
                        "Ungültige Bedienseite im Backup.");

        UiSettingsStore.ThemeMode themeMode =
                requireEnum(
                        UiSettingsStore.ThemeMode.class,
                        ui,
                        "themeMode",
                        "Ungültiges Design im Backup.");

        UiLocaleController.LanguageMode language =
                requireEnum(
                        UiLocaleController.LanguageMode.class,
                        ui,
                        "language",
                        "Ungültige Sprache im Backup.");

        UiSettingsStore.Settings uiSettings =
                new UiSettingsStore.Settings(
                        startBehavior,
                        controlSide,
                        themeMode);

        JSONObject statisticsDisplay =
                settings.getJSONObject(
                        KEY_STATISTICS_DISPLAY);

        UsageStatisticsStore.Period period =
                requireEnum(
                        UsageStatisticsStore.Period.class,
                        statisticsDisplay,
                        "period",
                        "Ungültiger Statistikzeitraum im Backup.");

        int topLimit =
                requireInt(
                        statisticsDisplay,
                        "topLimit",
                        "Ungültige Statistikanzahl im Backup.");

        if (!isValidStatisticsTopLimit(
                topLimit)) {

            throw new JSONException(
                    "Ungültige Statistikanzahl im Backup.");
        }

        JSONArray cardOrderJson =
                statisticsDisplay.getJSONArray(
                        "cardOrder");

        List<String> cardOrder =
                new ArrayList<>();

        Set<String> seen =
                new HashSet<>();

        for (int i = 0;
             i < cardOrderJson.length();
             i++) {

            String id =
                    cardOrderJson.getString(
                            i);

            if (!isValidStatisticsCardId(
                    id)
                    || !seen.add(
                            id)) {

                throw new JSONException(
                        "Ungültige Statistikkarten-Reihenfolge im Backup.");
            }

            cardOrder.add(
                    id);
        }

        return new State(
                sortingSettings,
                uiSettings,
                language,
                period,
                topLimit,
                cardOrder);
    }

    static boolean restorePreferences(
            Context context,
            State state) {

        SharedPreferences sortingPrefs =
                context.getSharedPreferences(
                        SORTING_PREFS,
                        Context.MODE_PRIVATE);

        SharedPreferences uiPrefs =
                context.getSharedPreferences(
                        UI_PREFS,
                        Context.MODE_PRIVATE);

        SharedPreferences statisticsDisplay =
                context.getSharedPreferences(
                        STATISTICS_DISPLAY_PREFS,
                        Context.MODE_PRIVATE);

        SortingSettingsStore.Settings sorting =
                state.sorting;

        boolean sortingSaved =
                sortingPrefs.edit()
                        .clear()
                        .putString(
                                "mode",
                                sorting.mode.name())
                        .putBoolean(
                                "semi_favorites",
                                sorting.semiFavorites)
                        .putBoolean(
                                "semi_categories",
                                sorting.semiCategories)
                        .putBoolean(
                                "semi_apps",
                                sorting.semiApps)
                        .putBoolean(
                                "semi_shortcuts",
                                sorting.semiShortcuts)
                        .putBoolean(
                                "time_profile_enabled",
                                sorting.timeProfileEnabled)
                        .putInt(
                                "day_start_hour",
                                sorting.dayStartHour)
                        .putInt(
                                "evening_start_hour",
                                sorting.eveningStartHour)
                        .putInt(
                                "automatic_favorite_count",
                                sorting.automaticFavoriteCount)
                        .putBoolean(
                                "suggestions_enabled",
                                sorting.suggestionsEnabled)
                        .putInt(
                                "suggestion_interval_days",
                                sorting.suggestionIntervalDays)
                        .commit();

        boolean uiSaved =
                uiPrefs.edit()
                        .clear()
                        .putString(
                                "start_behavior",
                                state.ui.startBehavior.name())
                        .putString(
                                "control_side",
                                state.ui.controlSide.name())
                        .putString(
                                "theme_mode",
                                state.ui.themeMode.name())
                        .commit();

        boolean statisticsDisplaySaved =
                statisticsDisplay.edit()
                        .clear()
                        .putString(
                                KEY_STATISTICS_PERIOD,
                                state.statisticsPeriod.name())
                        .putInt(
                                KEY_STATISTICS_TOP_LIMIT,
                                state.statisticsTopLimit)
                        .putString(
                                KEY_STATISTICS_CARD_ORDER,
                                String.join(
                                        ",",
                                        state.statisticsCardOrder))
                        .commit();

        return sortingSaved
                && uiSaved
                && statisticsDisplaySaved;
    }

    private static UsageStatisticsStore.Period
            parseStoredStatisticsPeriod(
                    String value) {

        try {
            return UsageStatisticsStore.Period.valueOf(
                    value);

        } catch (IllegalArgumentException
                 | NullPointerException exception) {

            return UsageStatisticsStore.Period.ONE_MONTH;
        }
    }

    private static List<String> parseStoredCardOrder(
            String value) {

        List<String> result =
                new ArrayList<>();

        Set<String> seen =
                new HashSet<>();

        if (value == null
                || value.isBlank()) {

            return result;
        }

        for (String part :
                value.split(",")) {

            String id =
                    part.trim();

            if (isValidStatisticsCardId(
                    id)
                    && seen.add(
                            id)) {

                result.add(
                        id);
            }
        }

        return result;
    }

    private static boolean requireBoolean(
            JSONObject object,
            String key,
            String errorMessage)
            throws JSONException {

        Object value =
                object.get(
                        key);

        if (!(value instanceof Boolean)) {
            throw new JSONException(
                    errorMessage);
        }

        return (Boolean) value;
    }

    private static int requireInt(
            JSONObject object,
            String key,
            String errorMessage)
            throws JSONException {

        Object value =
                object.get(
                        key);

        if (!(value instanceof Number)) {
            throw new JSONException(
                    errorMessage);
        }

        Number number =
                (Number) value;

        int result =
                number.intValue();

        if (number.doubleValue()
                != result) {

            throw new JSONException(
                    errorMessage);
        }

        return result;
    }

    private static <T extends Enum<T>> T requireEnum(
            Class<T> type,
            JSONObject object,
            String key,
            String errorMessage)
            throws JSONException {

        String value =
                object.getString(
                        key);

        if (value.isEmpty()
                || !value.equals(
                        value.trim())) {

            throw new JSONException(
                    errorMessage);
        }

        try {
            return Enum.valueOf(
                    type,
                    value);

        } catch (IllegalArgumentException exception) {

            throw new JSONException(
                    errorMessage);
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

    private static boolean isValidStatisticsTopLimit(
            int value) {

        return value == 5
                || value == 10
                || value == 25
                || value == 50
                || value == STATISTICS_TOP_LIMIT_ALL;
    }

    private static boolean isValidStatisticsCardId(
            String id) {

        return StatisticsAdapter.CARD_TOP_SHORTCUTS.equals(
                id)
                || StatisticsAdapter.CARD_TOP_CATEGORIES.equals(
                        id)
                || StatisticsAdapter.CARD_TOP_APPS.equals(
                        id)
                || StatisticsAdapter.CARD_CUSTOM_SHORTCUTS.equals(
                        id)
                || StatisticsAdapter.CARD_UNUSED_APPS.equals(
                        id)
                || StatisticsAdapter.CARD_TOTAL.equals(
                        id);
    }
}
