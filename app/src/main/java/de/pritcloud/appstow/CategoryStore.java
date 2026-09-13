package de.pritcloud.appstow;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

final class CategoryStore {

    private static final String PREFS_NAME = "categories";
    private static final String KEY_CATEGORIES = "category_list";
    private static final String KEY_ASSIGNMENTS = "category_assignments";

    private final SharedPreferences preferences;
    private final List<CategoryEntry> categories =
            new ArrayList<>();

    CategoryStore(Context context) {
        preferences = context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE);

        loadCategories();
    }

    List<CategoryEntry> getCategories() {
        return new ArrayList<>(categories);
    }

    boolean addCategory(String name) {
        String normalized = name.trim();

        if (normalized.isEmpty()
                || nameExists(normalized, null)) {
            return false;
        }

        categories.add(
                new CategoryEntry(
                        UUID.randomUUID().toString(),
                        normalized));

        saveCategories();
        return true;
    }

    boolean renameCategory(
            String id,
            String name) {

        String normalized = name.trim();

        if (normalized.isEmpty()
                || nameExists(normalized, id)) {
            return false;
        }

        for (int i = 0; i < categories.size(); i++) {
            CategoryEntry category = categories.get(i);

            if (category.id.equals(id)) {
                categories.set(
                        i,
                        new CategoryEntry(
                                id,
                                normalized));

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

    Set<String> getAssignedCategoryIds(
            String packageName) {

        Set<String> result = new HashSet<>();

        try {
            JSONObject assignments =
                    loadAssignments();

            JSONArray ids =
                    assignments.optJSONArray(packageName);

            if (ids == null) {
                return result;
            }

            for (int i = 0; i < ids.length(); i++) {
                result.add(ids.getString(i));
            }
        } catch (JSONException ignored) {
        }

        return result;
    }

    void setAssignedCategoryIds(
            String packageName,
            Set<String> categoryIds) {

        try {
            JSONObject assignments =
                    loadAssignments();

            if (categoryIds.isEmpty()) {
                assignments.remove(packageName);
            } else {
                JSONArray ids = new JSONArray();

                for (String id : categoryIds) {
                    ids.put(id);
                }

                assignments.put(packageName, ids);
            }

            saveAssignments(assignments);
        } catch (JSONException ignored) {
        }
    }

    String getAssignedCategoryLabel(
            String packageName) {

        Set<String> assignedIds =
                getAssignedCategoryIds(packageName);

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

        try {
            JSONArray array = new JSONArray(saved);

            for (int i = 0; i < array.length(); i++) {
                JSONObject object =
                        array.getJSONObject(i);

                categories.add(
                        new CategoryEntry(
                                object.getString("id"),
                                object.getString("name")));
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

    private JSONObject loadAssignments()
            throws JSONException {

        return new JSONObject(
                preferences.getString(
                        KEY_ASSIGNMENTS,
                        "{}"));
    }

    private void saveAssignments(
            JSONObject assignments) {

        preferences.edit()
                .putString(
                        KEY_ASSIGNMENTS,
                        assignments.toString())
                .apply();
    }

    private void removeCategoryFromAssignments(
            String categoryId) {

        try {
            JSONObject assignments =
                    loadAssignments();

            List<String> packages =
                    new ArrayList<>();

            Iterator<String> keys =
                    assignments.keys();

            while (keys.hasNext()) {
                packages.add(keys.next());
            }

            for (String packageName : packages) {
                JSONArray oldIds =
                        assignments.optJSONArray(
                                packageName);

                if (oldIds == null) {
                    continue;
                }

                JSONArray newIds =
                        new JSONArray();

                for (int i = 0; i < oldIds.length(); i++) {
                    String id =
                            oldIds.getString(i);

                    if (!id.equals(categoryId)) {
                        newIds.put(id);
                    }
                }

                if (newIds.length() == 0) {
                    assignments.remove(packageName);
                } else {
                    assignments.put(
                            packageName,
                            newIds);
                }
            }

            saveAssignments(assignments);
        } catch (JSONException ignored) {
        }
    }
}
