package de.pritcloud.shortcutlauncher;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
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

import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity {

    private DrawerLayout drawerLayout;
    private TextView pageTitle;
    private TextView pageMessage;
    private RecyclerView appList;
    private EditText appSearch;
    private View appSearchContainer;
    private ImageButton appSearchClear;

    private AppAdapter appAdapter;

    private final List<AppEntry> apps = new ArrayList<>();

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
        appSearch = findViewById(R.id.appSearch);
        appSearchContainer = findViewById(R.id.appSearchContainer);
        appSearchClear = findViewById(R.id.appSearchClear);

        appAdapter = new AppAdapter(
                getPackageManager(),
                this::launchApp);

        appList.setLayoutManager(new LinearLayoutManager(this));
        appList.setAdapter(appAdapter);
        appList.setHasFixedSize(true);

        findViewById(R.id.buttonOpenMenu).setOnClickListener(v ->
                drawerLayout.openDrawer(Gravity.END));

        findViewById(R.id.navApps).setOnClickListener(v ->
                showApps());

        bindMenu(
                R.id.navCategories,
                "Kategorien",
                "Kategorien werden hier verwaltet.");

        bindMenu(
                R.id.navFavorites,
                "Favoriten",
                "Favorisierte Apps und Shortcuts.");

        bindMenu(
                R.id.navShortcuts,
                "Shortcuts",
                "Eigene Shortcuts werden hier verwaltet.");

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

        showMessage(getString(R.string.home_empty));
    }

    private void showApps() {
        loadApps();

        pageTitle.setText(R.string.nav_apps);

        appSearchContainer.setVisibility(View.VISIBLE);
        appSearchClear.setVisibility(
                appSearch.length() > 0
                        ? View.VISIBLE
                        : View.GONE);

        renderApps(appSearch.getText().toString());

        drawerLayout.closeDrawer(Gravity.END);
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
        appSearchContainer.setVisibility(View.GONE);
        appSearchClear.setVisibility(View.GONE);
        appSearch.clearFocus();

        appList.setVisibility(View.GONE);

        pageMessage.setText(message);
        pageMessage.setVisibility(View.VISIBLE);
    }
}
