package de.pritcloud.appstow;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

final class UsageStatisticsStore {

    private static final String PREFS_NAME =
            "usage_statistics";

    private static final String DAY_PREFIX =
            "day_";

    private static final String KEY_APPS =
            "apps";

    private static final String KEY_SHORTCUTS =
            "shortcuts";

    private static final String KEY_CATEGORIES =
            "categories";

    enum Period {
        ONE_MONTH(1),
        THREE_MONTHS(3),
        SIX_MONTHS(6),
        ONE_YEAR(12);

        final int months;

        Period(int months) {
            this.months = months;
        }
    }

    static final class Snapshot {

        private final Map<String, Integer> appCounts;
        private final Map<String, Integer> shortcutCounts;
        private final Map<String, Integer> categoryCounts;

        Snapshot(
                Map<String, Integer> appCounts,
                Map<String, Integer> shortcutCounts,
                Map<String, Integer> categoryCounts) {

            this.appCounts =
                    Collections.unmodifiableMap(
                            new HashMap<>(appCounts));

            this.shortcutCounts =
                    Collections.unmodifiableMap(
                            new HashMap<>(shortcutCounts));

            this.categoryCounts =
                    Collections.unmodifiableMap(
                            new HashMap<>(categoryCounts));
        }

        Map<String, Integer> getAppCounts() {
            return appCounts;
        }

        Map<String, Integer> getShortcutCounts() {
            return shortcutCounts;
        }

        Map<String, Integer> getCategoryCounts() {
            return categoryCounts;
        }

        int getTotalLaunches() {
            int total = 0;

            for (int count : appCounts.values()) {
                total += count;
            }

            for (int count : shortcutCounts.values()) {
                total += count;
            }

            return total;
        }
    }

    private final SharedPreferences preferences;
    private final Clock clock;

    UsageStatisticsStore(Context context) {
        this(
                context,
                Clock.systemDefaultZone());
    }

    UsageStatisticsStore(
            Context context,
            Clock clock) {

        preferences =
                context.getSharedPreferences(
                        PREFS_NAME,
                        Context.MODE_PRIVATE);

        this.clock = clock;
    }

    void recordAppLaunch(
            String packageName,
            String categoryId) {

        recordLaunch(
                KEY_APPS,
                packageName,
                categoryId);
    }

    void recordShortcutLaunch(
            String shortcutId,
            String categoryId) {

        recordLaunch(
                KEY_SHORTCUTS,
                shortcutId,
                categoryId);
    }

    Snapshot getSnapshot(Period period) {
        LocalDate today =
                LocalDate.now(clock);

        pruneOldDays(today);

        LocalDate startDate =
                today.minusMonths(
                        period.months);

        Map<String, Integer> apps =
                new HashMap<>();

        Map<String, Integer> shortcuts =
                new HashMap<>();

        Map<String, Integer> categories =
                new HashMap<>();

        for (Map.Entry<String, ?>
                entry :
                preferences.getAll()
                        .entrySet()) {

            LocalDate date =
                    parseDateKey(
                            entry.getKey());

            if (date == null
                    || date.isBefore(startDate)
                    || date.isAfter(today)
                    || !(entry.getValue()
                    instanceof String)) {

                continue;
            }

            JSONObject day =
                    parseDay(
                            (String) entry.getValue());

            mergeCounts(
                    apps,
                    day.optJSONObject(
                            KEY_APPS));

            mergeCounts(
                    shortcuts,
                    day.optJSONObject(
                            KEY_SHORTCUTS));

            mergeCounts(
                    categories,
                    day.optJSONObject(
                            KEY_CATEGORIES));
        }

        return new Snapshot(
                apps,
                shortcuts,
                categories);
    }

    void clear() {
        preferences.edit()
                .clear()
                .apply();
    }

    private void recordLaunch(
            String targetGroup,
            String targetId,
            String categoryId) {

        if (targetId == null
                || targetId.isBlank()) {

            return;
        }

        LocalDate today =
                LocalDate.now(clock);

        pruneOldDays(today);

        String key =
                dayKey(today);

        JSONObject day =
                parseDay(
                        preferences.getString(
                                key,
                                null));

        increment(
                day,
                targetGroup,
                targetId);

        if (categoryId != null
                && !categoryId.isBlank()) {

            increment(
                    day,
                    KEY_CATEGORIES,
                    categoryId);
        }

        preferences.edit()
                .putString(
                        key,
                        day.toString())
                .apply();
    }

    private static void increment(
            JSONObject day,
            String groupName,
            String id) {

        JSONObject group =
                day.optJSONObject(
                        groupName);

        if (group == null) {
            group =
                    new JSONObject();

            try {
                day.put(
                        groupName,
                        group);
            } catch (JSONException exception) {
                throw new IllegalStateException(
                        exception);
            }
        }

        int current =
                group.optInt(
                        id,
                        0);

        try {
            group.put(
                    id,
                    current + 1);
        } catch (JSONException exception) {
            throw new IllegalStateException(
                    exception);
        }
    }

    private void pruneOldDays(
            LocalDate today) {

        LocalDate oldestAllowed =
                today.minusYears(1);

        SharedPreferences.Editor editor =
                null;

        for (String key :
                preferences.getAll()
                        .keySet()) {

            LocalDate date =
                    parseDateKey(key);

            if (date == null
                    || !date.isBefore(
                            oldestAllowed)) {

                continue;
            }

            if (editor == null) {
                editor =
                        preferences.edit();
            }

            editor.remove(key);
        }

        if (editor != null) {
            editor.apply();
        }
    }

    private static String dayKey(
            LocalDate date) {

        return DAY_PREFIX
                + date.toEpochDay();
    }

    private static LocalDate parseDateKey(
            String key) {

        if (key == null
                || !key.startsWith(
                        DAY_PREFIX)) {

            return null;
        }

        try {
            long epochDay =
                    Long.parseLong(
                            key.substring(
                                    DAY_PREFIX.length()));

            return LocalDate.ofEpochDay(
                    epochDay);

        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static JSONObject parseDay(
            String value) {

        if (value == null
                || value.isBlank()) {

            return new JSONObject();
        }

        try {
            return new JSONObject(
                    value);

        } catch (JSONException exception) {
            return new JSONObject();
        }
    }

    private static void mergeCounts(
            Map<String, Integer> target,
            JSONObject source) {

        if (source == null) {
            return;
        }

        java.util.Iterator<String> keys =
                source.keys();

        while (keys.hasNext()) {
            String key =
                    keys.next();

            int count =
                    source.optInt(
                            key,
                            0);

            if (count <= 0) {
                continue;
            }

            target.merge(
                    key,
                    count,
                    Integer::sum);
        }
    }
}
