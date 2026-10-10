package de.pritcloud.appstow;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

final class AutomaticSortingSnapshotStore {

    static final long DAY_MILLIS =
            24L * 60L * 60L * 1000L;

    static final long REFRESH_INTERVAL_MILLIS =
            7L * DAY_MILLIS;

    private static final String PREFIX =
            "automatic_snapshot_v1_";

    private static final String[] SCOPES = {
            "global", "day", "evening"
    };

    private final SharedPreferences preferences;

    AutomaticSortingSnapshotStore(Context context) {
        preferences = context.getSharedPreferences(
                "sorting_settings",
                Context.MODE_PRIVATE);
    }

    UsageStatisticsStore.SortingSnapshot getOrCreate(
            UsageStatisticsStore.TimeProfile profile,
            UsageStatisticsStore.SortingSnapshot current,
            int intervalDays) {

        return getOrCreateAt(
                profile,
                current,
                System.currentTimeMillis(),
                intervalDays);
    }

    UsageStatisticsStore.SortingSnapshot getOrCreateAt(
            UsageStatisticsStore.TimeProfile profile,
            UsageStatisticsStore.SortingSnapshot current,
            long nowMillis) {

        return getOrCreateAt(profile, current, nowMillis, 7);
    }

    UsageStatisticsStore.SortingSnapshot getOrCreateAt(
            UsageStatisticsStore.TimeProfile profile,
            UsageStatisticsStore.SortingSnapshot current,
            long nowMillis,
            int intervalDays) {

        if (intervalDays != 7 && intervalDays != 14
                && intervalDays != 30 && intervalDays != 60
                && intervalDays != 90) {
            throw new IllegalArgumentException(
                    "Unsupported sorting interval.");
        }

        if (current == null) {
            throw new IllegalArgumentException(
                    "Sorting snapshot is required.");
        }

        String scope = profile == null
                ? "global"
                : profile.name().toLowerCase(Locale.ROOT);

        String key = PREFIX + scope;
        String stored = preferences.getString(key, null);

        if (stored != null) {
            try {
                JSONObject json = new JSONObject(stored);
                long savedAt = json.getLong("savedAt");

                if (json.getInt("version") == 1
                        && savedAt > 0L
                        && nowMillis >= savedAt
                        && nowMillis - savedAt
                        < intervalDays * DAY_MILLIS) {

                    UsageStatisticsStore.SortingSnapshot frozen =
                            decode(json);

                    if (hasLaunches(frozen)
                            || !hasLaunches(current)) {
                        return frozen;
                    }
                }

            } catch (JSONException ignored) {
                // Ungueltigen Snapshot neu erstellen.
            }
        }

        try {
            JSONObject json = encode(current, nowMillis);
            preferences.edit()
                    .putString(key, json.toString())
                    .apply();

        } catch (JSONException ignored) {
            // Aktuelle Daten bleiben nutzbar.
        }

        return current;
    }

    static void invalidate(
            SharedPreferences.Editor editor) {

        for (String scope : SCOPES) {
            editor.remove(PREFIX + scope);
        }
    }

    private static boolean hasLaunches(
            UsageStatisticsStore.SortingSnapshot snapshot) {

        for (Integer count : snapshot.getOverallWeighted()
                .getAppCounts().values()) {
            if (count != null && count > 0) {
                return true;
            }
        }

        for (Integer count : snapshot.getOverallWeighted()
                .getShortcutCounts().values()) {
            if (count != null && count > 0) {
                return true;
            }
        }

        return false;
    }

    private static JSONObject encode(
            UsageStatisticsStore.SortingSnapshot snapshot,
            long nowMillis) throws JSONException {

        return new JSONObject()
                .put("version", 1)
                .put("savedAt", nowMillis)
                .put("overall",
                        encodeCounts(snapshot.getOverallWeighted()))
                .put("profile",
                        encodeCounts(snapshot.getProfileWeighted()))
                .put("launches",
                        encodeCounts(snapshot.getProfileLaunches()))
                .put("overallSections",
                        new JSONObject(snapshot.getOverallSectionCounts()))
                .put("profileSections",
                        new JSONObject(snapshot.getProfileSectionCounts()))
                .put("sectionLaunches",
                        new JSONObject(snapshot.getProfileSectionLaunches()));
    }

    private static JSONObject encodeCounts(
            UsageStatisticsStore.Snapshot counts)
            throws JSONException {

        return new JSONObject()
                .put("apps",
                        new JSONObject(counts.getAppCounts()))
                .put("shortcuts",
                        new JSONObject(counts.getShortcutCounts()))
                .put("categories",
                        new JSONObject(counts.getCategoryCounts()));
    }

    private static UsageStatisticsStore.SortingSnapshot decode(
            JSONObject json) throws JSONException {

        return new UsageStatisticsStore.SortingSnapshot(
                decodeCounts(json.getJSONObject("overall")),
                decodeCounts(json.getJSONObject("profile")),
                decodeCounts(json.getJSONObject("launches")),
                decodeMap(json.getJSONObject("overallSections")),
                decodeMap(json.getJSONObject("profileSections")),
                decodeMap(json.getJSONObject("sectionLaunches")));
    }

    private static UsageStatisticsStore.Snapshot decodeCounts(
            JSONObject json) throws JSONException {

        return new UsageStatisticsStore.Snapshot(
                decodeMap(json.getJSONObject("apps")),
                decodeMap(json.getJSONObject("shortcuts")),
                decodeMap(json.getJSONObject("categories")));
    }

    private static Map<String, Integer> decodeMap(
            JSONObject json) throws JSONException {

        Map<String, Integer> result = new HashMap<>();
        Iterator<String> keys = json.keys();

        while (keys.hasNext()) {
            String key = keys.next();
            Object value = json.get(key);

            if (!(value instanceof Number)) {
                throw new JSONException("Invalid sorting score.");
            }

            long number = ((Number) value).longValue();

            if (number < 0 || number > Integer.MAX_VALUE) {
                throw new JSONException("Invalid sorting range.");
            }

            result.put(key, (int) number);
        }

        return result;
    }
}
