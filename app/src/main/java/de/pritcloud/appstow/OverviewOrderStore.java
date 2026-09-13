package de.pritcloud.appstow;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

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
        JSONArray array = new JSONArray();

        for (OverviewSection section : sections) {
            array.put(section.id);
        }

        preferences.edit()
                .putString(KEY_ORDER, array.toString())
                .apply();
    }
}
