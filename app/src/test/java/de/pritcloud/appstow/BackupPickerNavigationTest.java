package de.pritcloud.appstow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class BackupPickerNavigationTest {

    private static final int CREATE_BACKUP = 1001;
    private static final int RESTORE_BACKUP = 1002;

    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        for (String name : new String[] {
                "ui_settings", "sorting_settings", "categories", "shortcuts",
                "favorites", "overview_display", "usage_statistics",
                "statistics_display", "backup_runtime"}) {
            context.deleteSharedPreferences(name);
        }
    }

    private void configure(UiSettingsStore.StartBehavior behavior,
                           UiSettingsStore.StartView view) {
        new UiSettingsStore(context).save(new UiSettingsStore.Settings(
                behavior, UiSettingsStore.ControlSide.RIGHT,
                UiSettingsStore.ThemeMode.SYSTEM, view));
    }

    private static void openBackupAndPicker(MainActivity activity,
                                             boolean restore) {
        activity.findViewById(R.id.navBackup).performClick();
        assertEquals("backup", privateField(activity, "currentPage", String.class));
        activity.findViewById(restore
                ? R.id.backupRestoreButton
                : R.id.backupCreateButton).performClick();
        assertTrue(privateField(activity, "backupPickerPending", Boolean.class));
    }

    @Test
    public void createPickerReturnDoesNotActivateAlwaysFavorites() {
        configure(UiSettingsStore.StartBehavior.ALWAYS_FOREGROUND,
                UiSettingsStore.StartView.FAVORITES);
        try (ActivityScenario<MainActivity> scenario =
                     ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(a -> openBackupAndPicker(a, false));
            scenario.moveToState(Lifecycle.State.CREATED);
            scenario.moveToState(Lifecycle.State.RESUMED);
            scenario.onActivity(a -> {
                assertEquals("backup", privateField(a, "currentPage", String.class));
                a.onActivityResult(CREATE_BACKUP, Activity.RESULT_CANCELED, null);
                assertFalse(privateField(a, "backupPickerPending", Boolean.class));
                assertEquals("backup", privateField(a, "currentPage", String.class));
            });

            // A later real background return must still obey Always.
            scenario.moveToState(Lifecycle.State.CREATED);
            scenario.moveToState(Lifecycle.State.RESUMED);
            scenario.onActivity(a -> assertEquals("overview",
                    privateField(a, "currentPage", String.class)));
        }
    }

    @Test
    public void restorePickerReturnPreservesBackupPageForLastCategory() {
        CategoryStore categories = new CategoryStore(context);
        assertTrue(categories.addCategory("Tools", "🔧"));
        String sectionId = "category:" + categories.getCategories().get(0).id;
        configure(UiSettingsStore.StartBehavior.ALWAYS_FOREGROUND,
                UiSettingsStore.StartView.LAST_CATEGORY);
        new UiSettingsStore(context).rememberLastCategory(sectionId);
        SharedPreferences display = context.getSharedPreferences(
                "overview_display", Context.MODE_PRIVATE);
        assertTrue(display.edit().putBoolean("grid_mode", true).commit());

        try (ActivityScenario<MainActivity> scenario =
                     ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(a -> openBackupAndPicker(a, true));
            scenario.moveToState(Lifecycle.State.CREATED);
            scenario.moveToState(Lifecycle.State.RESUMED);
            scenario.onActivity(a -> {
                assertEquals("backup", privateField(a, "currentPage", String.class));
                a.onActivityResult(RESTORE_BACKUP, Activity.RESULT_CANCELED, null);
            });
            scenario.moveToState(Lifecycle.State.CREATED);
            scenario.moveToState(Lifecycle.State.RESUMED);
            scenario.onActivity(a -> {
                assertEquals("overview", privateField(a, "currentPage", String.class));
                OverviewAdapter adapter = privateField(a, "overviewAdapter", OverviewAdapter.class);
                assertEquals(sectionId, adapter.getGridOpenSectionId());
            });
        }
    }

    @Test
    public void resultDeliveredBeforeOnStartAlsoSkipsRedirect() {
        configure(UiSettingsStore.StartBehavior.ALWAYS_FOREGROUND,
                UiSettingsStore.StartView.FAVORITES);
        try (ActivityScenario<MainActivity> scenario =
                     ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(a -> openBackupAndPicker(a, false));
            scenario.onActivity(a -> {
                // Simulate Android delivering the canceled picker result
                // while onStop has run, but before onStart executes.
                setPrivateField(a, "stoppedForBackground", true);
                a.onActivityResult(CREATE_BACKUP, Activity.RESULT_CANCELED, null);
                assertTrue(privateField(a,
                        "skipNextForegroundStartForBackupPicker", Boolean.class));
            });
            scenario.moveToState(Lifecycle.State.CREATED);
            scenario.moveToState(Lifecycle.State.RESUMED);
            scenario.onActivity(a -> {
                assertEquals("backup", privateField(a, "currentPage", String.class));
                assertFalse(privateField(a,
                        "skipNextForegroundStartForBackupPicker", Boolean.class));
            });
        }
    }

    @Test
    public void recreatedWhilePickerPendingRetainsBackupPage() {
        configure(UiSettingsStore.StartBehavior.ALWAYS_FOREGROUND,
                UiSettingsStore.StartView.FAVORITES);
        try (ActivityScenario<MainActivity> scenario =
                     ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(a -> openBackupAndPicker(a, true));
            scenario.recreate();
            scenario.onActivity(a -> {
                assertEquals("backup", privateField(a, "currentPage", String.class));
                assertTrue(privateField(a, "backupPickerPending", Boolean.class));
            });
            scenario.moveToState(Lifecycle.State.CREATED);
            scenario.moveToState(Lifecycle.State.RESUMED);
            scenario.onActivity(a -> {
                assertEquals("backup", privateField(a, "currentPage", String.class));
                a.onActivityResult(RESTORE_BACKUP, Activity.RESULT_CANCELED, null);
                assertFalse(privateField(a, "backupPickerPending", Boolean.class));
            });
        }
    }

    @Test
    public void noPickerDoesNotDisableAlwaysBehavior() {
        configure(UiSettingsStore.StartBehavior.ALWAYS_FOREGROUND,
                UiSettingsStore.StartView.FAVORITES);
        try (ActivityScenario<MainActivity> scenario =
                     ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(a -> a.findViewById(R.id.navBackup).performClick());
            scenario.moveToState(Lifecycle.State.CREATED);
            scenario.moveToState(Lifecycle.State.RESUMED);
            scenario.onActivity(a -> assertEquals("overview",
                    privateField(a, "currentPage", String.class)));
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T privateField(Object target, String key, Class<T> type) {
        try {
            Field field = target.getClass().getDeclaredField(key);
            field.setAccessible(true);
            return (T) field.get(target);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private static void setPrivateField(Object target, String key, boolean value) {
        try {
            Field field = target.getClass().getDeclaredField(key);
            field.setAccessible(true);
            field.setBoolean(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
