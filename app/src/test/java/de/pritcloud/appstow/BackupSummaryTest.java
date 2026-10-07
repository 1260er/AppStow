package de.pritcloud.appstow;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class BackupSummaryTest {

    @Test
    public void summaryCountsUniqueAppsAndAllFavorites()
            throws Exception {

        JSONObject backup =
                new JSONObject()
                        .put(
                                "categories",
                                new JSONArray()
                                        .put(
                                                new JSONObject()
                                                        .put(
                                                                "id",
                                                                "cat-a"))
                                        .put(
                                                new JSONObject()
                                                        .put(
                                                                "id",
                                                                "cat-b")))
                        .put(
                                "categoryAssignments",
                                new JSONObject()
                                        .put(
                                                "com.example.one",
                                                new JSONArray()
                                                        .put(
                                                                "cat-a"))
                                        .put(
                                                "com.example.two",
                                                new JSONArray()
                                                        .put(
                                                                "cat-b")))
                        .put(
                                "favoritePackages",
                                new JSONArray()
                                        .put(
                                                "com.example.one")
                                        .put(
                                                "com.example.three"))
                        .put(
                                "shortcuts",
                                new JSONArray()
                                        .put(
                                                new JSONObject()
                                                        .put(
                                                                "id",
                                                                "favorite-shortcut")
                                                        .put(
                                                                "favorite",
                                                                true))
                                        .put(
                                                new JSONObject()
                                                        .put(
                                                                "id",
                                                                "normal-shortcut")
                                                        .put(
                                                                "favorite",
                                                                false))
                                        .put(
                                                new JSONObject()
                                                        .put(
                                                                "id",
                                                                "legacy-shortcut")))
                        .put(
                                "sectionItemOrder",
                                new JSONObject()
                                        .put(
                                                "favorites",
                                                new JSONArray()
                                                        .put(
                                                                "app:com.example.one")
                                                        .put(
                                                                "app:com.example.four")
                                                        .put(
                                                                "shortcut:favorite-shortcut")));

        BackupManager.BackupSummary summary =
                BackupManager.summarizeBackup(
                        backup,
                        123456L);

        assertEquals(
                123456L,
                summary.completedAtMillis);

        assertEquals(
                2,
                summary.categoryCount);

        assertEquals(
                3,
                summary.favoriteCount);

        assertEquals(
                4,
                summary.appCount);

        assertEquals(
                3,
                summary.customShortcutCount);
    }
}
