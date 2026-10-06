package de.pritcloud.appstow;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class OverviewOrderStore {

    private static final String PREFS_NAME = "overview_order";
    private static final String KEY_ORDER = "section_order";

    private final SharedPreferences preferences;

    OverviewOrderStore(Context context) {
        preferences = context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE);
    }

    List<String> getOrder() {
        List<String> result = new ArrayList<>();

        String saved =
                preferences.getString(
                        KEY_ORDER,
                        "[]");

        try {
            JSONArray array = new JSONArray(saved);

            for (int i = 0; i < array.length(); i++) {
                result.add(array.getString(i));
            }
        } catch (JSONException ignored) {
        }

        return result;
    }

    void saveOrder(List<OverviewSection> sections) {
        List<String> ids =
                new ArrayList<>();

        for (OverviewSection section :
                sections) {

            ids.add(
                    section.id);
        }

        saveOrderIds(
                ids);
    }

    void saveOrderIds(
            List<String> sectionIds) {

        JSONArray array =
                new JSONArray();

        Set<String> seen =
                new HashSet<>();

        if (sectionIds != null) {
            for (String id :
                    sectionIds) {

                if (id == null
                        || id.isBlank()
                        || !seen.add(id)) {

                    continue;
                }

                array.put(id);
            }
        }

        preferences.edit()
                .putString(
                        KEY_ORDER,
                        array.toString())
                .apply();
    }
}
