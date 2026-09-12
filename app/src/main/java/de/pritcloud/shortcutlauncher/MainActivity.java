package de.pritcloud.shortcutlauncher;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.DisplayCutout;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.drawerlayout.widget.DrawerLayout;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity {

    private DrawerLayout drawerLayout;
    private TextView pageTitle;
    private LinearLayout pageContent;

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
        pageContent = findViewById(R.id.pageContent);

        findViewById(R.id.buttonOpenMenu).setOnClickListener(v ->
                drawerLayout.openDrawer(Gravity.END));

        findViewById(R.id.navApps).setOnClickListener(v -> showApps());

        bindMenu(R.id.navCategories, "Kategorien",
                "Kategorien werden hier verwaltet.");
        bindMenu(R.id.navFavorites, "Favoriten",
                "Favorisierte Apps und Shortcuts.");
        bindMenu(R.id.navShortcuts, "Shortcuts",
                "Eigene Shortcuts werden hier verwaltet.");
        bindMenu(R.id.navSettings, "Einstellungen",
                "Einstellungen des ShortcutLaunchers.");
        bindMenu(R.id.navHelp, "Hilfe",
                "Hilfe und Bedienung.");
        bindMenu(R.id.navAbout, "Über",
                "ShortcutLauncher 0.1.0");

        showMessage(getString(R.string.home_empty));
    }

    private void showApps() {
        PackageManager packageManager = getPackageManager();

        Intent launcherIntent = new Intent(Intent.ACTION_MAIN);
        launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> resolveInfos;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            resolveInfos = packageManager.queryIntentActivities(
                    launcherIntent,
                    PackageManager.ResolveInfoFlags.of(0));
        } else {
            resolveInfos = packageManager.queryIntentActivities(
                    launcherIntent, 0);
        }

        resolveInfos.sort((first, second) -> {
            String firstLabel = first.loadLabel(packageManager).toString();
            String secondLabel = second.loadLabel(packageManager).toString();
            return firstLabel.compareToIgnoreCase(secondLabel);
        });

        pageTitle.setText(R.string.nav_apps);
        pageContent.removeAllViews();

        Set<String> seenPackages = new HashSet<>();

        for (ResolveInfo resolveInfo : resolveInfos) {
            String packageName = resolveInfo.activityInfo.packageName;

            if (packageName.equals(getPackageName())
                    || !seenPackages.add(packageName)) {
                continue;
            }

            String label = resolveInfo.loadLabel(packageManager).toString();

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(8), 0, dp(8));
            row.setClickable(true);
            row.setFocusable(true);

            ImageView icon = new ImageView(this);
            icon.setImageDrawable(resolveInfo.loadIcon(packageManager));

            LinearLayout.LayoutParams iconParams =
                    new LinearLayout.LayoutParams(dp(48), dp(48));
            row.addView(icon, iconParams);

            LinearLayout textContainer = new LinearLayout(this);
            textContainer.setOrientation(LinearLayout.VERTICAL);

            LinearLayout.LayoutParams textParams =
                    new LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1f);
            textParams.setMarginStart(dp(16));

            TextView nameView = new TextView(this);
            nameView.setText(label);
            nameView.setTextAppearance(R.style.UiBodyText);
            nameView.setTypeface(nameView.getTypeface(), android.graphics.Typeface.BOLD);

            TextView packageView = new TextView(this);
            packageView.setText(packageName);
            packageView.setTextAppearance(R.style.UiBodyText);
            packageView.setTextColor(getColor(R.color.ui_text_secondary));
            packageView.setTextSize(13);

            textContainer.addView(nameView);
            textContainer.addView(packageView);

            row.addView(textContainer, textParams);

            row.setOnClickListener(v -> {
                Intent launchIntent = new Intent(Intent.ACTION_MAIN);
                launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                launchIntent.setClassName(
                        packageName,
                        resolveInfo.activityInfo.name);

                try {
                    startActivity(launchIntent);
                } catch (ActivityNotFoundException exception) {
                    Toast.makeText(
                            this,
                            "App konnte nicht gestartet werden.",
                            Toast.LENGTH_SHORT).show();
                }
            });

            pageContent.addView(row);
        }

        if (pageContent.getChildCount() == 0) {
            showMessage("Keine startbaren Apps gefunden.");
        }

        drawerLayout.closeDrawer(Gravity.END);
    }

    private void bindMenu(int viewId, String title, String content) {
        findViewById(viewId).setOnClickListener(v -> {
            pageTitle.setText(title);
            showMessage(content);
            drawerLayout.closeDrawer(Gravity.END);
        });
    }

    private void showMessage(String message) {
        pageContent.removeAllViews();

        TextView textView = new TextView(this);
        textView.setText(message);
        textView.setTextAppearance(R.style.UiBodyText);

        pageContent.addView(textView);
    }

    private int dp(int value) {
        return Math.round(
                value * getResources().getDisplayMetrics().density);
    }
}
