package de.pritcloud.appstow;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

    private static final String KEY_SECTIONS =
            "sections";

    private static final String KEY_HOURS =
            "hours";

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

    enum TimeProfile {
        DAY,
        EVENING
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

    static final class SortingSnapshot {

        private final Snapshot overallWeighted;
        private final Snapshot profileWeighted;
        private final Snapshot profileLaunches;

        private final Map<String, Integer>
                overallSectionCounts;

        private final Map<String, Integer>
                profileSectionCounts;

        private final Map<String, Integer>
                profileSectionLaunches;

        SortingSnapshot(
                Snapshot overallWeighted,
                Snapshot profileWeighted,
                Snapshot profileLaunches,
                Map<String, Integer> overallSectionCounts,
                Map<String, Integer> profileSectionCounts,
                Map<String, Integer> profileSectionLaunches) {

            this.overallWeighted =
                    overallWeighted;

            this.profileWeighted =
                    profileWeighted;

            this.profileLaunches =
                    profileLaunches;

            this.overallSectionCounts =
                    Collections.unmodifiableMap(
                            new HashMap<>(
                                    overallSectionCounts));

            this.profileSectionCounts =
                    Collections.unmodifiableMap(
                            new HashMap<>(
                                    profileSectionCounts));

            this.profileSectionLaunches =
                    Collections.unmodifiableMap(
                            new HashMap<>(
                                    profileSectionLaunches));
        }

        Snapshot getOverallWeighted() {
            return overallWeighted;
        }

        Snapshot getProfileWeighted() {
            return profileWeighted;
        }

        Snapshot getProfileLaunches() {
            return profileLaunches;
        }

        Map<String, Integer> getOverallSectionCounts() {
            return overallSectionCounts;
        }

        Map<String, Integer> getProfileSectionCounts() {
            return profileSectionCounts;
        }

        Map<String, Integer> getProfileSectionLaunches() {
            return profileSectionLaunches;
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

        recordAppLaunch(
                packageName,
                categoryId,
                singleSectionSet(
                        categorySectionId(
                                categoryId)));
    }

    void recordAppLaunch(
            String packageName,
            String categoryId,
            String sectionId) {

        recordAppLaunch(
                packageName,
                categoryId,
                singleSectionSet(
                        sectionId));
    }

    void recordAppLaunch(
            String packageName,
            String categoryId,
            Set<String> sectionIds) {

        recordLaunch(
                KEY_APPS,
                packageName,
                categoryId,
                sectionIds);
    }

    void recordShortcutLaunch(
            String shortcutId,
            String categoryId) {

        recordShortcutLaunch(
                shortcutId,
                categoryId,
                singleSectionSet(
                        categorySectionId(
                                categoryId)));
    }

    void recordShortcutLaunch(
            String shortcutId,
            String categoryId,
            String sectionId) {

        recordShortcutLaunch(
                shortcutId,
                categoryId,
                singleSectionSet(
                        sectionId));
    }

    void recordShortcutLaunch(
            String shortcutId,
            String categoryId,
            Set<String> sectionIds) {

        recordLaunch(
                KEY_SHORTCUTS,
                shortcutId,
                categoryId,
                sectionIds);
    }

    private static Set<String> singleSectionSet(
            String sectionId) {

        Set<String> result =
                new HashSet<>();

        if (sectionId != null
                && !sectionId.isBlank()) {

            result.add(
                    sectionId);
        }

        return result;
    }

    private static String categorySectionId(
            String categoryId) {

        if (categoryId == null
                || categoryId.isBlank()) {

            return null;
        }

        return "category:"
                + categoryId;
    }

    void recordSectionAccess(
            String sectionId) {

        Set<String> sectionIds =
                singleSectionSet(
                        sectionId);

        if (sectionIds.isEmpty()) {
            return;
        }

        ZonedDateTime now =
                ZonedDateTime.now(
                        clock);

        LocalDate today =
                now.toLocalDate();

        int hour =
                now.getHour();

        pruneOldDays(
                today);

        String key =
                dayKey(
                        today);

        JSONObject day =
                parseDay(
                        preferences.getString(
                                key,
                                null));

        incrementSections(
                day,
                sectionIds);

        JSONObject hours =
                getOrCreateObject(
                        day,
                        KEY_HOURS);

        JSONObject hourly =
                getOrCreateObject(
                        hours,
                        Integer.toString(
                                hour));

        incrementSections(
                hourly,
                sectionIds);

        preferences.edit()
                .putString(
                        key,
                        day.toString())
                .apply();
    }

    TimeProfile getCurrentTimeProfile(
            int dayStartHour,
            int eveningStartHour) {

        validateProfileHours(
                dayStartHour,
                eveningStartHour);

        return resolveTimeProfile(
                ZonedDateTime.now(clock)
                        .getHour(),
                dayStartHour,
                eveningStartHour);
    }

    long millisUntilNextTimeProfileBoundary(
            int dayStartHour,
            int eveningStartHour) {

        validateProfileHours(
                dayStartHour,
                eveningStartHour);

        ZonedDateTime now =
                ZonedDateTime.now(clock);

        ZonedDateTime nextDay =
                nextBoundary(
                        now,
                        dayStartHour);

        ZonedDateTime nextEvening =
                nextBoundary(
                        now,
                        eveningStartHour);

        ZonedDateTime next =
                nextDay.isBefore(
                        nextEvening)
                        ? nextDay
                        : nextEvening;

        return Math.max(
                1L,
                Duration.between(
                                now,
                                next)
                        .toMillis());
    }

    private static ZonedDateTime nextBoundary(
            ZonedDateTime now,
            int hour) {

        ZonedDateTime candidate =
                now.withHour(hour)
                        .withMinute(0)
                        .withSecond(0)
                        .withNano(0);

        if (!candidate.isAfter(now)) {
            candidate =
                    candidate.plusDays(1);
        }

        return candidate;
    }

    Snapshot getSnapshot(
            Period period) {

        return getSnapshotInternal(
                period,
                null,
                0,
                0);
    }

    Snapshot getSnapshot(
            Period period,
            TimeProfile profile,
            int dayStartHour,
            int eveningStartHour) {

        if (profile == null) {
            throw new IllegalArgumentException(
                    "Time profile is required.");
        }

        validateProfileHours(
                dayStartHour,
                eveningStartHour);

        return getSnapshotInternal(
                period,
                profile,
                dayStartHour,
                eveningStartHour);
    }

    private Snapshot getSnapshotInternal(
            Period period,
            TimeProfile profile,
            int dayStartHour,
            int eveningStartHour) {

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

            if (profile == null) {
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

                continue;
            }

            JSONObject hours =
                    day.optJSONObject(
                            KEY_HOURS);

            if (hours == null) {
                continue;
            }

            for (int hour = 0;
                 hour < 24;
                 hour++) {

                if (resolveTimeProfile(
                        hour,
                        dayStartHour,
                        eveningStartHour)
                        != profile) {

                    continue;
                }

                JSONObject hourly =
                        hours.optJSONObject(
                                Integer.toString(
                                        hour));

                if (hourly == null) {
                    continue;
                }

                mergeCounts(
                        apps,
                        hourly.optJSONObject(
                                KEY_APPS));

                mergeCounts(
                        shortcuts,
                        hourly.optJSONObject(
                                KEY_SHORTCUTS));

                mergeCounts(
                        categories,
                        hourly.optJSONObject(
                                KEY_CATEGORIES));
            }
        }

        return new Snapshot(
                apps,
                shortcuts,
                categories);
    }

    SortingSnapshot getSortingSnapshot(
            TimeProfile profile,
            int dayStartHour,
            int eveningStartHour) {

        if (profile == null) {
            throw new IllegalArgumentException(
                    "Time profile is required.");
        }

        validateProfileHours(
                dayStartHour,
                eveningStartHour);

        LocalDate today =
                LocalDate.now(
                        clock);

        pruneOldDays(
                today);

        Map<String, Integer> overallApps =
                new HashMap<>();

        Map<String, Integer> overallShortcuts =
                new HashMap<>();

        Map<String, Integer> overallCategories =
                new HashMap<>();

        Map<String, Integer> overallSections =
                new HashMap<>();

        Map<String, Integer> profileApps =
                new HashMap<>();

        Map<String, Integer> profileShortcuts =
                new HashMap<>();

        Map<String, Integer> profileCategories =
                new HashMap<>();

        Map<String, Integer> profileSections =
                new HashMap<>();

        Map<String, Integer> profileAppLaunches =
                new HashMap<>();

        Map<String, Integer> profileShortcutLaunches =
                new HashMap<>();

        Map<String, Integer> profileCategoryLaunches =
                new HashMap<>();

        Map<String, Integer> profileSectionLaunches =
                new HashMap<>();

        for (Map.Entry<String, ?>
                entry :
                preferences.getAll()
                        .entrySet()) {

            LocalDate date =
                    parseDateKey(
                            entry.getKey());

            if (date == null
                    || date.isAfter(today)
                    || !(entry.getValue()
                    instanceof String)) {

                continue;
            }

            long ageDays =
                    ChronoUnit.DAYS.between(
                            date,
                            today);

            int weight =
                    getSortingWeight(
                            ageDays);

            if (weight == 0) {
                continue;
            }

            JSONObject day =
                    parseDay(
                            (String) entry.getValue());

            mergeCountsWeighted(
                    overallApps,
                    day.optJSONObject(
                            KEY_APPS),
                    weight);

            mergeCountsWeighted(
                    overallShortcuts,
                    day.optJSONObject(
                            KEY_SHORTCUTS),
                    weight);

            mergeCountsWeighted(
                    overallCategories,
                    day.optJSONObject(
                            KEY_CATEGORIES),
                    weight);

            mergeCountsWeighted(
                    overallSections,
                    day.optJSONObject(
                            KEY_SECTIONS),
                    weight);

            JSONObject hours =
                    day.optJSONObject(
                            KEY_HOURS);

            if (hours == null) {
                continue;
            }

            for (int hour = 0;
                 hour < 24;
                 hour++) {

                if (resolveTimeProfile(
                        hour,
                        dayStartHour,
                        eveningStartHour)
                        != profile) {

                    continue;
                }

                JSONObject hourly =
                        hours.optJSONObject(
                                Integer.toString(
                                        hour));

                if (hourly == null) {
                    continue;
                }

                JSONObject apps =
                        hourly.optJSONObject(
                                KEY_APPS);

                JSONObject shortcuts =
                        hourly.optJSONObject(
                                KEY_SHORTCUTS);

                JSONObject categories =
                        hourly.optJSONObject(
                                KEY_CATEGORIES);

                JSONObject sections =
                        hourly.optJSONObject(
                                KEY_SECTIONS);

                mergeCountsWeighted(
                        profileApps,
                        apps,
                        weight);

                mergeCountsWeighted(
                        profileShortcuts,
                        shortcuts,
                        weight);

                mergeCountsWeighted(
                        profileCategories,
                        categories,
                        weight);

                mergeCountsWeighted(
                        profileSections,
                        sections,
                        weight);

                mergeCounts(
                        profileAppLaunches,
                        apps);

                mergeCounts(
                        profileShortcutLaunches,
                        shortcuts);

                mergeCounts(
                        profileCategoryLaunches,
                        categories);

                mergeCounts(
                        profileSectionLaunches,
                        sections);
            }
        }

        return new SortingSnapshot(
                new Snapshot(
                        overallApps,
                        overallShortcuts,
                        overallCategories),
                new Snapshot(
                        profileApps,
                        profileShortcuts,
                        profileCategories),
                new Snapshot(
                        profileAppLaunches,
                        profileShortcutLaunches,
                        profileCategoryLaunches),
                overallSections,
                profileSections,
                profileSectionLaunches);
    }

    private static int getSortingWeight(
            long ageDays) {

        if (ageDays < 0
                || ageDays >= 90) {

            return 0;
        }

        if (ageDays < 7) {
            return 4;
        }

        if (ageDays < 30) {
            return 2;
        }

        return 1;
    }

    private static void mergeCountsWeighted(
            Map<String, Integer> target,
            JSONObject source,
            int weight) {

        if (source == null
                || weight <= 0) {

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
                    count * weight,
                    Integer::sum);
        }
    }

    void clear() {
        preferences.edit()
                .clear()
                .apply();
    }

    private void recordLaunch(
            String targetGroup,
            String targetId,
            String categoryId,
            Set<String> sectionIds) {

        if (targetId == null
                || targetId.isBlank()) {

            return;
        }

        ZonedDateTime now =
                ZonedDateTime.now(
                        clock);

        LocalDate today =
                now.toLocalDate();

        int hour =
                now.getHour();

        pruneOldDays(
                today);

        String key =
                dayKey(
                        today);

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

        incrementSections(
                day,
                sectionIds);

        JSONObject hours =
                getOrCreateObject(
                        day,
                        KEY_HOURS);

        JSONObject hourly =
                getOrCreateObject(
                        hours,
                        Integer.toString(
                                hour));

        increment(
                hourly,
                targetGroup,
                targetId);

        if (categoryId != null
                && !categoryId.isBlank()) {

            increment(
                    hourly,
                    KEY_CATEGORIES,
                    categoryId);
        }

        incrementSections(
                hourly,
                sectionIds);

        preferences.edit()
                .putString(
                        key,
                        day.toString())
                .apply();
    }

    private static void incrementSections(
            JSONObject target,
            Set<String> sectionIds) {

        if (target == null
                || sectionIds == null
                || sectionIds.isEmpty()) {

            return;
        }

        Set<String> unique =
                new HashSet<>();

        for (String sectionId :
                sectionIds) {

            if (sectionId == null
                    || sectionId.isBlank()
                    || !unique.add(
                            sectionId)) {

                continue;
            }

            increment(
                    target,
                    KEY_SECTIONS,
                    sectionId);
        }
    }

    static TimeProfile resolveTimeProfile(
            int hour,
            int dayStartHour,
            int eveningStartHour) {

        if (!isValidHour(hour)) {
            throw new IllegalArgumentException(
                    "Invalid current hour.");
        }

        validateProfileHours(
                dayStartHour,
                eveningStartHour);

        boolean day;

        if (dayStartHour < eveningStartHour) {
            day =
                    hour >= dayStartHour
                            && hour < eveningStartHour;
        } else {
            day =
                    hour >= dayStartHour
                            || hour < eveningStartHour;
        }

        return day
                ? TimeProfile.DAY
                : TimeProfile.EVENING;
    }

    private static void validateProfileHours(
            int dayStartHour,
            int eveningStartHour) {

        if (!isValidHour(dayStartHour)
                || !isValidHour(eveningStartHour)
                || dayStartHour
                == eveningStartHour) {

            throw new IllegalArgumentException(
                    "Invalid time profile hours.");
        }
    }

    private static boolean isValidHour(
            int hour) {

        return hour >= 0
                && hour <= 23;
    }

    private static JSONObject getOrCreateObject(
            JSONObject parent,
            String name) {

        JSONObject object =
                parent.optJSONObject(
                        name);

        if (object != null) {
            return object;
        }

        object =
                new JSONObject();

        try {
            parent.put(
                    name,
                    object);
        } catch (JSONException exception) {
            throw new IllegalStateException(
                    exception);
        }

        return object;
    }

    private static void increment(
            JSONObject parent,
            String groupName,
            String id) {

        JSONObject group =
                getOrCreateObject(
                        parent,
                        groupName);

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
                    parseDateKey(
                            key);

            if (date == null
                    || !date.isBefore(
                            oldestAllowed)) {

                continue;
            }

            if (editor == null) {
                editor =
                        preferences.edit();
            }

            editor.remove(
                    key);
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
