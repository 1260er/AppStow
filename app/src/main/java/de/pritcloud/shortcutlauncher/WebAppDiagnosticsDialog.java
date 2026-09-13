package de.pritcloud.shortcutlauncher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class WebAppDiagnosticsDialog {

    private static final String GENERIC_URL =
            "https://example.com/";

    private WebAppDiagnosticsDialog() {
    }

    static void show(Activity activity) {

        View view =
                LayoutInflater.from(activity)
                        .inflate(
                                R.layout.dialog_webapp_diagnostics,
                                null,
                                false);

        EditText url =
                view.findViewById(
                        R.id.webappDiagnosticUrl);

        TextView analyze =
                view.findViewById(
                        R.id.webappDiagnosticAnalyze);

        TextView openAndroid =
                view.findViewById(
                        R.id.webappDiagnosticOpenAndroid);

        TextView openBrowser =
                view.findViewById(
                        R.id.webappDiagnosticOpenBrowser);

        TextView openSpecific =
                view.findViewById(
                        R.id.webappDiagnosticOpenSpecific);

        TextView result =
                view.findViewById(
                        R.id.webappDiagnosticResult);

        url.setText(
                "https://www.booking.com/");

        url.setSelection(
                url.length());

        analyze.setOnClickListener(v ->
                analyze(
                        activity,
                        url,
                        result));

        openAndroid.setOnClickListener(v -> {
            Uri uri =
                    getUri(
                            activity,
                            url);

            if (uri == null) {
                return;
            }

            try {
                activity.startActivity(
                        createViewIntent(
                                uri));

            } catch (Exception exception) {
                showLaunchError(
                        activity);
            }
        });

        openBrowser.setOnClickListener(v -> {
            Uri uri =
                    getUri(
                            activity,
                            url);

            if (uri == null) {
                return;
            }

            openInDefaultBrowser(
                    activity,
                    uri);
        });

        openSpecific.setOnClickListener(v -> {
            Uri uri =
                    getUri(
                            activity,
                            url);

            if (uri == null) {
                return;
            }

            showSpecificHandlerPicker(
                    activity,
                    uri);
        });

        AlertDialog dialog =
                new AlertDialog.Builder(activity)
                        .setTitle(
                                R.string.webapp_diagnostics)
                        .setView(view)
                        .setPositiveButton(
                                R.string.action_close,
                                null)
                        .create();

        dialog.setOnShowListener(ignored -> {
            int neutral =
                    ContextCompat.getColor(
                            activity,
                            R.color.ui_text_primary);

            dialog.getButton(
                            AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(
                            neutral);

            analyze(
                    activity,
                    url,
                    result);
        });

        dialog.show();
    }

    private static void analyze(
            Activity activity,
            EditText urlInput,
            TextView result) {

        Uri uri =
                getUri(
                        activity,
                        urlInput);

        if (uri == null) {
            return;
        }

        PackageManager packageManager =
                activity.getPackageManager();

        Intent genericIntent =
                createViewIntent(
                        Uri.parse(
                                GENERIC_URL));

        Intent targetIntent =
                createViewIntent(
                        uri);

        ResolveInfo genericDefault =
                resolve(
                        packageManager,
                        genericIntent);

        ResolveInfo targetDefault =
                resolve(
                        packageManager,
                        targetIntent);

        List<ResolveInfo> targetHandlers =
                query(
                        packageManager,
                        targetIntent);

        List<ResolveInfo> specificHandlers =
                getSpecificHandlers(
                        packageManager,
                        uri);

        StringBuilder report =
                new StringBuilder();

        report.append("Android ")
                .append(Build.VERSION.RELEASE)
                .append(" (API ")
                .append(Build.VERSION.SDK_INT)
                .append(")\n\n");

        report.append("URL:\n")
                .append(uri)
                .append("\n\n");

        report.append("Standard-HTTPS-Handler:\n")
                .append(
                        describe(
                                packageManager,
                                genericDefault))
                .append("\n\n");

        report.append("Standard-Handler für diese URL:\n")
                .append(
                        describe(
                                packageManager,
                                targetDefault))
                .append("\n\n");

        report.append("Spezifische Handler:\n");

        if (specificHandlers.isEmpty()) {
            report.append("Keine gefunden");
        } else {
            for (ResolveInfo info :
                    specificHandlers) {

                report.append(
                        describe(
                                packageManager,
                                info))
                        .append("\n");
            }
        }

        report.append(
                "\nAlle Handler für diese URL:\n");

        if (targetHandlers.isEmpty()) {
            report.append("Keine gefunden");
        } else {
            for (ResolveInfo info :
                    targetHandlers) {

                report.append(
                        describe(
                                packageManager,
                                info))
                        .append("\n");
            }
        }

        result.setText(
                report.toString().trim());
    }

    private static List<ResolveInfo> getSpecificHandlers(
            PackageManager packageManager,
            Uri targetUri) {

        List<ResolveInfo> genericHandlers =
                query(
                        packageManager,
                        createViewIntent(
                                Uri.parse(
                                        GENERIC_URL)));

        List<ResolveInfo> targetHandlers =
                query(
                        packageManager,
                        createViewIntent(
                                targetUri));

        Set<String> genericKeys =
                new HashSet<>();

        for (ResolveInfo info :
                genericHandlers) {

            genericKeys.add(
                    getKey(info));
        }

        List<ResolveInfo> result =
                new ArrayList<>();

        for (ResolveInfo info :
                targetHandlers) {

            if (!genericKeys.contains(
                    getKey(info))) {

                result.add(info);
            }
        }

        return result;
    }

    private static void showSpecificHandlerPicker(
            Activity activity,
            Uri uri) {

        PackageManager packageManager =
                activity.getPackageManager();

        List<ResolveInfo> handlers =
                getSpecificHandlers(
                        packageManager,
                        uri);

        if (handlers.isEmpty()) {
            Toast.makeText(
                    activity,
                    R.string.webapp_diagnostics_no_specific,
                    Toast.LENGTH_LONG)
                    .show();

            return;
        }

        if (handlers.size() == 1) {
            startSpecificHandler(
                    activity,
                    uri,
                    handlers.get(0));

            return;
        }

        CharSequence[] labels =
                new CharSequence[
                        handlers.size()];

        for (int i = 0;
             i < handlers.size();
             i++) {

            labels[i] =
                    describe(
                            packageManager,
                            handlers.get(i));
        }

        new AlertDialog.Builder(activity)
                .setTitle(
                        R.string.webapp_diagnostics_choose_specific)
                .setItems(
                        labels,
                        (dialog, which) ->
                                startSpecificHandler(
                                        activity,
                                        uri,
                                        handlers.get(which)))
                .setNegativeButton(
                        R.string.action_cancel,
                        null)
                .show();
    }

    private static void startSpecificHandler(
            Activity activity,
            Uri uri,
            ResolveInfo info) {

        if (info == null
                || info.activityInfo == null) {

            showLaunchError(
                    activity);

            return;
        }

        Intent intent =
                createViewIntent(
                        uri);

        intent.setComponent(
                new ComponentName(
                        info.activityInfo.packageName,
                        info.activityInfo.name));

        try {
            activity.startActivity(
                    intent);

        } catch (Exception exception) {
            showLaunchError(
                    activity);
        }
    }

    private static void openInDefaultBrowser(
            Activity activity,
            Uri uri) {

        PackageManager packageManager =
                activity.getPackageManager();

        ResolveInfo defaultHandler =
                resolve(
                        packageManager,
                        createViewIntent(
                                Uri.parse(
                                        GENERIC_URL)));

        Intent intent =
                createViewIntent(
                        uri);

        if (defaultHandler != null
                && defaultHandler.activityInfo != null) {

            String packageName =
                    defaultHandler
                            .activityInfo
                            .packageName;

            if (packageName != null
                    && !packageName.isEmpty()
                    && !"android".equals(
                            packageName)) {

                intent.setPackage(
                        packageName);
            }
        }

        try {
            activity.startActivity(
                    intent);

        } catch (Exception exception) {
            showLaunchError(
                    activity);
        }
    }

    private static Uri getUri(
            Activity activity,
            EditText input) {

        String value =
                input.getText()
                        .toString()
                        .trim();

        Uri uri =
                Uri.parse(
                        value);

        String scheme =
                uri.getScheme();

        if (scheme == null
                || uri.getHost() == null
                || (!"http".equalsIgnoreCase(
                        scheme)
                && !"https".equalsIgnoreCase(
                        scheme))) {

            Toast.makeText(
                    activity,
                    R.string.webapp_diagnostics_invalid_url,
                    Toast.LENGTH_SHORT)
                    .show();

            return null;
        }

        return uri;
    }

    private static Intent createViewIntent(
            Uri uri) {

        Intent intent =
                new Intent(
                        Intent.ACTION_VIEW,
                        uri);

        intent.addCategory(
                Intent.CATEGORY_BROWSABLE);

        return intent;
    }

    private static ResolveInfo resolve(
            PackageManager packageManager,
            Intent intent) {

        if (Build.VERSION.SDK_INT
                >= Build.VERSION_CODES.TIRAMISU) {

            return packageManager.resolveActivity(
                    intent,
                    PackageManager.ResolveInfoFlags.of(
                            PackageManager.MATCH_DEFAULT_ONLY));
        }

        return packageManager.resolveActivity(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY);
    }

    private static List<ResolveInfo> query(
            PackageManager packageManager,
            Intent intent) {

        if (Build.VERSION.SDK_INT
                >= Build.VERSION_CODES.TIRAMISU) {

            return packageManager.queryIntentActivities(
                    intent,
                    PackageManager.ResolveInfoFlags.of(0));
        }

        return packageManager.queryIntentActivities(
                intent,
                0);
    }

    private static String describe(
            PackageManager packageManager,
            ResolveInfo info) {

        if (info == null
                || info.activityInfo == null) {

            return "Keiner";
        }

        CharSequence label =
                info.loadLabel(
                        packageManager);

        String displayLabel =
                label == null
                        ? "Unbekannt"
                        : label.toString();

        return displayLabel
                + "\n"
                + info.activityInfo.packageName
                + "\n"
                + info.activityInfo.name;
    }

    private static String getKey(
            ResolveInfo info) {

        if (info == null
                || info.activityInfo == null) {

            return "";
        }

        return info.activityInfo.packageName
                + "/"
                + info.activityInfo.name;
    }

    private static void showLaunchError(
            Activity activity) {

        Toast.makeText(
                activity,
                R.string.webapp_diagnostics_launch_failed,
                Toast.LENGTH_SHORT)
                .show();
    }
}
