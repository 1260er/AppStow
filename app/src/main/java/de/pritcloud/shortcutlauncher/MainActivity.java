package de.pritcloud.shortcutlauncher;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.TextView;

import androidx.drawerlayout.widget.DrawerLayout;

public class MainActivity extends Activity {

    private DrawerLayout drawerLayout;
    private TextView pageTitle;
    private TextView pageContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawerLayout = findViewById(R.id.drawerLayout);
        pageTitle = findViewById(R.id.pageTitle);
        pageContent = findViewById(R.id.pageContent);

        findViewById(R.id.buttonOpenMenu).setOnClickListener(v ->
                drawerLayout.openDrawer(Gravity.END));

        bindMenu(R.id.navApps, "Apps", "Installierte Apps werden hier angezeigt.");
        bindMenu(R.id.navCategories, "Kategorien", "Kategorien werden hier verwaltet.");
        bindMenu(R.id.navFavorites, "Favoriten", "Favorisierte Apps und Shortcuts.");
        bindMenu(R.id.navShortcuts, "Shortcuts", "Eigene Shortcuts werden hier verwaltet.");
        bindMenu(R.id.navSettings, "Einstellungen", "Einstellungen des ShortcutLaunchers.");
        bindMenu(R.id.navHelp, "Hilfe", "Hilfe und Bedienung.");
        bindMenu(R.id.navAbout, "Über", "ShortcutLauncher 0.1.0");
    }

    private void bindMenu(int viewId, String title, String content) {
        findViewById(viewId).setOnClickListener(v -> {
            pageTitle.setText(title);
            pageContent.setText(content);
            drawerLayout.closeDrawer(Gravity.END);
        });
    }
}
