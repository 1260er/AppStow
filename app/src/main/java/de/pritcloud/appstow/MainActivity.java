package de.pritcloud.appstow;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.DisplayCutout;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private static final int REQUEST_CREATE_BACKUP = 1001;
    private static final int REQUEST_RESTORE_BACKUP = 1002;

    private DrawerLayout drawerLayout;
    private TextView pageTitle;
    private TextView pageMessage;
    private RecyclerView appList;
    private RecyclerView overviewList;
    private RecyclerView categoryList;
    private View categoryManagement;
    private TextView categoryEmptyMessage;

    private RecyclerView shortcutList;
    private View shortcutManagement;
    private TextView shortcutEmptyMessage;
    private View backupManagement;
    private ScrollView helpManagement;
    private View helpShortcutSection;
    private EditText appSearch;
    private View appSearchContainer;
    private ImageButton appSearchClear;
    private ImageButton overviewSortButton;
    private ImageButton appFilterButton;
    private ImageButton shortcutHelpButton;
    private ImageButton topNavigationButton;
    private boolean showingOverview;
    private boolean showOnlyUnassignedApps;
    private boolean appsLoading;
    private boolean appsLoaded;

    private final ExecutorService appLoader =
            Executors.newSingleThreadExecutor();

    private AppAdapter appAdapter;
    private OverviewAdapter overviewAdapter;
    private FavoritesStore favoritesStore;
    private CategoryStore categoryStore;
    private CategoryAdapter categoryAdapter;

    private ShortcutStore shortcutStore;
    private ShortcutAdapter shortcutAdapter;
    private OverviewOrderStore overviewOrderStore;
    private SectionItemOrderStore sectionItemOrderStore;
    private ItemTouchHelper overviewItemTouchHelper;

    private final List<AppEntry> apps = new ArrayList<>();
    private final List<OverviewSection> overviewSections =
            new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View contentRoot = findViewById(android.R.id.content);
        contentRoot.setOnApplyWindowInsetsListener((view, insets) -> {
            int left;
            int top;
            int right;
            int bottom;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                android.graphics.Insets safeInsets = insets.getInsets(
                        android.view.WindowInsets.Type.systemBars()
                                | android.view.WindowInsets.Type.displayCutout());

                left = safeInsets.left;
                top = safeInsets.top;
                right = safeInsets.right;
                bottom = safeInsets.bottom;
            } else {
                left = insets.getSystemWindowInsetLeft();
                top = insets.getSystemWindowInsetTop();
                right = insets.getSystemWindowInsetRight();
                bottom = insets.getSystemWindowInsetBottom();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    DisplayCutout cutout = insets.getDisplayCutout();

                    if (cutout != null) {
                        left = Math.max(left, cutout.getSafeInsetLeft());
                        top = Math.max(top, cutout.getSafeInsetTop());
                        right = Math.max(right, cutout.getSafeInsetRight());
                        bottom = Math.max(bottom, cutout.getSafeInsetBottom());
                    }
                }
            }

            view.setPadding(left, top, right, bottom);
            return insets;
        });

        drawerLayout = findViewById(R.id.drawerLayout);
        pageTitle = findViewById(R.id.pageTitle);
        pageMessage = findViewById(R.id.pageMessage);
        appList = findViewById(R.id.appList);
        overviewList = findViewById(R.id.overviewList);
        categoryList = findViewById(R.id.categoryList);
        categoryManagement = findViewById(R.id.categoryManagement);
        categoryEmptyMessage = findViewById(R.id.categoryEmptyMessage);

        shortcutList =
                findViewById(R.id.shortcutList);
        shortcutManagement =
                findViewById(R.id.shortcutManagement);
        shortcutEmptyMessage =
                findViewById(R.id.shortcutEmptyMessage);

        backupManagement =
                findViewById(R.id.backupManagement);

        helpManagement =
                findViewById(R.id.helpManagement);

        helpShortcutSection =
                findViewById(R.id.helpShortcutSection);

        appSearch = findViewById(R.id.appSearch);
        appSearchContainer = findViewById(R.id.appSearchContainer);
        appSearchClear = findViewById(R.id.appSearchClear);
        overviewSortButton =
                findViewById(R.id.buttonSortOverview);

        appFilterButton =
                findViewById(R.id.buttonFilterApps);

        shortcutHelpButton =
                findViewById(R.id.buttonShortcutHelp);

        topNavigationButton =
                findViewById(R.id.buttonOpenMenu);

        favoritesStore = new FavoritesStore(this);
        categoryStore = new CategoryStore(this);
        shortcutStore = new ShortcutStore(this);

        overviewOrderStore =
                new OverviewOrderStore(this);

        sectionItemOrderStore =
                new SectionItemOrderStore(this);

        categoryAdapter = new CategoryAdapter(
                new CategoryAdapter.Listener() {
                    @Override
                    public void onRename(CategoryEntry category) {
                        showRenameCategoryDialog(category);
                    }

                    @Override
                    public void onDelete(CategoryEntry category) {
                        showDeleteCategoryDialog(category);
                    }
                });

        categoryList.setLayoutManager(
                new LinearLayoutManager(this));
        categoryList.setAdapter(categoryAdapter);

        findViewById(R.id.categoryAddButton)
                .setOnClickListener(v ->
                        showAddCategoryDialog());

        shortcutAdapter =
                new ShortcutAdapter(
                        categoryStore,
                        new ShortcutAdapter.Listener() {
                            @Override
                            public void onEdit(
                                    ShortcutEntry shortcut) {

                                showShortcutEditor(
                                        shortcut);
                            }

                            @Override
                            public void onDelete(
                                    ShortcutEntry shortcut) {

                                showDeleteShortcutDialog(
                                        shortcut);
                            }

                            @Override
                            public void onFavorite(
                                    ShortcutEntry shortcut) {

                                shortcutStore.toggleFavorite(
                                        shortcut.id);

                                refreshShortcuts();
                            }
                        });

        shortcutList.setLayoutManager(
                new LinearLayoutManager(this));

        shortcutList.setAdapter(
                shortcutAdapter);

        findViewById(R.id.shortcutAddButton)
                .setOnClickListener(v ->
                        showShortcutEditor(null));

        findViewById(R.id.backupCreateButton)
                .setOnClickListener(v ->
                        createBackup());

        findViewById(R.id.backupRestoreButton)
                .setOnClickListener(v ->
                        selectBackupForRestore());

        appAdapter = new AppAdapter(
                getPackageManager(),
                favoritesStore,
                categoryStore,
                this::launchApp,
                this::handleAppLongClick);

        appList.setLayoutManager(new LinearLayoutManager(this));
        appList.setAdapter(appAdapter);
        appList.setHasFixedSize(true);

        rebuildOverviewSections();

        overviewAdapter =
                new OverviewAdapter(
                        overviewSections,
                        getPackageManager(),
                        favoritesStore,
                        categoryStore,
                        shortcutStore,
                        sectionItemOrderStore,
                        this::launchApp,
                        this::launchShortcut,
                        this::handleAppLongClick,
                        this::startOverviewDrag);

        overviewList.setLayoutManager(
                new LinearLayoutManager(this));
        overviewList.setAdapter(overviewAdapter);

        overviewItemTouchHelper =
                new ItemTouchHelper(
                        new ItemTouchHelper.SimpleCallback(
                                ItemTouchHelper.UP
                                        | ItemTouchHelper.DOWN,
                                0) {

                            @Override
                            public boolean isLongPressDragEnabled() {
                                return false;
                            }

                            @Override
                            public boolean onMove(
                                    RecyclerView recyclerView,
                                    RecyclerView.ViewHolder source,
                                    RecyclerView.ViewHolder target) {

                                int fromPosition =
                                        source.getBindingAdapterPosition();

                                int toPosition =
                                        target.getBindingAdapterPosition();

                                if (overviewAdapter.isSortMode()) {
                                    return overviewAdapter.moveSection(
                                            fromPosition,
                                            toPosition);
                                }

                                if (overviewAdapter.isItemSortMode()) {
                                    return overviewAdapter.moveItem(
                                            fromPosition,
                                            toPosition);
                                }

                                return false;
                            }

                            @Override
                            public void onSwiped(
                                    RecyclerView.ViewHolder viewHolder,
                                    int direction) {
                            }

                            @Override
                            public void clearView(
                                    RecyclerView recyclerView,
                                    RecyclerView.ViewHolder viewHolder) {

                                super.clearView(
                                        recyclerView,
                                        viewHolder);

                                if (overviewAdapter.isSortMode()) {
                                    overviewOrderStore.saveOrder(
                                            overviewSections);
                                } else if (
                                        overviewAdapter.isItemSortMode()) {

                                    overviewAdapter.saveItemOrder();
                                }
                            }
                        });

        overviewItemTouchHelper.attachToRecyclerView(
                overviewList);

        overviewSortButton.setOnClickListener(v ->
                setOverviewSortMode(
                        !overviewAdapter.isSortMode()));

        appFilterButton.setOnClickListener(v -> {
            showOnlyUnassignedApps =
                    !showOnlyUnassignedApps;

            updateAppFilterButton();
            renderApps(
                    appSearch.getText().toString());
        });

        shortcutHelpButton.setOnClickListener(v ->
                showHelp(true));

        findViewById(R.id.navApps).setOnClickListener(v ->
                showApps());

        findViewById(R.id.navCategories).setOnClickListener(v ->
                showCategoryManagement());

        findViewById(R.id.navShortcuts)
                .setOnClickListener(v ->
                        showShortcutManagement());

        findViewById(R.id.navBackup)
                .setOnClickListener(v ->
                        showBackupManagement());

        findViewById(R.id.navHelp)
                .setOnClickListener(v ->
                        showHelp(false));

        bindMenu(
                R.id.navAbout,
                "Über",
                "AppStow 0.1.0");

        appSearchClear.setOnClickListener(v -> {
            appSearch.setText("");
            appSearch.requestFocus();
        });

        appSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence text,
                    int start,
                    int count,
                    int after) {
            }

            @Override
            public void onTextChanged(
                    CharSequence text,
                    int start,
                    int before,
                    int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                appSearchClear.setVisibility(
                        editable.length() > 0
                                ? View.VISIBLE
                                : View.GONE);

                if (appSearchContainer.getVisibility()
                        == View.VISIBLE) {
                    renderApps(editable.toString());
                }
            }
        });

        showOverview();
    }

    @Override
    protected void onDestroy() {
        appLoader.shutdownNow();
        super.onDestroy();
    }

    private void rebuildOverviewSections() {
        Map<String, Boolean> expandedStates =
                new HashMap<>();

        for (OverviewSection section : overviewSections) {
            expandedStates.put(
                    section.id,
                    section.expanded);
        }

        overviewSections.clear();

        OverviewSection favorites =
                new OverviewSection(
                        "favorites",
                        getString(R.string.overview_favorites),
                        getString(R.string.overview_favorites_empty));

        favorites.expanded =
                expandedStates.getOrDefault(
                        favorites.id,
                        false);

        overviewSections.add(favorites);

        for (CategoryEntry category :
                categoryStore.getCategories()) {

            OverviewSection section =
                    new OverviewSection(
                            "category:" + category.id,
                            category.name,
                            getString(
                                    R.string.overview_category_empty));

            section.expanded =
                    expandedStates.getOrDefault(
                            section.id,
                            false);

            overviewSections.add(section);
        }

        OverviewSection shortcuts =
                new OverviewSection(
                        "shortcuts",
                        getString(R.string.overview_shortcuts),
                        getString(R.string.overview_shortcuts_empty));

        shortcuts.expanded =
                expandedStates.getOrDefault(
                        shortcuts.id,
                        false);

        overviewSections.add(shortcuts);

        applySavedOverviewOrder();
    }

    private void applySavedOverviewOrder() {
        List<String> savedOrder =
                overviewOrderStore.getOrder();

        if (savedOrder.isEmpty()) {
            return;
        }

        List<OverviewSection> defaultSections =
                new ArrayList<>(overviewSections);

        Map<String, OverviewSection> remaining =
                new HashMap<>();

        for (OverviewSection section : defaultSections) {
            remaining.put(section.id, section);
        }

        List<OverviewSection> ordered =
                new ArrayList<>();

        for (String id : savedOrder) {
            OverviewSection section =
                    remaining.remove(id);

            if (section != null) {
                ordered.add(section);
            }
        }

        for (OverviewSection section : defaultSections) {
            if (!remaining.containsKey(section.id)) {
                continue;
            }

            remaining.remove(section.id);

            if ("favorites".equals(section.id)) {
                ordered.add(0, section);
                continue;
            }

            if ("shortcuts".equals(section.id)) {
                ordered.add(section);
                continue;
            }

            int shortcutsIndex = -1;

            for (int i = 0; i < ordered.size(); i++) {
                if ("shortcuts".equals(
                        ordered.get(i).id)) {
                    shortcutsIndex = i;
                    break;
                }
            }

            if (shortcutsIndex >= 0) {
                ordered.add(
                        shortcutsIndex,
                        section);
            } else {
                ordered.add(section);
            }
        }

        overviewSections.clear();
        overviewSections.addAll(ordered);

        overviewOrderStore.saveOrder(
                overviewSections);
    }

    private void startOverviewDrag(
            RecyclerView.ViewHolder holder) {

        if (overviewItemTouchHelper != null
                && (overviewAdapter.isSortMode()
                || overviewAdapter.isItemSortMode())) {

            overviewItemTouchHelper.startDrag(
                    holder);
        }
    }

    private void setOverviewSortMode(
            boolean enabled) {

        if (!enabled
                && overviewAdapter.isSortMode()) {

            overviewOrderStore.saveOrder(
                    overviewSections);
        }

        overviewAdapter.setSortMode(enabled);

        overviewSortButton.setContentDescription(
                getString(
                        enabled
                                ? R.string.action_finish_sorting
                                : R.string.action_sort_overview));
    }

    private void updateAppFilterButton() {
        appFilterButton.setImageResource(
                showOnlyUnassignedApps
                        ? R.drawable.ic_filter_apps_active
                        : R.drawable.ic_filter_apps);

        appFilterButton.setContentDescription(
                getString(
                        showOnlyUnassignedApps
                                ? R.string.action_show_all_apps
                                : R.string.action_filter_unassigned_apps));
    }

    private void hideOverviewSortMode() {
        overviewAdapter.finishItemSortMode();
        setOverviewSortMode(false);

        overviewSortButton.setVisibility(View.GONE);
        appFilterButton.setVisibility(View.GONE);
    }

    private void showOverview() {
        setTopNavigation(true);

        shortcutHelpButton.setVisibility(View.GONE);
        appFilterButton.setVisibility(View.GONE);

        categoryManagement.setVisibility(View.GONE);
        shortcutManagement.setVisibility(View.GONE);
        backupManagement.setVisibility(View.GONE);
        helpManagement.setVisibility(View.GONE);

        setOverviewSortMode(false);
        overviewSortButton.setVisibility(View.VISIBLE);

        rebuildOverviewSections();
        overviewAdapter.setApps(apps);

        pageTitle.setText(R.string.nav_overview);

        appSearchContainer.setVisibility(View.GONE);
        appSearchClear.setVisibility(View.GONE);
        appSearch.clearFocus();

        appList.setVisibility(View.GONE);
        pageMessage.setVisibility(View.GONE);
        overviewList.setVisibility(View.VISIBLE);

        drawerLayout.closeDrawer(Gravity.END);

        loadAppsAsync();
    }

    private void showApps() {
        setTopNavigation(false);

        shortcutHelpButton.setVisibility(View.GONE);

        hideOverviewSortMode();

        appFilterButton.setVisibility(View.VISIBLE);
        updateAppFilterButton();

        categoryManagement.setVisibility(View.GONE);
        shortcutManagement.setVisibility(View.GONE);
        backupManagement.setVisibility(View.GONE);
        helpManagement.setVisibility(View.GONE);
        overviewList.setVisibility(View.GONE);

        loadAppsAsync();

        pageTitle.setText(R.string.nav_apps);

        appSearchContainer.setVisibility(View.VISIBLE);
        appSearchClear.setVisibility(
                appSearch.length() > 0
                        ? View.VISIBLE
                        : View.GONE);

        renderApps(appSearch.getText().toString());
        appList.post(appAdapter::refreshAppRows);

        drawerLayout.closeDrawer(Gravity.END);
    }

    private void showCategoryManagement() {
        setTopNavigation(false);

        shortcutHelpButton.setVisibility(View.GONE);

        hideOverviewSortMode();

        shortcutManagement.setVisibility(View.GONE);
        backupManagement.setVisibility(View.GONE);
        helpManagement.setVisibility(View.GONE);

        pageTitle.setText(R.string.nav_categories);

        appSearchContainer.setVisibility(View.GONE);
        appSearchClear.setVisibility(View.GONE);
        appSearch.clearFocus();

        overviewList.setVisibility(View.GONE);
        appList.setVisibility(View.GONE);
        pageMessage.setVisibility(View.GONE);

        categoryManagement.setVisibility(View.VISIBLE);

        refreshCategories();

        drawerLayout.closeDrawer(Gravity.END);
    }

    private List<CategoryEntry> getCategoriesInOverviewOrder() {
        List<CategoryEntry> categories =
                categoryStore.getCategories();

        List<String> savedOrder =
                overviewOrderStore.getOrder();

        if (savedOrder.isEmpty()) {
            return categories;
        }

        Map<String, CategoryEntry> remaining =
                new HashMap<>();

        for (CategoryEntry category : categories) {
            remaining.put(
                    "category:" + category.id,
                    category);
        }

        List<CategoryEntry> ordered =
                new ArrayList<>();

        for (String sectionId : savedOrder) {
            CategoryEntry category =
                    remaining.remove(sectionId);

            if (category != null) {
                ordered.add(category);
            }
        }

        for (CategoryEntry category : categories) {
            String sectionId =
                    "category:" + category.id;

            if (remaining.remove(sectionId) != null) {
                ordered.add(category);
            }
        }

        return ordered;
    }

    private void showShortcutManagement() {
        setTopNavigation(false);

        hideOverviewSortMode();

        shortcutHelpButton.setVisibility(
                View.VISIBLE);

        categoryManagement.setVisibility(
                View.GONE);

        backupManagement.setVisibility(
                View.GONE);

        helpManagement.setVisibility(
                View.GONE);

        overviewList.setVisibility(
                View.GONE);

        appList.setVisibility(
                View.GONE);

        pageMessage.setVisibility(
                View.GONE);

        appSearchContainer.setVisibility(
                View.GONE);

        appSearchClear.setVisibility(
                View.GONE);

        appSearch.clearFocus();

        pageTitle.setText(
                R.string.nav_shortcuts_manage);

        shortcutManagement.setVisibility(
                View.VISIBLE);

        refreshShortcuts();

        drawerLayout.closeDrawer(
                Gravity.END);
    }

    private void refreshShortcuts() {
        List<ShortcutEntry> shortcuts =
                shortcutStore.getShortcuts();

        shortcutAdapter.setShortcuts(
                shortcuts);

        shortcutEmptyMessage.setVisibility(
                shortcuts.isEmpty()
                        ? View.VISIBLE
                        : View.GONE);

        shortcutList.setVisibility(
                shortcuts.isEmpty()
                        ? View.GONE
                        : View.VISIBLE);
    }

    private void showShortcutEditor(
            ShortcutEntry shortcut) {

        ShortcutEditorDialog.show(
                this,
                categoryStore,
                shortcut,
                (name,
                 type,
                 target,
                 categoryIds,
                 favorite) -> {

                    if (shortcut == null) {
                        shortcutStore.add(
                                name,
                                type,
                                target,
                                categoryIds,
                                favorite);
                    } else {
                        shortcutStore.update(
                                shortcut.id,
                                name,
                                type,
                                target,
                                categoryIds,
                                favorite);
                    }

                    refreshShortcuts();
                });
    }

    private void showDeleteShortcutDialog(
            ShortcutEntry shortcut) {

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(
                                R.string.shortcut_delete_title)
                        .setMessage(
                                getString(
                                        R.string.shortcut_delete_message,
                                        shortcut.name))
                        .setPositiveButton(
                                R.string.action_delete_shortcut,
                                (currentDialog, which) -> {
                                    shortcutStore.delete(
                                            shortcut.id);

                                    refreshShortcuts();
                                })
                        .setNegativeButton(
                                R.string.action_cancel,
                                null)
                        .show();

        styleCategoryDialog(
                dialog,
                true);
    }

    private void refreshCategories() {
        List<CategoryEntry> categories =
                getCategoriesInOverviewOrder();

        categoryAdapter.setCategories(categories);

        categoryEmptyMessage.setVisibility(
                categories.isEmpty()
                        ? View.VISIBLE
                        : View.GONE);

        categoryList.setVisibility(
                categories.isEmpty()
                        ? View.GONE
                        : View.VISIBLE);
    }

    private void showAddCategoryDialog() {
        EditText input = createCategoryInput("");

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(R.string.category_add_title)
                        .setView(input)
                        .setPositiveButton(
                                R.string.action_save,
                                (currentDialog, which) -> {
                                    if (!categoryStore.addCategory(
                                            input.getText().toString())) {

                                        Toast.makeText(
                                                this,
                                                R.string.category_invalid_name,
                                                Toast.LENGTH_SHORT).show();
                                    }

                                    refreshCategories();
                                })
                        .setNegativeButton(
                                R.string.action_cancel,
                                null)
                        .show();

        styleCategoryDialog(dialog, false);
    }

    private void showRenameCategoryDialog(
            CategoryEntry category) {

        EditText input =
                createCategoryInput(category.name);

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(R.string.category_rename_title)
                        .setView(input)
                        .setPositiveButton(
                                R.string.action_save,
                                (currentDialog, which) -> {
                                    if (!categoryStore.renameCategory(
                                            category.id,
                                            input.getText().toString())) {

                                        Toast.makeText(
                                                this,
                                                R.string.category_invalid_name,
                                                Toast.LENGTH_SHORT).show();
                                    }

                                    refreshCategories();
                                })
                        .setNegativeButton(
                                R.string.action_cancel,
                                null)
                        .show();

        styleCategoryDialog(dialog, false);
    }

    private void showDeleteCategoryDialog(
            CategoryEntry category) {

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(R.string.category_delete_title)
                        .setMessage(
                                getString(
                                        R.string.category_delete_message,
                                        category.name))
                        .setPositiveButton(
                                R.string.action_delete_category,
                                (currentDialog, which) -> {
                                    categoryStore.deleteCategory(
                                            category.id);

                                    shortcutStore.removeCategory(
                                            category.id);

                                    sectionItemOrderStore.removeOrder(
                                            "category:" + category.id);

                                    refreshCategories();
                                })
                        .setNegativeButton(
                                R.string.action_cancel,
                                null)
                        .show();

        styleCategoryDialog(dialog, true);
    }

    private void styleCategoryDialog(
            AlertDialog dialog,
            boolean destructive) {

        int neutralColor =
                ContextCompat.getColor(
                        this,
                        R.color.ui_text_primary);

        int dangerColor =
                ContextCompat.getColor(
                        this,
                        R.color.ui_danger);

        dialog.getButton(
                        AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(neutralColor);

        dialog.getButton(
                        AlertDialog.BUTTON_POSITIVE)
                .setTextColor(
                        destructive
                                ? dangerColor
                                : neutralColor);
    }

    private EditText createCategoryInput(
            String value) {

        EditText input = new EditText(this);

        int textColor =
                ContextCompat.getColor(
                        this,
                        R.color.ui_text_primary);

        int hintColor =
                ContextCompat.getColor(
                        this,
                        R.color.ui_text_secondary);

        int borderColor =
                ContextCompat.getColor(
                        this,
                        R.color.ui_border);

        input.setHint(R.string.category_name_hint);
        input.setSingleLine(true);
        input.setText(value);
        input.setSelectAllOnFocus(true);

        input.setTextColor(textColor);
        input.setHintTextColor(hintColor);

        input.setBackgroundTintList(
                ColorStateList.valueOf(borderColor));

        if (Build.VERSION.SDK_INT
                >= Build.VERSION_CODES.Q) {
            input.setTextCursorDrawable(
                    R.drawable.search_cursor);
        }

        return input;
    }

    private void loadAppsAsync() {
        if (appsLoading) {
            return;
        }

        appsLoading = true;

        PackageManager packageManager =
                getPackageManager();

        String ownPackageName =
                getPackageName();

        appLoader.execute(() -> {
            List<AppEntry> loadedApps;

            try {
                loadedApps =
                        queryLauncherApps(
                                packageManager,
                                ownPackageName);
            } catch (RuntimeException exception) {
                loadedApps =
                        new ArrayList<>();
            }

            List<AppEntry> result =
                    loadedApps;

            runOnUiThread(() -> {
                if (isFinishing()
                        || isDestroyed()) {
                    return;
                }

                appsLoading = false;
                appsLoaded = true;

                apps.clear();
                apps.addAll(result);

                rebuildOverviewSections();
                overviewAdapter.setApps(apps);

                if (appSearchContainer.getVisibility()
                        == View.VISIBLE) {

                    renderApps(
                            appSearch.getText()
                                    .toString());
                }
            });
        });
    }

    private List<AppEntry> queryLauncherApps(
            PackageManager packageManager,
            String ownPackageName) {

        Intent launcherIntent =
                new Intent(Intent.ACTION_MAIN);

        launcherIntent.addCategory(
                Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> resolveInfos;

        if (Build.VERSION.SDK_INT
                >= Build.VERSION_CODES.TIRAMISU) {

            resolveInfos =
                    packageManager.queryIntentActivities(
                            launcherIntent,
                            PackageManager.ResolveInfoFlags.of(0));
        } else {
            resolveInfos =
                    packageManager.queryIntentActivities(
                            launcherIntent,
                            0);
        }

        List<AppEntry> loadedApps =
                new ArrayList<>();

        Set<String> seenPackages =
                new HashSet<>();

        for (ResolveInfo resolveInfo :
                resolveInfos) {

            String packageName =
                    resolveInfo.activityInfo.packageName;

            if (packageName.equals(
                    ownPackageName)
                    || !seenPackages.add(
                            packageName)) {

                continue;
            }

            CharSequence labelSequence =
                    resolveInfo.loadLabel(
                            packageManager);

            String label =
                    labelSequence != null
                            ? labelSequence.toString()
                            : packageName;

            loadedApps.add(
                    new AppEntry(
                            label,
                            packageName,
                            resolveInfo));
        }

        loadedApps.sort(
                (first, second) ->
                        first.label.compareToIgnoreCase(
                                second.label));

        return loadedApps;
    }

    private List<AppEntry> getFavoriteApps() {
        List<AppEntry> favoriteApps =
                new ArrayList<>();

        for (AppEntry app : apps) {
            if (favoritesStore.isFavorite(app.packageName)) {
                favoriteApps.add(app);
            }
        }

        return favoriteApps;
    }

    private void renderApps(String query) {
        String normalizedQuery =
                query.trim().toLowerCase(Locale.ROOT);

        List<AppEntry> filteredApps =
                new ArrayList<>();

        for (AppEntry app : apps) {
            String label =
                    app.label.toLowerCase(Locale.ROOT);

            String packageName =
                    app.packageName.toLowerCase(Locale.ROOT);

            if (!normalizedQuery.isEmpty()
                    && !label.contains(normalizedQuery)
                    && !packageName.contains(normalizedQuery)) {
                continue;
            }

            if (showOnlyUnassignedApps
                    && !categoryStore.getAssignedCategoryIds(
                            app.packageName).isEmpty()) {
                continue;
            }

            filteredApps.add(app);
        }

        appAdapter.submitList(
                filteredApps,
                appAdapter::refreshAppRows);

        if (filteredApps.isEmpty()) {
            appList.setVisibility(View.GONE);
            pageMessage.setVisibility(View.VISIBLE);

            if (appsLoading
                    && !appsLoaded) {

                pageMessage.setText(
                        R.string.apps_loading);

            } else if (showOnlyUnassignedApps
                    && normalizedQuery.isEmpty()) {

                pageMessage.setText(
                        R.string.apps_unassigned_empty);

            } else {
                pageMessage.setText(
                        normalizedQuery.isEmpty()
                                ? "Keine startbaren Apps gefunden."
                                : "Keine passenden Apps gefunden.");
            }
        } else {
            pageMessage.setVisibility(View.GONE);
            appList.setVisibility(View.VISIBLE);
        }
    }

    private void handleAppLongClick(AppEntry app) {
        List<CategoryEntry> categories =
                categoryStore.getCategories();

        if (categories.isEmpty()) {
            Toast.makeText(
                    this,
                    R.string.category_assign_none,
                    Toast.LENGTH_LONG).show();
            return;
        }

        CharSequence[] names =
                new CharSequence[categories.size()];

        boolean[] checked =
                new boolean[categories.size()];

        Set<String> assignedIds =
                categoryStore.getAssignedCategoryIds(
                        app.packageName);

        Set<String> selectedIds =
                new HashSet<>(assignedIds);

        for (int i = 0; i < categories.size(); i++) {
            CategoryEntry category =
                    categories.get(i);

            names[i] = category.name;
            checked[i] =
                    assignedIds.contains(category.id);
        }

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(R.string.category_assign_title)
                        .setMultiChoiceItems(
                                names,
                                checked,
                                (currentDialog, which, isChecked) -> {
                                    String categoryId =
                                            categories.get(which).id;

                                    if (isChecked) {
                                        selectedIds.add(categoryId);
                                    } else {
                                        selectedIds.remove(categoryId);
                                    }
                                })
                        .setPositiveButton(
                                R.string.action_save,
                                (currentDialog, which) -> {
                                    categoryStore.setAssignedCategoryIds(
                                            app.packageName,
                                            selectedIds);

                                    renderApps(
                                            appSearch.getText().toString());

                                    rebuildOverviewSections();
                                    overviewAdapter.setApps(apps);
                                })
                        .setNegativeButton(
                                R.string.action_cancel,
                                null)
                        .show();

        styleCategoryDialog(dialog, false);
    }

    private void launchShortcut(
            ShortcutEntry shortcut) {

        try {
            if (ShortcutEntry.TYPE_WEBSITE.equals(
                    shortcut.type)) {

                launchWebShortcut(
                        shortcut.target);

                return;
            }

            if (ShortcutEntry.TYPE_WEB_APP.equals(
                    shortcut.type)) {

                launchWebAppShortcut(
                        shortcut.target);

                return;
            }

            if (ShortcutEntry.TYPE_APP_SETTINGS.equals(
                    shortcut.type)) {

                launchAppSettings(
                        shortcut.target);

                return;
            }

            String target =
                    shortcut.target.trim();

            if (target.startsWith(
                    "package:")) {

                launchAppSettings(
                        target);

                return;
            }

            Intent intent;

            if (target.startsWith(
                    "intent:")) {

                intent =
                        Intent.parseUri(
                                target,
                                Intent.URI_INTENT_SCHEME);

            } else {
                intent =
                        new Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(target));
            }

            startActivity(intent);

        } catch (Exception exception) {
            Toast.makeText(
                    this,
                    R.string.shortcut_launch_failed,
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void launchWebAppShortcut(
            String target) {

        if (!NetworkAccess.hasUsableNetwork(
                this)) {

            showWebAppNetworkDialog();
            return;
        }

        Intent intent =
                new Intent(
                        this,
                        WebAppActivity.class);

        intent.putExtra(
                WebAppActivity.EXTRA_URL,
                target);

        startActivity(
                intent);
    }

    private void showWebAppNetworkDialog() {

        NetworkAccess.showNetworkHelp(
                this);
    }

    private void launchWebShortcut(
            String target) {

        Uri uri =
                Uri.parse(target);

        Intent intent =
                new Intent(
                        Intent.ACTION_VIEW,
                        uri);

        intent.addCategory(
                Intent.CATEGORY_BROWSABLE);

        Intent browserSelector =
                Intent.makeMainSelectorActivity(
                        Intent.ACTION_MAIN,
                        Intent.CATEGORY_APP_BROWSER);

        ResolveInfo defaultBrowser =
                getPackageManager()
                        .resolveActivity(
                                browserSelector,
                                PackageManager.MATCH_DEFAULT_ONLY);

        if (defaultBrowser != null
                && defaultBrowser.activityInfo != null) {

            String browserPackage =
                    defaultBrowser
                            .activityInfo
                            .packageName;

            if (browserPackage != null
                    && !browserPackage.isEmpty()
                    && !"android".equals(
                            browserPackage)) {

                intent.setPackage(
                        browserPackage);
            }
        }

        try {
            startActivity(intent);

        } catch (ActivityNotFoundException exception) {
            intent.setPackage(null);
            startActivity(intent);
        }
    }

    private void launchAppSettings(
            String target) {

        String packageTarget =
                target.startsWith(
                        "package:")
                        ? target
                        : "package:" + target;

        Intent intent =
                new Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse(packageTarget));

        startActivity(intent);
    }

    private void launchApp(AppEntry app) {
        Intent launchIntent =
                new Intent(Intent.ACTION_MAIN);

        launchIntent.addCategory(
                Intent.CATEGORY_LAUNCHER);

        launchIntent.setClassName(
                app.packageName,
                app.resolveInfo.activityInfo.name);

        try {
            startActivity(launchIntent);
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(
                    this,
                    "App konnte nicht gestartet werden.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void setTopNavigation(
            boolean overview) {

        showingOverview = overview;

        if (overview) {
            drawerLayout.setDrawerLockMode(
                    DrawerLayout.LOCK_MODE_UNLOCKED,
                    Gravity.END);

            topNavigationButton.setImageResource(
                    R.drawable.ic_menu);

            topNavigationButton.setContentDescription(
                    getString(
                            R.string.action_open_menu));

            topNavigationButton.setOnClickListener(v ->
                    drawerLayout.openDrawer(
                            Gravity.END));

            return;
        }

        drawerLayout.closeDrawer(
                Gravity.END);

        drawerLayout.setDrawerLockMode(
                DrawerLayout.LOCK_MODE_LOCKED_CLOSED,
                Gravity.END);

        topNavigationButton.setImageResource(
                R.drawable.ic_arrow_back);

        topNavigationButton.setContentDescription(
                getString(
                        R.string.action_back_to_overview));

        topNavigationButton.setOnClickListener(v ->
                showOverview());
    }

    @Override
    public void onBackPressed() {

        if (drawerLayout.isDrawerOpen(
                Gravity.END)) {

            drawerLayout.closeDrawer(
                    Gravity.END);

            return;
        }

        if (!showingOverview) {
            showOverview();
            return;
        }

        super.onBackPressed();
    }

    private void showHelp(
            boolean jumpToShortcuts) {

        setTopNavigation(false);

        shortcutHelpButton.setVisibility(
                View.GONE);

        hideOverviewSortMode();

        categoryManagement.setVisibility(
                View.GONE);

        shortcutManagement.setVisibility(
                View.GONE);

        backupManagement.setVisibility(
                View.GONE);

        overviewList.setVisibility(
                View.GONE);

        appList.setVisibility(
                View.GONE);

        pageMessage.setVisibility(
                View.GONE);

        appSearchContainer.setVisibility(
                View.GONE);

        appSearchClear.setVisibility(
                View.GONE);

        appSearch.clearFocus();

        pageTitle.setText(
                R.string.nav_help);

        helpManagement.setVisibility(
                View.VISIBLE);

        drawerLayout.closeDrawer(
                Gravity.END);

        helpManagement.post(() -> {
            if (jumpToShortcuts) {
                helpManagement.smoothScrollTo(
                        0,
                        helpShortcutSection.getTop());
            } else {
                helpManagement.scrollTo(0, 0);
            }
        });
    }

    private void showBackupManagement() {
        setTopNavigation(false);

        shortcutHelpButton.setVisibility(
                View.GONE);

        hideOverviewSortMode();

        categoryManagement.setVisibility(
                View.GONE);

        shortcutManagement.setVisibility(
                View.GONE);

        helpManagement.setVisibility(
                View.GONE);

        overviewList.setVisibility(
                View.GONE);

        appList.setVisibility(
                View.GONE);

        pageMessage.setVisibility(
                View.GONE);

        appSearchContainer.setVisibility(
                View.GONE);

        appSearchClear.setVisibility(
                View.GONE);

        appSearch.clearFocus();

        pageTitle.setText(
                R.string.nav_backup);

        backupManagement.setVisibility(
                View.VISIBLE);

        drawerLayout.closeDrawer(
                Gravity.END);
    }

    private void createBackup() {
        Intent intent =
                new Intent(
                        Intent.ACTION_CREATE_DOCUMENT);

        intent.addCategory(
                Intent.CATEGORY_OPENABLE);

        intent.setType(
                "application/json");

        intent.putExtra(
                Intent.EXTRA_TITLE,
                "AppStow-backup.json");

        try {
            startActivityForResult(
                    intent,
                    REQUEST_CREATE_BACKUP);

        } catch (ActivityNotFoundException exception) {
            Toast.makeText(
                    this,
                    R.string.backup_failed,
                    Toast.LENGTH_LONG)
                    .show();
        }
    }

    private void selectBackupForRestore() {
        Intent intent =
                new Intent(
                        Intent.ACTION_OPEN_DOCUMENT);

        intent.addCategory(
                Intent.CATEGORY_OPENABLE);

        intent.setType(
                "application/json");

        try {
            startActivityForResult(
                    intent,
                    REQUEST_RESTORE_BACKUP);

        } catch (ActivityNotFoundException exception) {
            Toast.makeText(
                    this,
                    R.string.backup_restore_failed,
                    Toast.LENGTH_LONG)
                    .show();
        }
    }

    private void confirmBackupRestore(
            JSONObject backup) {

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(
                                R.string.backup_restore_title)
                        .setMessage(
                                R.string.backup_restore_message)
                        .setPositiveButton(
                                R.string.backup_restore_confirm,
                                (currentDialog, which) -> {
                                    try {
                                        BackupManager.restoreBackup(
                                                this,
                                                backup);

                                        Toast.makeText(
                                                this,
                                                R.string.backup_restored,
                                                Toast.LENGTH_LONG)
                                                .show();

                                        recreate();

                                    } catch (Exception exception) {
                                        Toast.makeText(
                                                this,
                                                R.string.backup_restore_failed,
                                                Toast.LENGTH_LONG)
                                                .show();
                                    }
                                })
                        .setNegativeButton(
                                R.string.action_cancel,
                                null)
                        .show();

        styleCategoryDialog(
                dialog,
                true);
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data);

        if (resultCode != RESULT_OK
                || data == null
                || data.getData() == null) {
            return;
        }

        Uri uri =
                data.getData();

        if (requestCode
                == REQUEST_CREATE_BACKUP) {

            try {
                BackupManager.writeBackup(
                        this,
                        uri);

                Toast.makeText(
                        this,
                        R.string.backup_created,
                        Toast.LENGTH_LONG)
                        .show();

            } catch (Exception exception) {
                Toast.makeText(
                        this,
                        R.string.backup_failed,
                        Toast.LENGTH_LONG)
                        .show();
            }

            return;
        }

        if (requestCode
                == REQUEST_RESTORE_BACKUP) {

            try {
                JSONObject backup =
                        BackupManager.readBackup(
                                this,
                                uri);

                confirmBackupRestore(
                        backup);

            } catch (Exception exception) {
                Toast.makeText(
                        this,
                        R.string.backup_restore_failed,
                        Toast.LENGTH_LONG)
                        .show();
            }
        }
    }

    private void bindMenu(
            int viewId,
            String title,
            String content) {

        findViewById(viewId).setOnClickListener(v -> {
            pageTitle.setText(title);
            showMessage(content);
            drawerLayout.closeDrawer(Gravity.END);
        });
    }

    private void showMessage(String message) {
        setTopNavigation(false);

        shortcutHelpButton.setVisibility(View.GONE);
        hideOverviewSortMode();
        categoryManagement.setVisibility(View.GONE);
        shortcutManagement.setVisibility(View.GONE);
        backupManagement.setVisibility(View.GONE);
        helpManagement.setVisibility(View.GONE);
        overviewList.setVisibility(View.GONE);
        appSearchContainer.setVisibility(View.GONE);
        appSearchClear.setVisibility(View.GONE);
        appSearch.clearFocus();

        appList.setVisibility(View.GONE);

        pageMessage.setText(message);
        pageMessage.setVisibility(View.VISIBLE);
    }
}
