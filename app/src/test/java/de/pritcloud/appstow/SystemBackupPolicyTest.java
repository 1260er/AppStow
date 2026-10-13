package de.pritcloud.appstow;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;

import androidx.test.core.app.ApplicationProvider;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.xmlpull.v1.XmlPullParser;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class SystemBackupPolicyTest {

    private Context context;

    private static final Set<String> ALLOWED = new HashSet<>(Arrays.asList(
            "categories.xml", "favorites.xml", "shortcuts.xml",
            "overview_order.xml", "section_item_order.xml",
            "overview_display.xml", "ui_settings.xml",
            "statistics_display.xml", "system_backup_sorting.xml"));

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        context.deleteSharedPreferences("sorting_settings");
        context.deleteSharedPreferences(SystemBackupSortingStore.PREFS_NAME);
    }

    @Test
    public void onlyAllowlistedConfigurationIsBackedUp() throws Exception {
        XmlPullParser xml = context.getResources().getXml(
                R.xml.data_extraction_rules);
        String section = null;
        Set<String> cloud = new HashSet<>();
        Set<String> device = new HashSet<>();
        boolean encryptionRequired = false;

        for (int event = xml.getEventType();
             event != XmlPullParser.END_DOCUMENT;
             event = xml.next()) {
            if (event == XmlPullParser.START_TAG) {
                String name = xml.getName();
                if ("cloud-backup".equals(name)) {
                    section = name;
                    encryptionRequired = "true".equals(
                            xml.getAttributeValue(
                                    null, "disableIfNoEncryptionCapabilities"));
                } else if ("device-transfer".equals(name)) {
                    section = name;
                } else if ("include".equals(name)) {
                    assertEquals("sharedpref", xml.getAttributeValue(null, "domain"));
                    String path = xml.getAttributeValue(null, "path");
                    assertNotNull(path);
                    assertTrue("Unexpected preference file: " + path,
                            ALLOWED.contains(path));
                    if ("cloud-backup".equals(section)) {
                        assertTrue(cloud.add(path));
                    } else if ("device-transfer".equals(section)) {
                        assertTrue(device.add(path));
                    } else {
                        throw new AssertionError("Include outside backup mode.");
                    }
                } else if ("exclude".equals(name)) {
                    throw new AssertionError("Strict allowlist must not have excludes.");
                }
            } else if (event == XmlPullParser.END_TAG
                    && ("cloud-backup".equals(xml.getName())
                    || "device-transfer".equals(xml.getName()))) {
                section = null;
            }
        }

        assertTrue(encryptionRequired);
        assertEquals(ALLOWED, cloud);
        assertEquals(ALLOWED, device);
        assertFalse(cloud.contains("sorting_settings.xml"));
        assertFalse(cloud.contains("usage_statistics.xml"));
        assertFalse(cloud.contains("backup_runtime.xml"));
        assertFalse(cloud.contains("appstow-restore-journal.json"));
    }

    @Test
    public void manifestAllowsAndroidBackup() {
        ApplicationInfo info = context.getApplicationInfo();
        assertTrue((info.flags & ApplicationInfo.FLAG_ALLOW_BACKUP) != 0);
    }

    @Test
    public void mirrorContainsOnlyExplicitConfiguration() throws Exception {
        SortingSettingsStore store = new SortingSettingsStore(context);
        SortingSettingsStore.Settings selected =
                new SortingSettingsStore.Settings(
                        SortingSettingsStore.Mode.SEMI_AUTOMATIC,
                        true, false, true, false, true,
                        7, 22, 12, false, true, 30);
        store.save(selected);

        // Simulate sensitive cached usage data coexisting with settings.
        context.getSharedPreferences("sorting_settings", Context.MODE_PRIVATE)
                .edit().putString("automatic_snapshot_v1_global",
                        "{\"launches\":{\"apps\":{\"private.app\":99}}}")
                .putLong("last_suggestion_handled_at", 99999L)
                .commit();

        SharedPreferences mirror = context.getSharedPreferences(
                SystemBackupSortingStore.PREFS_NAME, Context.MODE_PRIVATE);
        assertEquals(Set.of("configuration_v1"), mirror.getAll().keySet());
        String payload = mirror.getString("configuration_v1", null);
        assertNotNull(payload);
        JSONObject config = new JSONObject(payload);
        assertEquals("SEMI_AUTOMATIC", config.getString("mode"));
        assertEquals(12, config.getInt("automaticFavoriteCount"));
        assertFalse(config.has("launches"));
        assertFalse(payload.contains("private.app"));
        assertFalse(payload.contains("last_suggestion_handled_at"));
    }

    @Test
    public void systemRestoreRehydratesSortingWithoutUsageHistory() {
        SortingSettingsStore.Settings chosen =
                new SortingSettingsStore.Settings(
                        SortingSettingsStore.Mode.AUTOMATIC,
                        false, true, false, true, true,
                        7, 22, 11, false, true, 14);
        SystemBackupSortingStore.save(context, chosen);

        // The transport restores only the mirror, NOT sorting_settings.xml.
        context.deleteSharedPreferences("sorting_settings");
        AppStowBackupAgent.restoreConfiguration(context);
        SortingSettingsStore restored = new SortingSettingsStore(context);
        SortingSettingsStore.Settings actual = restored.load();
        assertEquals(SortingSettingsStore.Mode.AUTOMATIC, actual.mode);
        assertEquals(11, actual.automaticFavoriteCount);
        assertEquals(14, actual.suggestionIntervalDays);
        assertEquals(false, actual.semiFavorites);
        assertEquals(false, restored.hasApprovedAutomaticHistory());
        assertEquals(0L, restored.getLastSuggestionHandledAt());
    }

    @Test
    public void corruptMirrorIsIgnoredSafely() {
        SharedPreferences mirror = context.getSharedPreferences(
                SystemBackupSortingStore.PREFS_NAME, Context.MODE_PRIVATE);
        mirror.edit().putString("configuration_v1", "not json").commit();
        AppStowBackupAgent.restoreConfiguration(context);
        SortingSettingsStore restored = new SortingSettingsStore(context);
        assertEquals(SortingSettingsStore.Mode.MANUAL, restored.load().mode);
        assertNull(SystemBackupSortingStore.load(context));
    }

    @Test
    public void installedPreferencesStayAuthoritative() {
        SortingSettingsStore.Settings manual = SortingSettingsStore.Settings.defaults();
        SortingSettingsStore store = new SortingSettingsStore(context);
        store.save(manual);
        SystemBackupSortingStore.save(context,
                new SortingSettingsStore.Settings(
                        SortingSettingsStore.Mode.AUTOMATIC,
                        true, true, true, true, false,
                        8, 21, 10, false));
        assertEquals(SortingSettingsStore.Mode.MANUAL,
                new SortingSettingsStore(context).load().mode);
    }
}
