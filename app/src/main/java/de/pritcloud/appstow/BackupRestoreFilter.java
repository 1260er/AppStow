package de.pritcloud.appstow;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

final class BackupRestoreFilter {

    private static final String APP_PREFIX =
            "app:";

    private BackupRestoreFilter() {
    }

    static Set<String> queryInstalledLauncherPackages(
            Context context)
            throws IOException {

        try {
            Intent launcherIntent =
                    new Intent(
                            Intent.ACTION_MAIN);

            launcherIntent.addCategory(
                    Intent.CATEGORY_LAUNCHER);

            PackageManager packageManager =
                    context.getPackageManager();

            List<ResolveInfo> resolveInfos;

            if (Build.VERSION.SDK_INT
                    >= Build.VERSION_CODES.TIRAMISU) {

                resolveInfos =
                        packageManager.queryIntentActivities(
                                launcherIntent,
                                PackageManager.ResolveInfoFlags.of(
                                        0));

            } else {

                resolveInfos =
                        packageManager.queryIntentActivities(
                                launcherIntent,
                                0);
            }

            Set<String> installed =
                    new HashSet<>();

            for (ResolveInfo resolveInfo :
                    resolveInfos) {

                if (resolveInfo == null
                        || resolveInfo.activityInfo == null
                        || resolveInfo.activityInfo.packageName == null
                        || resolveInfo.activityInfo.packageName.isBlank()) {

                    continue;
                }

                installed.add(
                        resolveInfo.activityInfo.packageName);
            }

            return installed;

        } catch (RuntimeException exception) {

            throw new IOException(
                    "Installierte Apps konnten für die Wiederherstellung nicht geprüft werden.",
                    exception);
        }
    }

    static JSONObject filterAssignments(
            JSONObject assignments,
            Set<String> installedPackages)
            throws JSONException {

        requireInstalledPackages(
                installedPackages);

        JSONObject filtered =
                new JSONObject();

        Iterator<String> keys =
                assignments.keys();

        while (keys.hasNext()) {
            String packageName =
                    keys.next();

            if (!installedPackages.contains(
                    packageName)) {

                continue;
            }

            filtered.put(
                    packageName,
                    assignments.getJSONArray(
                            packageName));
        }

        return filtered;
    }

    static Set<String> filterFavorites(
            JSONArray favoritePackages,
            Set<String> installedPackages)
            throws JSONException {

        requireInstalledPackages(
                installedPackages);

        Set<String> filtered =
                new HashSet<>();

        for (int i = 0;
             i < favoritePackages.length();
             i++) {

            String packageName =
                    favoritePackages.getString(
                            i);

            if (installedPackages.contains(
                    packageName)) {

                filtered.add(
                        packageName);
            }
        }

        return filtered;
    }

    static JSONObject filterSectionItemOrder(
            JSONObject sectionItemOrder,
            Set<String> installedPackages)
            throws JSONException {

        requireInstalledPackages(
                installedPackages);

        JSONObject filtered =
                new JSONObject();

        Iterator<String> keys =
                sectionItemOrder.keys();

        while (keys.hasNext()) {
            String sectionId =
                    keys.next();

            JSONArray source =
                    sectionItemOrder.getJSONArray(
                            sectionId);

            JSONArray target =
                    new JSONArray();

            for (int i = 0;
                 i < source.length();
                 i++) {

                String itemId =
                        source.getString(
                                i);

                if (itemId.startsWith(
                        APP_PREFIX)
                        && !installedPackages.contains(
                                itemId.substring(
                                        APP_PREFIX.length()))) {

                    continue;
                }

                target.put(
                        itemId);
            }

            filtered.put(
                    sectionId,
                    target);
        }

        return filtered;
    }

    private static void requireInstalledPackages(
            Set<String> installedPackages) {

        if (installedPackages == null) {
            throw new IllegalArgumentException(
                    "Installed package set is required.");
        }
    }
}
