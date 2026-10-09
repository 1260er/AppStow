package de.pritcloud.appstow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class BackupRestoreJournalTest {

    private Context context;
    private SharedPreferences categories;
    private SharedPreferences favorites;
    private SharedPreferences statistics;

    @Before
    public void setUp() throws Exception {
        context = RuntimeEnvironment.getApplication();
        BackupRestoreJournal.complete(context);

        for (String name : Arrays.asList(
                "categories", "favorites", "shortcuts",
                "overview_order", "section_item_order",
                "overview_display", "sorting_settings",
                "ui_settings", "statistics_display",
                "usage_statistics")) {
            context.getSharedPreferences(name, Context.MODE_PRIVATE)
                    .edit().clear().commit();
        }

        categories = context.getSharedPreferences(
                "categories", Context.MODE_PRIVATE);
        favorites = context.getSharedPreferences(
                "favorites", Context.MODE_PRIVATE);
        statistics = context.getSharedPreferences(
                "usage_statistics", Context.MODE_PRIVATE);
    }

    @After
    public void tearDown() throws Exception {
        BackupRestoreJournal.complete(context);
    }

    @Test
    public void simulatedProcessDeathRollsBackAllPreferenceTypes()
            throws Exception {
        Set<String> previousFavorites =
                new HashSet<>(Arrays.asList("app.one", "app.two"));

        assertTrue(categories.edit()
                .putString("category_list", "before")
                .putBoolean("enabled", true)
                .putInt("columns", 3)
                .putLong("timestamp", Long.MAX_VALUE)
                .putFloat("scale", 1.25f)
                .commit());
        assertTrue(favorites.edit()
                .putStringSet("packages", previousFavorites)
                .commit());
        assertTrue(statistics.edit()
                .putString("day_1", "original counts")
                .commit());

        BackupRestoreJournal.prepare(context);

        // An interrupted restore has already overwritten some
        // of the stores, but the process dies before completion.
        assertTrue(categories.edit()
                .clear()
                .putString("category_list", "partly restored")
                .commit());
        assertTrue(favorites.edit().clear().commit());
        assertTrue(statistics.edit().clear().commit());

        assertTrue(BackupManager.recoverInterruptedRestore(context));
        assertEquals("before", categories.getString("category_list", null));
        assertTrue(categories.getBoolean("enabled", false));
        assertEquals(3, categories.getInt("columns", 0));
        assertEquals(Long.MAX_VALUE,
                categories.getLong("timestamp", 0L));
        assertEquals(1.25f, categories.getFloat("scale", 0f), 0f);
        assertEquals(previousFavorites,
                favorites.getStringSet("packages", Set.of()));
        assertEquals("original counts",
                statistics.getString("day_1", null));
        assertFalse(BackupManager.recoverInterruptedRestore(context));
    }

    @Test
    public void corruptedJournalNeverModifiesExistingPreferences()
            throws Exception {
        assertTrue(categories.edit()
                .putString("category_list", "safe")
                .commit());

        File journal = new File(
                context.getFilesDir(),
                "appstow-restore-journal.json");

        try (FileOutputStream output = new FileOutputStream(journal)) {
            output.write("invalid JSON".getBytes(
                    StandardCharsets.UTF_8));
            output.getFD().sync();
        }

        try {
            BackupManager.recoverInterruptedRestore(context);
            fail("Expected invalid-journal failure");
        } catch (IOException expected) {
            assertTrue(journal.exists());
            assertEquals("safe",
                    categories.getString("category_list", null));
        }
    }

    @Test
    public void successfulBackupRestoreDoesNotLeaveJournal()
            throws Exception {
        JSONObject settings = BackupV3Configuration.create(context);
        assertEquals("MANUAL",
                settings.getJSONObject("sorting").getString("mode"));

        BackupRestoreJournal.prepare(context);

        // A completed successful transaction must not be
        // rolled back during the next app launch.
        assertTrue(categories.edit()
                .putString("category_list", "new configuration")
                .commit());
        BackupRestoreJournal.complete(context);
        assertFalse(BackupManager.recoverInterruptedRestore(context));
        assertEquals("new configuration",
                categories.getString("category_list", null));
    }

    @Test
    public void realSuccessfulRestoreRemovesJournal() throws Exception {
        File backupFile = File.createTempFile(
                "appstow-journal-restore-",
                ".json",
                context.getCacheDir());

        try {
            Uri uri = Uri.fromFile(backupFile);
            BackupManager.writeBackup(context, uri);
            JSONObject backup = BackupManager.readBackup(context, uri);

            assertTrue(categories.edit()
                    .putString("category_list", "stale")
                    .commit());

            BackupManager.restoreBackup(context, backup, Set.of());
            assertFalse(BackupManager.recoverInterruptedRestore(context));
            assertEquals("[]",
                    categories.getString("category_list", null));
        } finally {
            backupFile.delete();
        }
    }

    @Test
    public void pendingJournalBlocksAnotherRestore() throws Exception {
        BackupRestoreJournal.prepare(context);

        try {
            BackupRestoreJournal.prepare(context);
            fail("Existing journal must not be overwritten");
        } catch (IOException expected) {
            assertTrue(BackupManager.recoverInterruptedRestore(context));
        }
    }
}
