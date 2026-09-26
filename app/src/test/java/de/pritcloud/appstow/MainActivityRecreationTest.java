package de.pritcloud.appstow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class MainActivityRecreationTest {

    @Before
    public void clearStoredEditorData() {

        Context context =
                ApplicationProvider
                        .getApplicationContext();

        context.deleteSharedPreferences(
                "categories");

        context.deleteSharedPreferences(
                "shortcuts");
    }

    @Test
    public void categorySymbolPreferenceWinsOverSavedViewState() {

        try (ActivityScenario<MainActivity> scenario =
                     ActivityScenario.launch(
                             MainActivity.class)) {

            scenario.onActivity(
                    activity -> {

                        CategoryStore store =
                                getPrivateField(
                                        activity,
                                        "categoryStore",
                                        CategoryStore.class);

                        Switch symbolsSwitch =
                                activity.findViewById(
                                        R.id.categorySymbolsSwitch);

                        assertTrue(
                                !symbolsSwitch.isChecked());

                        store.setSymbolsEnabled(
                                true);

                        assertTrue(
                                store.areSymbolsEnabled());
                    });

            scenario.recreate();

            scenario.onActivity(
                    activity -> {

                        CategoryStore store =
                                getPrivateField(
                                        activity,
                                        "categoryStore",
                                        CategoryStore.class);

                        Switch symbolsSwitch =
                                activity.findViewById(
                                        R.id.categorySymbolsSwitch);

                        assertTrue(
                                store.areSymbolsEnabled());

                        assertTrue(
                                symbolsSwitch.isChecked());

                        assertTrue(
                                !symbolsSwitch.isSaveEnabled());
                    });
        }
    }

    @Test
    public void shortcutDraftAndCategoryPickerSurviveRecreation() {

        AtomicReference<String>
                categoryId =
                new AtomicReference<>();

        try (ActivityScenario<MainActivity> scenario =
                     ActivityScenario.launch(
                             MainActivity.class)) {

            scenario.onActivity(
                    activity -> {

                        CategoryStore store =
                                getPrivateField(
                                        activity,
                                        "categoryStore",
                                        CategoryStore.class);

                        assertTrue(
                                store.addCategory(
                                        "Test",
                                        "📁"));

                        categoryId.set(
                                store.getCategories()
                                        .get(0)
                                        .id);

                        showShortcutEditor(
                                activity);

                        AlertDialog editor =
                                latestDialog();

                        EditText name =
                                editor.findViewById(
                                        R.id.shortcutEditName);

                        EditText target =
                                editor.findViewById(
                                        R.id.shortcutEditTarget);

                        CheckBox favorite =
                                editor.findViewById(
                                        R.id.shortcutEditFavorite);

                        TextView categories =
                                editor.findViewById(
                                        R.id.shortcutPickCategories);

                        assertNotNull(name);
                        assertNotNull(target);
                        assertNotNull(favorite);
                        assertNotNull(categories);

                        name.setText(
                                "Recreation test");

                        target.setText(
                                "https://example.com");

                        favorite.setChecked(
                                true);

                        categories.performClick();

                        AlertDialog picker =
                                latestDialog();

                        ListView list =
                                picker.getListView();

                        assertNotNull(list);
                        assertEquals(
                                1,
                                list.getCount());

                        list.performItemClick(
                                null,
                                0,
                                list.getAdapter()
                                        .getItemId(0));
                    });

            scenario.recreate();

            scenario.onActivity(
                    activity -> {

                        Bundle draft =
                                getPrivateField(
                                        activity,
                                        "shortcutEditorDraft",
                                        Bundle.class);

                        assertNotNull(draft);

                        assertEquals(
                                "Recreation test",
                                draft.getString(
                                        "shortcut_name"));

                        assertEquals(
                                "https://example.com",
                                draft.getString(
                                        "shortcut_target"));

                        assertTrue(
                                draft.getBoolean(
                                        "shortcut_favorite"));

                        assertTrue(
                                draft.getBoolean(
                                        "shortcut_picker_open"));

                        ArrayList<String> temporary =
                                draft.getStringArrayList(
                                        "shortcut_picker_temp");

                        assertNotNull(
                                temporary);

                        assertTrue(
                                temporary.contains(
                                        categoryId.get()));

                        AlertDialog picker =
                                latestDialog();

                        assertTrue(
                                picker.isShowing());

                        assertNotNull(
                                picker.getListView());

                        assertTrue(
                                picker.getListView()
                                        .isItemChecked(0));
                    });
        }
    }

    @Test
    public void appCategoryAssignmentSurvivesRecreation() {

        AtomicReference<String>
                categoryId =
                new AtomicReference<>();

        try (ActivityScenario<MainActivity> scenario =
                     ActivityScenario.launch(
                             MainActivity.class)) {

            scenario.onActivity(
                    activity -> {

                        CategoryStore store =
                                getPrivateField(
                                        activity,
                                        "categoryStore",
                                        CategoryStore.class);

                        assertTrue(
                                store.addCategory(
                                        "Assignment test",
                                        "📁"));

                        categoryId.set(
                                store.getCategories()
                                        .get(0)
                                        .id);

                        showAppCategoryAssignment(
                                activity,
                                "com.example.assignment");

                        AlertDialog dialog =
                                latestDialog();

                        ListView list =
                                dialog.getListView();

                        assertNotNull(
                                list);

                        assertEquals(
                                1,
                                list.getCount());

                        list.performItemClick(
                                null,
                                0,
                                list.getAdapter()
                                        .getItemId(0));
                    });

            scenario.recreate();

            scenario.onActivity(
                    activity -> {

                        Bundle draft =
                                getPrivateField(
                                        activity,
                                        "appAssignmentDraft",
                                        Bundle.class);

                        assertNotNull(
                                draft);

                        assertEquals(
                                "com.example.assignment",
                                draft.getString(
                                        "app_assignment_package"));

                        ArrayList<String> selected =
                                draft.getStringArrayList(
                                        "app_assignment_categories");

                        assertNotNull(
                                selected);

                        assertTrue(
                                selected.contains(
                                        categoryId.get()));

                        AlertDialog dialog =
                                latestDialog();

                        ListView list =
                                dialog.getListView();

                        assertNotNull(
                                list);

                        assertTrue(
                                list.isItemChecked(
                                        0));
                    });
        }
    }

    @Test
    public void categoryDraftSurvivesRecreation() {

        try (ActivityScenario<MainActivity> scenario =
                     ActivityScenario.launch(
                             MainActivity.class)) {

            scenario.onActivity(
                    activity -> {

                        showAddCategoryDialog(
                                activity);

                        AlertDialog dialog =
                                latestDialog();

                        EditText name =
                                dialog.findViewById(
                                        R.id.categoryNameInput);

                        TextView symbol =
                                dialog.findViewById(
                                        R.id.categorySymbolInput);

                        assertNotNull(name);
                        assertNotNull(symbol);

                        name.setText(
                                "Recreation category");

                        symbol.setText(
                                "🚀");
                    });

            scenario.recreate();

            scenario.onActivity(
                    activity -> {

                        Bundle draft =
                                getPrivateField(
                                        activity,
                                        "categoryEditorDraft",
                                        Bundle.class);

                        assertNotNull(draft);

                        assertEquals(
                                "Recreation category",
                                draft.getString(
                                        "category_editor_name"));

                        assertEquals(
                                "🚀",
                                draft.getString(
                                        "category_editor_symbol"));

                        AlertDialog dialog =
                                latestDialog();

                        EditText name =
                                dialog.findViewById(
                                        R.id.categoryNameInput);

                        TextView symbol =
                                dialog.findViewById(
                                        R.id.categorySymbolInput);

                        assertNotNull(name);
                        assertNotNull(symbol);

                        assertEquals(
                                "Recreation category",
                                name.getText()
                                        .toString());

                        assertEquals(
                                "🚀",
                                symbol.getText()
                                        .toString());
                    });
        }
    }

    @Test
    public void emojiSearchSurvivesRecreation() {

        try (ActivityScenario<MainActivity> scenario =
                     ActivityScenario.launch(
                             MainActivity.class)) {

            scenario.onActivity(
                    activity -> {

                        showAddCategoryDialog(
                                activity);

                        AlertDialog category =
                                latestDialog();

                        TextView symbol =
                                category.findViewById(
                                        R.id.categorySymbolInput);

                        assertNotNull(symbol);

                        symbol.performClick();

                        AlertDialog emoji =
                                latestDialog();

                        EditText search =
                                emoji.findViewById(
                                        R.id.emojiSearchInput);

                        assertNotNull(search);

                        search.setText(
                                "auto");
                    });

            scenario.recreate();

            scenario.onActivity(
                    activity -> {

                        Bundle draft =
                                getPrivateField(
                                        activity,
                                        "emojiPickerDraft",
                                        Bundle.class);

                        assertNotNull(draft);

                        assertEquals(
                                "auto",
                                draft.getString(
                                        "emoji_query"));

                        AlertDialog emoji =
                                latestDialog();

                        assertTrue(
                                emoji.isShowing());

                        EditText search =
                                emoji.findViewById(
                                        R.id.emojiSearchInput);

                        assertNotNull(search);

                        assertEquals(
                                "auto",
                                search.getText()
                                        .toString());
                    });
        }
    }

    private static AlertDialog latestDialog() {

        AlertDialog dialog =
                ShadowAlertDialog
                        .getLatestAlertDialog();

        assertNotNull(
                dialog);

        assertTrue(
                dialog.isShowing());

        return dialog;
    }

    private static void showShortcutEditor(
            MainActivity activity) {

        invoke(
                activity,
                "showShortcutEditor",
                new Class<?>[] {
                        ShortcutEntry.class
                },
                new Object[] {
                        null
                });
    }

    private static void showAppCategoryAssignment(
            MainActivity activity,
            String packageName) {

        invoke(
                activity,
                "showAppCategoryAssignment",
                new Class<?>[] {
                        String.class,
                        Bundle.class
                },
                new Object[] {
                        packageName,
                        null
                });
    }

    private static void showAddCategoryDialog(
            MainActivity activity) {

        invoke(
                activity,
                "showAddCategoryDialog",
                new Class<?>[0],
                new Object[0]);
    }

    private static void invoke(
            Object target,
            String name,
            Class<?>[] parameterTypes,
            Object[] arguments) {

        try {
            Method method =
                    target.getClass()
                            .getDeclaredMethod(
                                    name,
                                    parameterTypes);

            method.setAccessible(
                    true);

            method.invoke(
                    target,
                    arguments);

        } catch (ReflectiveOperationException exception) {

            throw new AssertionError(
                    exception);
        }
    }

    private static <T> T getPrivateField(
            Object target,
            String name,
            Class<T> type) {

        try {
            Field field =
                    target.getClass()
                            .getDeclaredField(
                                    name);

            field.setAccessible(
                    true);

            return type.cast(
                    field.get(
                            target));

        } catch (ReflectiveOperationException exception) {

            throw new AssertionError(
                    exception);
        }
    }
}
