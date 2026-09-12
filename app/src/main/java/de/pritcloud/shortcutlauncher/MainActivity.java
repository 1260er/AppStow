package de.pritcloud.shortcutlauncher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.DisplayCutout;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity {

    private DrawerLayout drawerLayout;
    private TextView pageTitle;
    private TextView pageMessage;
    private RecyclerView appList;
    private RecyclerView overviewList;
    private RecyclerView categoryList;
    private View categoryManagement;
    private TextView categoryEmptyMessage;
    private EditText appSearch;
    private View appSearchContainer;
    private ImageButton appSearchClear;

    private AppAdapter appAdapter;
    private OverviewAdapter overviewAdapter;
    private FavoritesStore favoritesStore;
    private CategoryStore categoryStore;
    private CategoryAdapter categoryAdapter;

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
        appSearch = findViewById(R.id.appSearch);
        appSearchContainer = findViewById(R.id.appSearchContainer);
        appSearchClear = findViewById(R.id.appSearchClear);

        favoritesStore = new FavoritesStore(this);
        categoryStore = new CategoryStore(this);

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
                        this::launchApp,
                        this::handleAppLongClick);

        overviewList.setLayoutManager(
                new LinearLayoutManager(this));
        overviewList.setAdapter(overviewAdapter);

        findViewById(R.id.buttonOpenMenu).setOnClickListener(v ->
                drawerLayout.openDrawer(Gravity.END));

        findViewById(R.id.navOverview).setOnClickListener(v ->
                showOverview());

        findViewById(R.id.navApps).setOnClickListener(v ->
                showApps());

        findViewById(R.id.navCategories).setOnClickListener(v ->
                showCategoryManagement());

        bindMenu(
                R.id.navSettings,
                "Einstellungen",
                "Einstellungen des ShortcutLaunchers.");

        bindMenu(
                R.id.navHelp,
                "Hilfe",
                "Hilfe und Bedienung.");

        bindMenu(
                R.id.navAbout,
                "Über",
                "ShortcutLauncher 0.1.0");

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
    }

    private void showOverview() {
        categoryManagement.setVisibility(View.GONE);
        loadApps();

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
    }

    private void showApps() {
        categoryManagement.setVisibility(View.GONE);
        overviewList.setVisibility(View.GONE);
        loadApps();

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

    private void refreshCategories() {
        List<CategoryEntry> categories =
                categoryStore.getCategories();

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

    private void loadApps() {
        PackageManager packageManager = getPackageManager();

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

        apps.clear();

        Set<String> seenPackages = new HashSet<>();

        for (ResolveInfo resolveInfo : resolveInfos) {
            String packageName =
                    resolveInfo.activityInfo.packageName;

            if (packageName.equals(getPackageName())
                    || !seenPackages.add(packageName)) {
                continue;
            }

            CharSequence labelSequence =
                    resolveInfo.loadLabel(packageManager);

            String label =
                    labelSequence != null
                            ? labelSequence.toString()
                            : packageName;

            apps.add(
                    new AppEntry(
                            label,
                            packageName,
                            resolveInfo));
        }

        apps.sort((first, second) ->
                first.label.compareToIgnoreCase(
                        second.label));
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

            filteredApps.add(app);
        }

        appAdapter.submitList(filteredApps);

        if (filteredApps.isEmpty()) {
            appList.setVisibility(View.GONE);
            pageMessage.setVisibility(View.VISIBLE);

            pageMessage.setText(
                    normalizedQuery.isEmpty()
                            ? "Keine startbaren Apps gefunden."
                            : "Keine passenden Apps gefunden.");
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

                                    appAdapter.refreshAppRows();
                                    overviewAdapter.refreshAppRows();
                                })
                        .setNegativeButton(
                                R.string.action_cancel,
                                null)
                        .show();

        styleCategoryDialog(dialog, false);
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
        categoryManagement.setVisibility(View.GONE);
        overviewList.setVisibility(View.GONE);
        appSearchContainer.setVisibility(View.GONE);
        appSearchClear.setVisibility(View.GONE);
        appSearch.clearFocus();

        appList.setVisibility(View.GONE);

        pageMessage.setText(message);
        pageMessage.setVisibility(View.VISIBLE);
    }
}
