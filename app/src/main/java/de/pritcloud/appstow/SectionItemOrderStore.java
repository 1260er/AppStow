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

final class SectionItemOrderStore {

    private static final String PREFS_NAME =
            "section_item_order";

    private static final String KEY_ORDERS =
            "orders";

    private static final String APP_PREFIX =
            "app:";

    private static final String SHORTCUT_PREFIX =
            "shortcut:";

    private final SharedPreferences preferences;

    SectionItemOrderStore(Context context) {
        preferences = context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE);
    }

    static String appItemId(
            String packageName) {

        return APP_PREFIX + packageName;
    }

    static String shortcutItemId(
            String shortcutId) {

        return SHORTCUT_PREFIX + shortcutId;
    }

    List<String> getOrderedIds(
            String sectionId,
            List<String> currentIds) {

        List<String> result =
                new ArrayList<>();

        Set<String> current =
                new HashSet<>(currentIds);

        Set<String> added =
                new HashSet<>();

        JSONObject orders =
                loadOrders();

        JSONArray saved =
                orders.optJSONArray(sectionId);

        if (saved != null) {
            for (int i = 0;
                 i < saved.length();
                 i++) {

                String id =
                        saved.optString(i, "");

                if (current.contains(id)
                        && added.add(id)) {

                    result.add(id);
                }
            }
        }

        for (String id : currentIds) {
            if (added.add(id)) {
                result.add(id);
            }
        }

        saveOrder(
                sectionId,
                result);

        return result;
    }

    void saveOrder(
            String sectionId,
            List<String> itemIds) {

        JSONObject orders =
                loadOrders();

        JSONArray array =
                new JSONArray();

        Set<String> seen =
                new HashSet<>();

        for (String id : itemIds) {
            if (id == null
                    || id.isEmpty()
                    || !seen.add(id)) {

                continue;
            }

            array.put(id);
        }

        try {
            orders.put(
                    sectionId,
                    array);

            preferences.edit()
                    .putString(
                            KEY_ORDERS,
                            orders.toString())
                    .apply();

        } catch (JSONException ignored) {
        }
    }

    void removeOrder(
            String sectionId) {

        JSONObject orders =
                loadOrders();

        orders.remove(sectionId);

        preferences.edit()
                .putString(
                        KEY_ORDERS,
                        orders.toString())
                .apply();
    }

    private JSONObject loadOrders() {
        try {
            return new JSONObject(
                    preferences.getString(
                            KEY_ORDERS,
                            "{}"));

        } catch (JSONException ignored) {
            return new JSONObject();
        }
    }
}
