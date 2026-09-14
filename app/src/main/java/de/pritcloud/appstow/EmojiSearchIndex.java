package de.pritcloud.appstow;

import android.content.Context;
import android.text.TextPaint;
import android.util.JsonReader;
import android.util.Log;

import androidx.core.graphics.PaintCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class EmojiSearchIndex {

    private static final String TAG =
            "EmojiSearchIndex";

    private static final int MAX_RESULTS =
            120;

    private static final Map<String, Boolean>
            RENDER_CACHE =
            new HashMap<>();

    private static List<Entry> cachedEntries;

    private EmojiSearchIndex() {
    }

    static List<Result> search(
            Context context,
            String query) {

        String normalizedQuery =
                normalizeForSearch(
                        query);

        if (normalizedQuery.isEmpty()) {
            return Collections.emptyList();
        }

        boolean german =
                "de".equals(
                        context.getResources()
                                .getConfiguration()
                                .getLocales()
                                .get(0)
                                .getLanguage());

        List<ScoredEntry> candidates =
                new ArrayList<>();

        for (Entry entry : getEntries(context)) {
            if (!matchesTerms(
                    entry.terms,
                    normalizedQuery)) {

                continue;
            }

            candidates.add(
                    new ScoredEntry(
                            entry,
                            score(
                                    entry,
                                    normalizedQuery,
                                    german)));
        }

        Collections.sort(
                candidates,
                (left, right) -> {
                    int scoreCompare =
                            Integer.compare(
                                    left.score,
                                    right.score);

                    if (scoreCompare != 0) {
                        return scoreCompare;
                    }

                    return Integer.compare(
                            left.entry.order,
                            right.entry.order);
                });

        List<Result> results =
                new ArrayList<>();

        for (ScoredEntry candidate
                : candidates) {

            Entry entry =
                    candidate.entry;

            if (!isRenderable(
                    entry.emoji)) {

                continue;
            }

            String label =
                    german
                            ? firstNonEmpty(
                                    entry.germanLabel,
                                    entry.englishLabel,
                                    entry.emoji)
                            : firstNonEmpty(
                                    entry.englishLabel,
                                    entry.germanLabel,
                                    entry.emoji);

            results.add(
                    new Result(
                            entry.emoji,
                            label));

            if (results.size()
                    >= MAX_RESULTS) {

                break;
            }
        }

        return results;
    }

    static String normalizeForSearch(
            String value) {

        if (value == null) {
            return "";
        }

        String normalized =
                Normalizer.normalize(
                        value.toLowerCase(
                                        Locale.ROOT)
                                .replace(
                                        "ß",
                                        "ss"),
                        Normalizer.Form.NFKD);

        StringBuilder result =
                new StringBuilder();

        boolean previousSpace =
                true;

        for (int offset = 0;
             offset < normalized.length();) {

            int codePoint =
                    normalized.codePointAt(
                            offset);

            offset +=
                    Character.charCount(
                            codePoint);

            int type =
                    Character.getType(
                            codePoint);

            if (type
                    == Character.NON_SPACING_MARK
                    || type
                    == Character.COMBINING_SPACING_MARK
                    || type
                    == Character.ENCLOSING_MARK) {

                continue;
            }

            if (Character.isLetterOrDigit(
                    codePoint)) {

                result.appendCodePoint(
                        codePoint);

                previousSpace =
                        false;

            } else if (!previousSpace) {

                result.append(
                        " ");

                previousSpace =
                        true;
            }
        }

        return result.toString()
                .trim();
    }

    static boolean matchesTerms(
            String terms,
            String normalizedQuery) {

        if (terms == null
                || terms.isEmpty()
                || normalizedQuery == null
                || normalizedQuery.isEmpty()) {

            return false;
        }

        String[] available =
                terms.split(
                        " +");

        String[] requested =
                normalizedQuery.split(
                        " +");

        for (String needle : requested) {
            boolean found =
                    false;

            for (String term : available) {
                if (term.startsWith(
                        needle)) {

                    found =
                            true;

                    break;
                }
            }

            if (!found) {
                return false;
            }
        }

        return true;
    }

    private static int score(
            Entry entry,
            String query,
            boolean german) {

        String preferred =
                german
                        ? entry.normalizedGerman
                        : entry.normalizedEnglish;

        String alternate =
                german
                        ? entry.normalizedEnglish
                        : entry.normalizedGerman;

        if (preferred.equals(
                query)) {

            return 0;
        }

        if (alternate.equals(
                query)) {

            return 1;
        }

        if (preferred.startsWith(
                query)) {

            return 2;
        }

        if (alternate.startsWith(
                query)) {

            return 3;
        }

        return 4;
    }

    private static boolean isRenderable(
            String emoji) {

        synchronized (RENDER_CACHE) {
            Boolean cached =
                    RENDER_CACHE.get(
                            emoji);

            if (cached != null) {
                return cached;
            }
        }

        boolean renderable =
                PaintCompat.hasGlyph(
                        new TextPaint(),
                        emoji);

        synchronized (RENDER_CACHE) {
            RENDER_CACHE.put(
                    emoji,
                    renderable);
        }

        return renderable;
    }

    private static List<Entry> getEntries(
            Context context) {

        synchronized (EmojiSearchIndex.class) {
            if (cachedEntries != null) {
                return cachedEntries;
            }

            List<Entry> entries =
                    new ArrayList<>();

            try (
                    InputStream input =
                            context.getAssets()
                                    .open(
                                            "emoji_search.json");

                    JsonReader reader =
                            new JsonReader(
                                    new InputStreamReader(
                                            input,
                                            StandardCharsets.UTF_8))) {

                reader.beginArray();

                int order =
                        0;

                while (reader.hasNext()) {
                    String emoji =
                            "";

                    String german =
                            "";

                    String english =
                            "";

                    String terms =
                            "";

                    reader.beginObject();

                    while (reader.hasNext()) {
                        String name =
                                reader.nextName();

                        switch (name) {
                            case "e":
                                emoji =
                                        reader.nextString();
                                break;

                            case "de":
                                german =
                                        reader.nextString();
                                break;

                            case "en":
                                english =
                                        reader.nextString();
                                break;

                            case "q":
                                terms =
                                        reader.nextString();
                                break;

                            default:
                                reader.skipValue();
                                break;
                        }
                    }

                    reader.endObject();

                    if (!emoji.isEmpty()
                            && !terms.isEmpty()) {

                        entries.add(
                                new Entry(
                                        emoji,
                                        german,
                                        english,
                                        terms,
                                        order));
                    }

                    order++;
                }

                reader.endArray();

            } catch (IOException exception) {

                Log.w(
                        TAG,
                        "Could not load emoji search index.",
                        exception);
            }

            cachedEntries =
                    Collections.unmodifiableList(
                            entries);

            return cachedEntries;
        }
    }

    private static String firstNonEmpty(
            String first,
            String second,
            String fallback) {

        if (first != null
                && !first.isEmpty()) {

            return first;
        }

        if (second != null
                && !second.isEmpty()) {

            return second;
        }

        return fallback;
    }

    static final class Result {

        final String emoji;
        final String label;

        Result(
                String emoji,
                String label) {

            this.emoji =
                    emoji;

            this.label =
                    label;
        }
    }

    private static final class Entry {

        final String emoji;
        final String germanLabel;
        final String englishLabel;
        final String normalizedGerman;
        final String normalizedEnglish;
        final String terms;
        final int order;

        Entry(
                String emoji,
                String germanLabel,
                String englishLabel,
                String terms,
                int order) {

            this.emoji =
                    emoji;

            this.germanLabel =
                    germanLabel;

            this.englishLabel =
                    englishLabel;

            this.normalizedGerman =
                    normalizeForSearch(
                            germanLabel);

            this.normalizedEnglish =
                    normalizeForSearch(
                            englishLabel);

            this.terms =
                    terms;

            this.order =
                    order;
        }
    }

    private static final class ScoredEntry {

        final Entry entry;
        final int score;

        ScoredEntry(
                Entry entry,
                int score) {

            this.entry =
                    entry;

            this.score =
                    score;
        }
    }
}
