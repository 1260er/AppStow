package de.pritcloud.appstow;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class BackupRestoreFilterTest {

    @Test
    public void missingAppsAreRemovedButShortcutsRemain()
            throws Exception {

        Set<String> installed =
                new HashSet<>(
                        Arrays.asList(
                                "com.keep"));

        JSONObject assignments =
                new JSONObject()
                        .put(
                                "com.keep",
                                new JSONArray()
                                        .put(
                                                "cat"))
                        .put(
                                "com.missing",
                                new JSONArray()
                                        .put(
                                                "cat"));

        JSONArray favorites =
                new JSONArray()
                        .put(
                                "com.keep")
                        .put(
                                "com.missing");

        JSONObject orders =
                new JSONObject()
                        .put(
                                "favorites",
                                new JSONArray()
                                        .put(
                                                "app:com.keep")
                                        .put(
                                                "app:com.missing")
                                        .put(
                                                "shortcut:web"));

        JSONObject filteredAssignments =
                BackupRestoreFilter.filterAssignments(
                        assignments,
                        installed);

        Set<String> filteredFavorites =
                BackupRestoreFilter.filterFavorites(
                        favorites,
                        installed);

        JSONObject filteredOrders =
                BackupRestoreFilter.filterSectionItemOrder(
                        orders,
                        installed);

        assertTrue(
                filteredAssignments.has(
                        "com.keep"));

        assertTrue(
                !filteredAssignments.has(
                        "com.missing"));

        assertEquals(
                new HashSet<>(
                        Arrays.asList(
                                "com.keep")),
                filteredFavorites);

        JSONArray favoriteOrder =
                filteredOrders.getJSONArray(
                        "favorites");

        assertEquals(
                2,
                favoriteOrder.length());

        assertEquals(
                "app:com.keep",
                favoriteOrder.getString(
                        0));

        assertEquals(
                "shortcut:web",
                favoriteOrder.getString(
                        1));
    }
}
