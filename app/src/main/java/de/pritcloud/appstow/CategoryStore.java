package de.pritcloud.appstow;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class CategoryStore {

    private static final String PREFS_NAME = "categories";
    private static final String KEY_CATEGORIES = "category_list";
    private static final String KEY_ASSIGNMENTS = "category_assignments";
    private static final String KEY_SYMBOLS_ENABLED =
            "category_symbols_enabled";

    static final String DEFAULT_SYMBOL = "📁";

    private final SharedPreferences preferences;
    private final List<CategoryEntry> categories =
            new ArrayList<>();

    private final Map<String, Set<String>> assignments =
            new HashMap<>();

    CategoryStore(Context context) {
        preferences = context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE);

        loadCategories();
        loadAssignments();
    }

    List<CategoryEntry> getCategories() {
        return new ArrayList<>(categories);
    }

    boolean areSymbolsEnabled() {
        return preferences.getBoolean(
                KEY_SYMBOLS_ENABLED,
                false);
    }

    void setSymbolsEnabled(
            boolean enabled) {

        preferences.edit()
                .putBoolean(
                        KEY_SYMBOLS_ENABLED,
                        enabled)
                .apply();
    }

    boolean addCategory(
            String name,
            String symbol) {

        String normalizedName =
                name.trim();

        String normalizedSymbol =
                symbol.trim();

        if (normalizedName.isEmpty()
                || !EmojiValidator.isValid(normalizedSymbol)
                || nameExists(
                        normalizedName,
                        null)) {

            return false;
        }

        categories.add(
                new CategoryEntry(
                        UUID.randomUUID().toString(),
                        normalizedName,
                        normalizedSymbol));

        saveCategories();
        return true;
    }

    boolean renameCategory(
            String id,
            String name,
            String symbol) {

        String normalizedName =
                name.trim();

        String normalizedSymbol =
                symbol.trim();

        if (normalizedName.isEmpty()
                || !EmojiValidator.isValid(normalizedSymbol)
                || nameExists(
                        normalizedName,
                        id)) {

            return false;
        }

        for (int i = 0; i < categories.size(); i++) {
            CategoryEntry category = categories.get(i);

            if (category.id.equals(id)) {
                categories.set(
                        i,
                        new CategoryEntry(
                                id,
                                normalizedName,
                                normalizedSymbol));

                saveCategories();
                return true;
            }
        }

        return false;
    }

    void deleteCategory(String id) {
        categories.removeIf(
                category -> category.id.equals(id));

        saveCategories();
        removeCategoryFromAssignments(id);
    }

    boolean hasAssignments(
            String packageName) {

        Set<String> assigned =
                assignments.get(
                        packageName);

        return assigned != null
                && !assigned.isEmpty();
    }

    boolean isAssignedToCategory(
            String packageName,
            String categoryId) {

        Set<String> assigned =
                assignments.get(
                        packageName);

        return assigned != null
                && assigned.contains(
                        categoryId);
    }

    Set<String> getAssignedCategoryIds(
            String packageName) {

        Set<String> assigned =
                assignments.get(packageName);

        return assigned == null
                ? new HashSet<>()
                : new HashSet<>(assigned);
    }

    void setAssignedCategoryIds(
            String packageName,
            Set<String> categoryIds) {

        if (categoryIds.isEmpty()) {
            assignments.remove(packageName);
        } else {
            assignments.put(
                    packageName,
                    new HashSet<>(categoryIds));
        }

        saveAssignments();
    }

    String getCategoryLabel(
            Set<String> categoryIds) {

        if (categoryIds == null
                || categoryIds.isEmpty()) {

            return "";
        }

        StringBuilder label =
                new StringBuilder();

        for (CategoryEntry category : categories) {
            if (!categoryIds.contains(
                    category.id)) {

                continue;
            }

            if (label.length() > 0) {
                label.append(" · ");
            }

            label.append(
                    category.name);
        }

        return label.toString();
    }

    String getAssignedCategoryLabel(
            String packageName) {

        Set<String> assignedIds =
                assignments.get(
                        packageName);

        if (assignedIds == null
                || assignedIds.isEmpty()) {

            return "";
        }

        StringBuilder label =
                new StringBuilder();

        for (CategoryEntry category : categories) {
            if (!assignedIds.contains(category.id)) {
                continue;
            }

            if (label.length() > 0) {
                label.append(" · ");
            }

            label.append(category.name);
        }

        return label.toString();
    }

    private boolean nameExists(
            String name,
            String excludedId) {

        for (CategoryEntry category : categories) {
            if (excludedId != null
                    && category.id.equals(excludedId)) {
                continue;
            }

            if (category.name.equalsIgnoreCase(name)) {
                return true;
            }
        }

        return false;
    }

    private void loadCategories() {
        categories.clear();

        String saved =
                preferences.getString(
                        KEY_CATEGORIES,
                        "[]");

        boolean migrated =
                false;

        try {
            JSONArray array =
                    new JSONArray(saved);

            for (int i = 0;
                 i < array.length();
                 i++) {

                JSONObject object =
                        array.getJSONObject(i);

                String symbol =
                        object.optString(
                                        "symbol",
                                        "")
                                .trim();

                if (!EmojiValidator.isValid(symbol)) {
                    symbol = DEFAULT_SYMBOL;
                    migrated = true;
                }

                categories.add(
                        new CategoryEntry(
                                object.getString("id"),
                                object.getString("name"),
                                symbol));
            }

            if (migrated) {
                saveCategories();
            }

        } catch (JSONException ignored) {
        }
    }

    private void saveCategories() {
        JSONArray array = new JSONArray();

        try {
            for (CategoryEntry category : categories) {
                JSONObject object =
                        new JSONObject();

                object.put("id", category.id);
                object.put("name", category.name);
                object.put(
                        "symbol",
                        category.symbol);

                array.put(object);
            }
        } catch (JSONException ignored) {
        }

        preferences.edit()
                .putString(
                        KEY_CATEGORIES,
                        array.toString())
                .apply();
    }

    private void loadAssignments() {
        assignments.clear();

        String saved =
                preferences.getString(
                        KEY_ASSIGNMENTS,
                        "{}");

        try {
            JSONObject object =
                    new JSONObject(saved);

            Iterator<String> keys =
                    object.keys();

            while (keys.hasNext()) {
                String packageName =
                        keys.next();

                JSONArray ids =
                        object.optJSONArray(
                                packageName);

                if (ids == null) {
                    continue;
                }

                Set<String> categoryIds =
                        new HashSet<>();

                for (int i = 0;
                     i < ids.length();
                     i++) {

                    categoryIds.add(
                            ids.getString(i));
                }

                if (!categoryIds.isEmpty()) {
                    assignments.put(
                            packageName,
                            categoryIds);
                }
            }

        } catch (JSONException ignored) {
            assignments.clear();
        }
    }

    private void saveAssignments() {
        JSONObject object =
                new JSONObject();

        try {
            for (Map.Entry<String, Set<String>> entry :
                    assignments.entrySet()) {

                JSONArray ids =
                        new JSONArray();

                for (String id :
                        entry.getValue()) {

                    ids.put(id);
                }

                if (ids.length() > 0) {
                    object.put(
                            entry.getKey(),
                            ids);
                }
            }

        } catch (JSONException ignored) {
            return;
        }

        preferences.edit()
                .putString(
                        KEY_ASSIGNMENTS,
                        object.toString())
                .apply();
    }

    private void removeCategoryFromAssignments(
            String categoryId) {

        boolean changed = false;

        Iterator<Map.Entry<String, Set<String>>> iterator =
                assignments.entrySet()
                        .iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, Set<String>> entry =
                    iterator.next();

            Set<String> ids =
                    entry.getValue();

            if (!ids.remove(categoryId)) {
                continue;
            }

            changed = true;

            if (ids.isEmpty()) {
                iterator.remove();
            }
        }

        if (changed) {
            saveAssignments();
        }
    }

}
