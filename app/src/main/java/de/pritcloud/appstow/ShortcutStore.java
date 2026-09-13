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
import java.util.UUID;

final class ShortcutStore {

    private static final String PREFS_NAME = "shortcuts";
    private static final String KEY_SHORTCUTS = "shortcut_list";

    private final SharedPreferences preferences;
    private final List<ShortcutEntry> shortcuts = new ArrayList<>();

    ShortcutStore(Context context) {
        preferences = context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE);
        load();
    }

    List<ShortcutEntry> getShortcuts() {
        return new ArrayList<>(shortcuts);
    }

    void add(
            String name,
            String type,
            String target,
            Set<String> categoryIds,
            boolean favorite) {

        shortcuts.add(new ShortcutEntry(
                UUID.randomUUID().toString(),
                name.trim(),
                type,
                target.trim(),
                categoryIds,
                favorite));

        save();
    }

    void update(
            String id,
            String name,
            String type,
            String target,
            Set<String> categoryIds,
            boolean favorite) {

        for (int i = 0; i < shortcuts.size(); i++) {
            if (!shortcuts.get(i).id.equals(id)) continue;

            shortcuts.set(i, new ShortcutEntry(
                    id,
                    name.trim(),
                    type,
                    target.trim(),
                    categoryIds,
                    favorite));

            save();
            return;
        }
    }

    void delete(String id) {
        shortcuts.removeIf(shortcut -> shortcut.id.equals(id));
        save();
    }

    void toggleFavorite(String id) {
        for (int i = 0; i < shortcuts.size(); i++) {
            ShortcutEntry shortcut = shortcuts.get(i);
            if (!shortcut.id.equals(id)) continue;

            shortcuts.set(i, new ShortcutEntry(
                    shortcut.id,
                    shortcut.name,
                    shortcut.type,
                    shortcut.target,
                    shortcut.categoryIds,
                    !shortcut.favorite));

            save();
            return;
        }
    }

    void removeCategory(String categoryId) {
        boolean changed = false;

        for (int i = 0; i < shortcuts.size(); i++) {
            ShortcutEntry shortcut =
                    shortcuts.get(i);

            if (!shortcut.categoryIds.contains(
                    categoryId)) {
                continue;
            }

            Set<String> categoryIds =
                    new HashSet<>(
                            shortcut.categoryIds);

            categoryIds.remove(categoryId);

            shortcuts.set(
                    i,
                    new ShortcutEntry(
                            shortcut.id,
                            shortcut.name,
                            shortcut.type,
                            shortcut.target,
                            categoryIds,
                            shortcut.favorite));

            changed = true;
        }

        if (changed) {
            save();
        }
    }

    private void load() {
        shortcuts.clear();

        String saved = preferences.getString(KEY_SHORTCUTS, "[]");

        try {
            JSONArray array = new JSONArray(saved);

            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                Set<String> categoryIds = new HashSet<>();

                JSONArray categories = object.optJSONArray("categories");
                if (categories != null) {
                    for (int j = 0; j < categories.length(); j++) {
                        categoryIds.add(categories.getString(j));
                    }
                }

                shortcuts.add(new ShortcutEntry(
                        object.getString("id"),
                        object.getString("name"),
                        object.getString("type"),
                        object.getString("target"),
                        categoryIds,
                        object.optBoolean("favorite", false)));
            }
        } catch (JSONException ignored) {
        }
    }

    private void save() {
        JSONArray array = new JSONArray();

        try {
            for (ShortcutEntry shortcut : shortcuts) {
                JSONObject object = new JSONObject();
                object.put("id", shortcut.id);
                object.put("name", shortcut.name);
                object.put("type", shortcut.type);
                object.put("target", shortcut.target);
                object.put("favorite", shortcut.favorite);

                JSONArray categories = new JSONArray();
                for (String id : shortcut.categoryIds) {
                    categories.put(id);
                }

                object.put("categories", categories);
                array.put(object);
            }
        } catch (JSONException ignored) {
        }

        preferences.edit()
                .putString(KEY_SHORTCUTS, array.toString())
                .apply();
    }
}
