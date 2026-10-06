package de.pritcloud.appstow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
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

        context.deleteSharedPreferences(
                "overview_display");

        context.deleteSharedPreferences(
                "usage_statistics");

        context.deleteSharedPreferences(
                "statistics_display");

        context.deleteSharedPreferences(
                "sorting_settings");
    }

    @Test
    public void categorySymbolsAreAlwaysShownAfterRecreation() {

        Context context =
                ApplicationProvider
                        .getApplicationContext();

        assertTrue(
                context.getSharedPreferences(
                                "categories",
                                Context.MODE_PRIVATE)
                        .edit()
                        .putBoolean(
                                "category_symbols_enabled",
                                false)
                        .commit());

        AtomicReference<String> categoryId =
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
                                        "Work",
                                        "💼"));

                        categoryId.set(
                                store.getCategories()
                                        .get(0)
                                        .id);

                        invoke(
                                activity,
                                "rebuildOverviewSections",
                                new Class<?>[0],
                                new Object[0]);

                        OverviewSection section =
                                findCategorySection(
                                        activity,
                                        categoryId.get());

                        assertEquals(
                                "💼 Work",
                                section.title);
                    });

            scenario.recreate();

            scenario.onActivity(
                    activity -> {

                        OverviewSection section =
                                findCategorySection(
                                        activity,
                                        categoryId.get());

                        assertEquals(
                                "💼 Work",
                                section.title);
                    });
        }
    }

    @Test
    public void automaticCategoryOrderKeepsOverviewSortButtonHidden()
            throws Exception {

        Context context =
                ApplicationProvider
                        .getApplicationContext();

        new SortingSettingsStore(
                context)
                .save(
                        new SortingSettingsStore.Settings(
                                SortingSettingsStore.Mode.SEMI_AUTOMATIC,
                                false,
                                true,
                                false,
                                false,
                                false,
                                8,
                                21,
                                10,
                                false));

        try (ActivityScenario<MainActivity> scenario =
                     ActivityScenario.launch(
                             MainActivity.class)) {

            scenario.onActivity(
                    activity -> {

                        EditText search =
                                activity.findViewById(
                                        R.id.appSearch);

                        search.setText(
                                "test");

                        search.setText(
                                "");

                        ImageButton sort =
                                activity.findViewById(
                                        R.id.buttonSortOverview);

                        assertEquals(
                                View.GONE,
                                sort.getVisibility());
                    });
        }
    }

    @Test
    public void fullAutomaticModeKeepsOverviewSortButtonHidden() {

        Context context =
                ApplicationProvider
                        .getApplicationContext();

        new SortingSettingsStore(
                context)
                .save(
                        new SortingSettingsStore.Settings(
                                SortingSettingsStore.Mode.AUTOMATIC,
                                true,
                                true,
                                true,
                                true,
                                false,
                                8,
                                21,
                                10,
                                false));

        try (ActivityScenario<MainActivity> scenario =
                     ActivityScenario.launch(
                             MainActivity.class)) {

            scenario.onActivity(
                    activity -> {

                        EditText search =
                                activity.findViewById(
                                        R.id.appSearch);

                        search.setText(
                                "test");

                        search.setText(
                                "");

                        ImageButton sort =
                                activity.findViewById(
                                        R.id.buttonSortOverview);

                        assertEquals(
                                View.GONE,
                                sort.getVisibility());
                    });
        }
    }

    @Test
    public void automaticStartupFavoritesCountOnceAcrossRecreation() {

        Context context =
                ApplicationProvider
                        .getApplicationContext();

        new SortingSettingsStore(
                context)
                .save(
                        new SortingSettingsStore.Settings(
                                SortingSettingsStore.Mode.AUTOMATIC,
                                true,
                                true,
                                true,
                                true,
                                false,
                                8,
                                21,
                                10,
                                true));

        try (ActivityScenario<MainActivity> scenario =
                     ActivityScenario.launch(
                             MainActivity.class)) {

            scenario.onActivity(
                    activity -> {

                        UsageStatisticsStore store =
                                getPrivateField(
                                        activity,
                                        "usageStatisticsStore",
                                        UsageStatisticsStore.class);

                        UsageStatisticsStore.TimeProfile profile =
                                store.getCurrentTimeProfile(
                                        8,
                                        21);

                        UsageStatisticsStore.SortingSnapshot sorting =
                                store.getSortingSnapshot(
                                        profile,
                                        8,
                                        21);

                        assertEquals(
                                1,
                                sorting.getProfileSectionLaunches()
                                        .get("favorites")
                                        .intValue());

                        assertEquals(
                                0,
                                store.getSnapshot(
                                                UsageStatisticsStore.Period.ONE_MONTH)
                                        .getTotalLaunches());
                    });

            scenario.recreate();

            scenario.onActivity(
                    activity -> {

                        UsageStatisticsStore store =
                                getPrivateField(
                                        activity,
                                        "usageStatisticsStore",
                                        UsageStatisticsStore.class);

                        UsageStatisticsStore.TimeProfile profile =
                                store.getCurrentTimeProfile(
                                        8,
                                        21);

                        UsageStatisticsStore.SortingSnapshot sorting =
                                store.getSortingSnapshot(
                                        profile,
                                        8,
                                        21);

                        assertEquals(
                                1,
                                sorting.getProfileSectionLaunches()
                                        .get("favorites")
                                        .intValue());
                    });
        }
    }

    @Test
    public void categoryManagementUsesGlobalGridMode() {

        Context context =
                ApplicationProvider
                        .getApplicationContext();

        assertTrue(
                context.getSharedPreferences(
                                "overview_display",
                                Context.MODE_PRIVATE)
                        .edit()
                        .putBoolean(
                                "grid_mode",
                                true)
                        .putInt(
                                "grid_columns",
                                4)
                        .commit());

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
                                        "Work",
                                        "💼"));

                        activity.findViewById(
                                        R.id.navCategories)
                                .performClick();

                        CategoryAdapter adapter =
                                getPrivateField(
                                        activity,
                                        "categoryAdapter",
                                        CategoryAdapter.class);

                        GridLayoutManager layoutManager =
                                getPrivateField(
                                        activity,
                                        "categoryGridLayoutManager",
                                        GridLayoutManager.class);

                        assertTrue(
                                adapter.isGridMode());

                        assertEquals(
                                4,
                                layoutManager
                                        .getSpanCount());

                        assertEquals(
                                1,
                                adapter.getItemCount());
                    });
        }
    }

    @Test
    public void statisticsCountCategoriesOnlyForCategoryLaunches() {

        try (ActivityScenario<MainActivity> scenario =
                     ActivityScenario.launch(
                             MainActivity.class)) {

            scenario.onActivity(
                    activity -> {

                        UsageStatisticsStore store =
                                getPrivateField(
                                        activity,
                                        "usageStatisticsStore",
                                        UsageStatisticsStore.class);

                        AppEntry app =
                                new AppEntry(
                                        "Mail",
                                        "com.example.mail",
                                        new ResolveInfo(),
                                        null,
                                        1);

                        CategoryStore categoryStore =
                                getPrivateField(
                                        activity,
                                        "categoryStore",
                                        CategoryStore.class);

                        categoryStore.setAssignedCategoryIds(
                                "com.example.mail",
                                java.util.Set.of(
                                        "work"));

                        ShortcutEntry shortcut =
                                new ShortcutEntry(
                                        "shortcut-weather",
                                        "Weather",
                                        ShortcutEntry.TYPE_WEBSITE,
                                        "https://example.com",
                                        java.util.Set.of(
                                                "work"),
                                        false);

                        OverviewSection category =
                                new OverviewSection(
                                        "category:work",
                                        "Work",
                                        "Empty");

                        OverviewSection favorites =
                                new OverviewSection(
                                        "favorites",
                                        "Favorites",
                                        "Empty");

                        invoke(
                                activity,
                                "recordAppLaunch",
                                new Class<?>[] {
                                        OverviewSection.class,
                                        AppEntry.class
                                },
                                new Object[] {
                                        category,
                                        app
                                });

                        invoke(
                                activity,
                                "recordAppLaunch",
                                new Class<?>[] {
                                        OverviewSection.class,
                                        AppEntry.class
                                },
                                new Object[] {
                                        favorites,
                                        app
                                });

                        invoke(
                                activity,
                                "recordShortcutLaunch",
                                new Class<?>[] {
                                        OverviewSection.class,
                                        ShortcutEntry.class
                                },
                                new Object[] {
                                        category,
                                        shortcut
                                });

                        invoke(
                                activity,
                                "recordShortcutLaunch",
                                new Class<?>[] {
                                        OverviewSection.class,
                                        ShortcutEntry.class
                                },
                                new Object[] {
                                        favorites,
                                        shortcut
                                });

                        UsageStatisticsStore.Snapshot snapshot =
                                store.getSnapshot(
                                        UsageStatisticsStore.Period.ONE_MONTH);

                        assertEquals(
                                2,
                                snapshot.getAppCounts()
                                        .get(
                                                "com.example.mail")
                                        .intValue());

                        assertEquals(
                                2,
                                snapshot.getShortcutCounts()
                                        .get(
                                                "shortcut-weather")
                                        .intValue());

                        assertEquals(
                                2,
                                snapshot.getCategoryCounts()
                                        .get(
                                                "work")
                                        .intValue());

                        assertEquals(
                                4,
                                snapshot.getTotalLaunches());

                        UsageStatisticsStore.TimeProfile profile =
                                store.getCurrentTimeProfile(
                                        8,
                                        21);

                        UsageStatisticsStore.SortingSnapshot sorting =
                                store.getSortingSnapshot(
                                        profile,
                                        8,
                                        21);

                        /*
                         * Vier erfolgreiche Starts:
                         * App direkt aus Work,
                         * App aus Favoriten,
                         * Shortcut direkt aus Work,
                         * Shortcut aus Favoriten.
                         *
                         * Da beide Einträge Work zugeordnet sind,
                         * zählt Work für die Sortierung bei allen vier.
                         * Die sichtbare Kategorienstatistik oben
                         * bleibt dagegen bei den zwei direkten Starts.
                         */
                        assertEquals(
                                4,
                                sorting.getProfileSectionLaunches()
                                        .get("category:work")
                                        .intValue());

                        assertEquals(
                                2,
                                sorting.getProfileSectionLaunches()
                                        .get("favorites")
                                        .intValue());
                    });
        }
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void statisticsSummaryKeepsExplicitTopLimitAboveAppCount() {

        try (ActivityScenario<MainActivity> scenario =
                     ActivityScenario.launch(
                             MainActivity.class)) {

            scenario.onActivity(
                    activity -> {

                        List currentApps =
                                getPrivateField(
                                        activity,
                                        "apps",
                                        List.class);

                        currentApps.clear();

                        for (int index = 0;
                             index < 8;
                             index++) {

                            currentApps.add(
                                    new AppEntry(
                                            "App " + index,
                                            "com.example.app" + index,
                                            new ResolveInfo(),
                                            null,
                                            1));
                        }

                        setPrivateField(
                                activity,
                                "appsLoaded",
                                true);

                        setPrivateField(
                                activity,
                                "statisticsTopLimit",
                                10);

                        invoke(
                                activity,
                                "updateStatisticsSummary",
                                new Class<?>[0],
                                new Object[0]);

                        TextView summary =
                                getPrivateField(
                                        activity,
                                        "statisticsSummaryLabel",
                                        TextView.class);

                        assertEquals(
                                activity.getString(
                                        R.string.statistics_summary_top_one_month,
                                        10,
                                        8),
                                summary.getText()
                                        .toString());

                        setPrivateField(
                                activity,
                                "statisticsTopLimit",
                                -1);

                        invoke(
                                activity,
                                "updateStatisticsSummary",
                                new Class<?>[0],
                                new Object[0]);

                        assertEquals(
                                activity.getString(
                                        R.string.statistics_summary_all_one_month,
                                        8),
                                summary.getText()
                                        .toString());
                    });
        }
    }

    @Test
    public void statisticsResetKeepsDisplaySettings() {

        Context context =
                ApplicationProvider
                        .getApplicationContext();

        SharedPreferences display =
                context.getSharedPreferences(
                        "statistics_display",
                        Context.MODE_PRIVATE);

        assertTrue(
                display.edit()
                        .putString(
                                "statistics_period",
                                UsageStatisticsStore.Period
                                        .THREE_MONTHS
                                        .name())
                        .putInt(
                                "statistics_top_limit",
                                25)
                        .putString(
                                "statistics_card_order_v2",
                                "top_apps,total")
                        .commit());

        try (ActivityScenario<MainActivity> scenario =
                     ActivityScenario.launch(
                             MainActivity.class)) {

            scenario.onActivity(
                    activity -> {

                        UsageStatisticsStore store =
                                getPrivateField(
                                        activity,
                                        "usageStatisticsStore",
                                        UsageStatisticsStore.class);

                        store.recordAppLaunch(
                                "com.example.reset",
                                "work");

                        assertEquals(
                                1,
                                store.getSnapshot(
                                                UsageStatisticsStore.Period.ONE_YEAR)
                                        .getTotalLaunches());

                        invoke(
                                activity,
                                "showStatisticsResetDialog",
                                new Class<?>[0],
                                new Object[0]);

                        AlertDialog dialog =
                                latestDialog();

                        assertNotNull(
                                dialog.getButton(
                                        AlertDialog.BUTTON_POSITIVE));

                        assertNotNull(
                                dialog.getButton(
                                        AlertDialog.BUTTON_NEGATIVE));

                        dialog.dismiss();

                        invoke(
                                activity,
                                "resetStatistics",
                                new Class<?>[0],
                                new Object[0]);

                        UsageStatisticsStore.Snapshot snapshot =
                                store.getSnapshot(
                                        UsageStatisticsStore.Period.ONE_YEAR);

                        assertTrue(
                                snapshot.getAppCounts()
                                        .isEmpty());

                        assertTrue(
                                snapshot.getShortcutCounts()
                                        .isEmpty());

                        assertTrue(
                                snapshot.getCategoryCounts()
                                        .isEmpty());

                        assertEquals(
                                UsageStatisticsStore.Period
                                        .THREE_MONTHS
                                        .name(),
                                display.getString(
                                        "statistics_period",
                                        null));

                        assertEquals(
                                25,
                                display.getInt(
                                        "statistics_top_limit",
                                        -1));

                        assertEquals(
                                "top_apps,total",
                                display.getString(
                                        "statistics_card_order_v2",
                                        null));
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

    @Test
    public void appAssignmentFollowsGlobalViewAfterRecreation() {

        Context context =
                ApplicationProvider
                        .getApplicationContext();

        SharedPreferences display =
                context.getSharedPreferences(
                        "overview_display",
                        Context.MODE_PRIVATE);

        assertTrue(
                display.edit()
                        .putBoolean(
                                "grid_mode",
                                true)
                        .putInt(
                                "grid_columns",
                                5)
                        .commit());

        try (ActivityScenario<MainActivity> scenario =
                     ActivityScenario.launch(
                             MainActivity.class)) {

            scenario.onActivity(activity -> {

                invoke(
                        activity,
                        "showApps",
                        new Class<?>[0],
                        new Object[0]);

                AppAdapter adapter =
                        getPrivateField(
                                activity,
                                "appAdapter",
                                AppAdapter.class);

                GridLayoutManager layout =
                        getPrivateField(
                                activity,
                                "appGridLayoutManager",
                                GridLayoutManager.class);

                assertTrue(
                        adapter.isGridMode());

                assertEquals(
                        5,
                        layout.getSpanCount());

                AppAdapter.AppViewHolder holder =
                        adapter.onCreateViewHolder(
                                new FrameLayout(activity),
                                adapter.getItemViewType(0));

                assertTrue(
                        holder.gridTile);

                assertNotNull(
                        holder.favorite);
            });

            scenario.recreate();

            scenario.onActivity(activity -> {

                AppAdapter adapter =
                        getPrivateField(
                                activity,
                                "appAdapter",
                                AppAdapter.class);

                GridLayoutManager layout =
                        getPrivateField(
                                activity,
                                "appGridLayoutManager",
                                GridLayoutManager.class);

                assertTrue(
                        adapter.isGridMode());

                assertEquals(
                        5,
                        layout.getSpanCount());

                invoke(
                        activity,
                        "showOverview",
                        new Class<?>[0],
                        new Object[0]);

                invoke(
                        activity,
                        "setOverviewGridMode",
                        new Class<?>[] {
                                boolean.class
                        },
                        new Object[] {
                                false
                        });

                invoke(
                        activity,
                        "showApps",
                        new Class<?>[0],
                        new Object[0]);

                assertTrue(
                        !adapter.isGridMode());

                assertEquals(
                        1,
                        layout.getSpanCount());
            });
        }
    }

    @Test
    public void appAssignmentUsesUpdatedGlobalColumns() {

        Context context =
                ApplicationProvider
                        .getApplicationContext();

        SharedPreferences display =
                context.getSharedPreferences(
                        "overview_display",
                        Context.MODE_PRIVATE);

        assertTrue(
                display.edit()
                        .putBoolean(
                                "grid_mode",
                                true)
                        .putInt(
                                "grid_columns",
                                5)
                        .commit());

        try (ActivityScenario<MainActivity> scenario =
                     ActivityScenario.launch(
                             MainActivity.class)) {

            scenario.onActivity(activity -> {

                invoke(
                        activity,
                        "showOverviewColumnsDialog",
                        new Class<?>[0],
                        new Object[0]);

                ListView choices =
                        latestDialog().getListView();

                assertNotNull(choices);

                choices.performItemClick(
                        null,
                        0,
                        choices.getAdapter()
                                .getItemId(0));

                assertEquals(
                        3,
                        display.getInt(
                                "grid_columns",
                                -1));

                invoke(
                        activity,
                        "showApps",
                        new Class<?>[0],
                        new Object[0]);

                AppAdapter adapter =
                        getPrivateField(
                                activity,
                                "appAdapter",
                                AppAdapter.class);

                GridLayoutManager layout =
                        getPrivateField(
                                activity,
                                "appGridLayoutManager",
                                GridLayoutManager.class);

                assertTrue(
                        adapter.isGridMode());

                assertEquals(
                        3,
                        layout.getSpanCount());

                assertTrue(
                        activity.findViewById(
                                        R.id.buttonFilterApps)
                                .performClick());

                assertTrue(
                        getPrivateField(
                                activity,
                                "showOnlyUnassignedApps",
                                Boolean.class));

                EditText search =
                        activity.findViewById(
                                R.id.appSearch);

                search.setText("example");

                assertEquals(
                        "example",
                        getPrivateField(
                                activity,
                                "assignmentSearchQuery",
                                String.class));
            });
        }
    }

    @Test
    public void shortTapAssignsInListAndGridView() {

        Context context =
                ApplicationProvider
                        .getApplicationContext();

        CategoryStore store =
                new CategoryStore(context);

        assertTrue(
                store.addCategory(
                        "Tap test",
                        "📁"));

        try (ActivityScenario<MainActivity> scenario =
                     ActivityScenario.launch(
                             MainActivity.class)) {

            scenario.onActivity(activity -> {

                AppAdapter original =
                        getPrivateField(
                                activity,
                                "appAdapter",
                                AppAdapter.class);

                AppAdapter.OnAppClickListener click =
                        getPrivateField(
                                original,
                                "clickListener",
                                AppAdapter.OnAppClickListener.class);

                AppAdapter adapter =
                        new AppAdapter(
                                getPrivateField(
                                        activity,
                                        "favoritesStore",
                                        FavoritesStore.class),
                                getPrivateField(
                                        activity,
                                        "categoryStore",
                                        CategoryStore.class),
                                getPrivateField(
                                        activity,
                                        "appIconLoader",
                                        AppIconLoader.class),
                                click);

                ResolveInfo info =
                        new ResolveInfo();

                info.activityInfo =
                        new ActivityInfo();

                info.activityInfo.name =
                        "ExampleActivity";

                info.activityInfo.packageName =
                        "com.example.tap";

                info.activityInfo.applicationInfo =
                        new ApplicationInfo();

                info.activityInfo.applicationInfo.packageName =
                        "com.example.tap";

                AppEntry entry =
                        new AppEntry(
                                "Example",
                                "com.example.tap",
                                info,
                                null,
                                1);

                adapter.submitList(
                        java.util.Collections.singletonList(
                                entry));

                for (boolean grid :
                        new boolean[] {
                                false,
                                true
                        }) {

                    adapter.setGridMode(
                            grid);

                    AppAdapter.AppViewHolder holder =
                            adapter.onCreateViewHolder(
                                    new FrameLayout(
                                            activity),
                                    adapter.getItemViewType(
                                            0));

                    adapter.onBindViewHolder(
                            holder,
                            0);

                    assertEquals(
                            grid,
                            holder.gridTile);

                    assertTrue(
                            !holder.itemView.isLongClickable());

                    int expectedSize =
                            grid
                                    ? Math.round(
                                            32f
                                            * activity.getResources()
                                                    .getDisplayMetrics()
                                                    .density)
                                    : activity.getResources()
                                            .getDimensionPixelSize(
                                                    R.dimen.icon_button_size);

                    assertEquals(
                            expectedSize,
                            holder.favorite
                                    .getLayoutParams()
                                    .width);

                    assertTrue(
                            holder.itemView
                                    .performClick());

                    AlertDialog dialog =
                            latestDialog();

                    assertTrue(
                            dialog.isShowing());

                    assertNotNull(
                            dialog.getListView());

                    assertEquals(
                            1,
                            dialog.getListView()
                                    .getCount());

                    dialog.dismiss();
                }
            });
        }
    }

    @Test
    public void gridStarAndDragHandleSharePosition() {

        try (ActivityScenario<MainActivity> scenario =
                     ActivityScenario.launch(
                             MainActivity.class)) {

            scenario.onActivity(activity -> {

                int size =
                        activity.getResources()
                                .getDimensionPixelSize(
                                        R.dimen.icon_button_size);

                float density =
                        activity.getResources()
                                .getDisplayMetrics()
                                .density;

                int gridSize =
                        Math.round(32f * density);

                int offset =
                        -Math.round(2f * density);

                int starPadding =
                        Math.round(7f * density);

                int handlePadding =
                        Math.round(8f * density);

                FrameLayout overviewTile =
                        (FrameLayout)
                                LayoutInflater.from(
                                                activity)
                                        .inflate(
                                                R.layout.item_overview_grid_entry,
                                                new FrameLayout(
                                                        activity),
                                                false);

                ImageButton star =
                        overviewTile.findViewById(
                                R.id.appFavorite);

                ImageView handle =
                        overviewTile.findViewById(
                                R.id.overviewItemDragHandle);

                assertNotNull(star);
                assertNotNull(handle);

                FrameLayout.LayoutParams starParams =
                        (FrameLayout.LayoutParams)
                                star.getLayoutParams();

                FrameLayout.LayoutParams handleParams =
                        (FrameLayout.LayoutParams)
                                handle.getLayoutParams();

                assertEquals(gridSize, starParams.width);
                assertEquals(gridSize, starParams.height);

                assertEquals(gridSize, handleParams.width);
                assertEquals(gridSize, handleParams.height);

                assertEquals(
                        Gravity.TOP | Gravity.END,
                        starParams.gravity);

                assertEquals(
                        starParams.gravity,
                        handleParams.gravity);

                assertEquals(
                        offset,
                        starParams.topMargin);

                assertEquals(
                        offset,
                        starParams.getMarginEnd());

                assertEquals(
                        starParams.topMargin,
                        handleParams.topMargin);

                assertEquals(
                        starParams.getMarginEnd(),
                        handleParams.getMarginEnd());

                assertEquals(
                        starPadding,
                        star.getPaddingLeft());

                assertEquals(
                        starPadding,
                        star.getPaddingRight());

                assertEquals(
                        handlePadding,
                        handle.getPaddingLeft());

                assertEquals(
                        View.VISIBLE,
                        star.getVisibility());

                assertEquals(
                        View.GONE,
                        handle.getVisibility());

                assertEquals(
                        0,
                        overviewTile.getChildAt(0)
                                .getPaddingTop());

                FrameLayout assignmentTile =
                        (FrameLayout)
                                LayoutInflater.from(
                                                activity)
                                        .inflate(
                                                R.layout.item_app_grid,
                                                new FrameLayout(
                                                        activity),
                                                false);

                ImageButton assignmentStar =
                        assignmentTile.findViewById(
                                R.id.appFavorite);

                assertNotNull(assignmentStar);

                FrameLayout.LayoutParams assignmentParams =
                        (FrameLayout.LayoutParams)
                                assignmentStar.getLayoutParams();

                assertEquals(
                        gridSize,
                        assignmentParams.width);

                assertEquals(
                        gridSize,
                        assignmentParams.height);

                assertEquals(
                        starParams.gravity,
                        assignmentParams.gravity);

                assertEquals(
                        starParams.topMargin,
                        assignmentParams.topMargin);

                assertEquals(
                        starParams.getMarginEnd(),
                        assignmentParams.getMarginEnd());

                assertEquals(
                        starPadding,
                        assignmentStar.getPaddingLeft());

                assertEquals(
                        0,
                        assignmentTile.getChildAt(0)
                                .getPaddingTop());

                assertTrue(
                        assignmentTile.findViewById(
                                R.id.overviewItemDragHandle)
                                == null);

                View listTile =
                        LayoutInflater.from(activity)
                                .inflate(
                                        R.layout.item_app,
                                        new FrameLayout(
                                                activity),
                                        false);

                ImageButton listStar =
                        listTile.findViewById(
                                R.id.appFavorite);

                ImageView listHandle =
                        listTile.findViewById(
                                R.id.overviewItemDragHandle);

                assertEquals(
                        size,
                        listStar.getLayoutParams()
                                .width);

                assertEquals(
                        size,
                        listHandle.getLayoutParams()
                                .width);

                assertEquals(
                        View.GONE,
                        listHandle.getVisibility());
            });
        }
    }

    @Test
    public void gridSortingReplacesStarWithHandle() {

        try (ActivityScenario<MainActivity> scenario =
                     ActivityScenario.launch(
                             MainActivity.class)) {

            scenario.onActivity(activity -> {

                OverviewAdapter adapter =
                        getPrivateField(
                                activity,
                                "overviewAdapter",
                                OverviewAdapter.class);

                OverviewSection section =
                        new OverviewSection(
                                "category:layout-test",
                                "Layout test",
                                "Empty",
                                "Layout test",
                                "📁");

                ResolveInfo info =
                        new ResolveInfo();

                info.activityInfo =
                        new ActivityInfo();

                info.activityInfo.name =
                        "ExampleActivity";

                info.activityInfo.packageName =
                        "com.example.layout";

                info.activityInfo.applicationInfo =
                        new ApplicationInfo();

                info.activityInfo.applicationInfo.packageName =
                        "com.example.layout";

                AppEntry entry =
                        new AppEntry(
                                "Layout test",
                                "com.example.layout",
                                info,
                                null,
                                1);

                View tile =
                        LayoutInflater.from(activity)
                                .inflate(
                                        R.layout.item_overview_grid_entry,
                                        new FrameLayout(
                                                activity),
                                        false);

                OverviewAdapter.EntryViewHolder holder =
                        new OverviewAdapter.EntryViewHolder(
                                tile,
                                true);

                Class<?>[] arguments = {
                        OverviewAdapter.EntryViewHolder.class,
                        OverviewSection.class,
                        AppEntry.class
                };

                Object[] values = {
                        holder,
                        section,
                        entry
                };

                invoke(
                        adapter,
                        "bindApp",
                        arguments,
                        values);

                assertEquals(
                        View.VISIBLE,
                        holder.favorite.getVisibility());

                assertEquals(
                        View.GONE,
                        holder.itemDragHandle.getVisibility());

                setPrivateField(
                        adapter,
                        "itemSortSectionId",
                        section.id);

                invoke(
                        adapter,
                        "bindApp",
                        arguments,
                        values);

                assertEquals(
                        View.GONE,
                        holder.favorite.getVisibility());

                assertEquals(
                        View.VISIBLE,
                        holder.itemDragHandle.getVisibility());

                setPrivateField(
                        adapter,
                        "itemSortSectionId",
                        null);

                invoke(
                        adapter,
                        "bindApp",
                        arguments,
                        values);

                assertEquals(
                        View.VISIBLE,
                        holder.favorite.getVisibility());

                assertEquals(
                        View.GONE,
                        holder.itemDragHandle.getVisibility());
            });
        }
    }

    @Test
    public void openGridCategorySurvivesActivityRecreation() {

        Context context =
                ApplicationProvider
                        .getApplicationContext();

        CategoryStore store =
                new CategoryStore(context);

        assertTrue(
                store.addCategory(
                        "Recreation",
                        "📁"));

        String categoryId =
                store.getCategories().get(0).id;

        SharedPreferences display =
                context.getSharedPreferences(
                        "overview_display",
                        Context.MODE_PRIVATE);

        assertTrue(
                display.edit()
                        .putBoolean(
                                "grid_mode",
                                true)
                        .putInt(
                                "grid_columns",
                                3)
                        .putInt(
                                "section_grid_columns_category:"
                                        + categoryId,
                                5)
                        .commit());

        try (ActivityScenario<MainActivity> scenario =
                     ActivityScenario.launch(
                             MainActivity.class)) {

            scenario.onActivity(activity -> {

                OverviewSection section =
                        findCategorySection(
                                activity,
                                categoryId);

                invoke(
                        activity,
                        "openGridSection",
                        new Class<?>[] {
                                OverviewSection.class
                        },
                        new Object[] {
                                section
                        });

                OverviewAdapter adapter =
                        getPrivateField(
                                activity,
                                "overviewAdapter",
                                OverviewAdapter.class);

                GridLayoutManager layout =
                        getPrivateField(
                                activity,
                                "overviewGridLayoutManager",
                                GridLayoutManager.class);

                assertEquals(
                        section.id,
                        adapter.getGridOpenSectionId());

                assertEquals(
                        5,
                        layout.getSpanCount());
            });

            scenario.recreate();

            scenario.onActivity(activity -> {

                OverviewAdapter adapter =
                        getPrivateField(
                                activity,
                                "overviewAdapter",
                                OverviewAdapter.class);

                GridLayoutManager layout =
                        getPrivateField(
                                activity,
                                "overviewGridLayoutManager",
                                GridLayoutManager.class);

                assertEquals(
                        "category:" + categoryId,
                        adapter.getGridOpenSectionId());

                assertEquals(
                        5,
                        layout.getSpanCount());

                invoke(
                        activity,
                        "showOverview",
                        new Class<?>[0],
                        new Object[0]);

                assertEquals(
                        null,
                        adapter.getGridOpenSectionId());

                assertEquals(
                        3,
                        layout.getSpanCount());

                assertTrue(
                        adapter.isGridOverview());
            });
        }
    }

    @Test
    public void deletedCategoryRemovesColumnOverride() {

        Context context =
                ApplicationProvider
                        .getApplicationContext();

        CategoryStore store =
                new CategoryStore(context);

        assertTrue(
                store.addCategory(
                        "Temporary",
                        "📁"));

        String categoryId =
                store.getCategories().get(0).id;

        SharedPreferences display =
                context.getSharedPreferences(
                        "overview_display",
                        Context.MODE_PRIVATE);

        String settingKey =
                "section_grid_columns_category:"
                        + categoryId;

        assertTrue(
                display.edit()
                        .putInt(
                                settingKey,
                                5)
                        .commit());

        try (ActivityScenario<MainActivity> scenario =
                     ActivityScenario.launch(
                             MainActivity.class)) {

            scenario.onActivity(activity -> {

                CategoryStore activityStore =
                        getPrivateField(
                                activity,
                                "categoryStore",
                                CategoryStore.class);

                CategoryEntry category =
                        activityStore.getCategories()
                                .stream()
                                .filter(
                                        entry ->
                                                categoryId.equals(entry.id))
                                .findFirst()
                                .orElseThrow(
                                        AssertionError::new);

                assertEquals(
                        categoryId,
                        category.id);

                invoke(
                        activity,
                        "showDeleteCategoryDialog",
                        new Class<?>[] {
                                CategoryEntry.class
                        },
                        new Object[] {
                                category
                        });

                assertTrue(
                        latestDialog()
                                .getButton(
                                        AlertDialog.BUTTON_POSITIVE)
                                .performClick());
            });

            Shadows.shadowOf(
                    Looper.getMainLooper())
                    .idle();

            scenario.onActivity(activity -> {

                CategoryStore activityStore =
                        getPrivateField(
                                activity,
                                "categoryStore",
                                CategoryStore.class);

                boolean deletedCategoryPresent =
                        activityStore.getCategories()
                                .stream()
                                .anyMatch(
                                        entry ->
                                                categoryId.equals(entry.id));

                assertTrue(
                        !deletedCategoryPresent);

                assertTrue(
                        !display.contains(
                                settingKey));
            });
        }
    }

    @Test
    public void globalSelectionFollowsChangedGlobalColumns() {

        Context context =
                ApplicationProvider
                        .getApplicationContext();

        CategoryStore store =
                new CategoryStore(context);

        assertTrue(
                store.addCategory(
                        "Global setting",
                        "📁"));

        String categoryId =
                store.getCategories().get(0).id;

        String settingKey =
                "section_grid_columns_category:"
                        + categoryId;

        SharedPreferences display =
                context.getSharedPreferences(
                        "overview_display",
                        Context.MODE_PRIVATE);

        assertTrue(
                display.edit()
                        .putBoolean(
                                "grid_mode",
                                true)
                        .putInt(
                                "grid_columns",
                                4)
                        .putInt(
                                settingKey,
                                5)
                        .commit());

        try (ActivityScenario<MainActivity> scenario =
                     ActivityScenario.launch(
                             MainActivity.class)) {

            scenario.onActivity(activity -> {

                OverviewSection section =
                        findCategorySection(
                                activity,
                                categoryId);

                GridLayoutManager layout =
                        getPrivateField(
                                activity,
                                "overviewGridLayoutManager",
                                GridLayoutManager.class);

                invoke(
                        activity,
                        "openGridSection",
                        new Class<?>[] {
                                OverviewSection.class
                        },
                        new Object[] {
                                section
                        });

                assertEquals(
                        5,
                        layout.getSpanCount());

                invoke(
                        activity,
                        "showSectionGridColumnsDialog",
                        new Class<?>[] {
                                OverviewSection.class
                        },
                        new Object[] {
                                section
                        });

                ListView choices =
                        latestDialog().getListView();

                assertNotNull(choices);

                choices.performItemClick(
                        null,
                        0,
                        choices.getAdapter()
                                .getItemId(0));

                assertTrue(
                        !display.contains(
                                settingKey));

                assertEquals(
                        4,
                        layout.getSpanCount());

                invoke(
                        activity,
                        "showOverview",
                        new Class<?>[0],
                        new Object[0]);

                invoke(
                        activity,
                        "showOverviewColumnsDialog",
                        new Class<?>[0],
                        new Object[0]);

                ListView globalChoices =
                        latestDialog().getListView();

                assertNotNull(globalChoices);

                globalChoices.performItemClick(
                        null,
                        0,
                        globalChoices.getAdapter()
                                .getItemId(0));

                assertEquals(
                        3,
                        display.getInt(
                                "grid_columns",
                                -1));

                OverviewSection updatedSection =
                        findCategorySection(
                                activity,
                                categoryId);

                invoke(
                        activity,
                        "openGridSection",
                        new Class<?>[] {
                                OverviewSection.class
                        },
                        new Object[] {
                                updatedSection
                        });

                assertEquals(
                        3,
                        layout.getSpanCount());

                assertTrue(
                        !display.contains(
                                settingKey));
            });
        }
    }

    private static OverviewSection findCategorySection(
            MainActivity activity,
            String categoryId) {

        List<?> sections =
                getPrivateField(
                        activity,
                        "overviewSections",
                        List.class);

        for (Object entry : sections) {
            OverviewSection section =
                    (OverviewSection) entry;

            if (("category:" + categoryId)
                    .equals(section.id)) {

                return section;
            }
        }

        throw new AssertionError(
                "Kategorie in der Übersicht nicht gefunden.");
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

    private static void setPrivateField(
            Object target,
            String name,
            Object value) {

        try {
            Field field =
                    target.getClass()
                            .getDeclaredField(
                                    name);

            field.setAccessible(true);

            field.set(
                    target,
                    value);

        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
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
