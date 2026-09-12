package de.pritcloud.shortcutlauncher;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

final class FavoritesStore {

    private static final String PREFS_NAME = "favorites";
    private static final String KEY_PACKAGES = "packages";

    private final SharedPreferences preferences;
    private final Set<String> favorites;

    FavoritesStore(Context context) {
        preferences = context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE);

        favorites = new HashSet<>(
                preferences.getStringSet(
                        KEY_PACKAGES,
                        Collections.emptySet()));
    }

    boolean isFavorite(String packageName) {
        return favorites.contains(packageName);
    }

    boolean toggle(String packageName) {
        boolean favorite;

        if (favorites.contains(packageName)) {
            favorites.remove(packageName);
            favorite = false;
        } else {
            favorites.add(packageName);
            favorite = true;
        }

        preferences.edit()
                .putStringSet(
                        KEY_PACKAGES,
                        new HashSet<>(favorites))
                .apply();

        return favorite;
    }
}
