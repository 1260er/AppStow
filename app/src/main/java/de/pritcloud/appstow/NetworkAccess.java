package de.pritcloud.appstow;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.provider.Settings;

final class NetworkAccess {

    private NetworkAccess() {
    }

    static boolean hasUsableNetwork(
            Context context) {

        ConnectivityManager manager =
                (ConnectivityManager)
                        context.getSystemService(
                                Context.CONNECTIVITY_SERVICE);

        if (manager == null) {
            return false;
        }

        Network network =
                manager.getActiveNetwork();

        if (network == null) {
            return false;
        }

        NetworkCapabilities capabilities =
                manager.getNetworkCapabilities(
                        network);

        return capabilities != null
                && capabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && capabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    static void showNetworkHelp(
            Activity activity) {

        new AlertDialog.Builder(activity)
                .setTitle(
                        R.string.webapp_network_title)
                .setMessage(
                        R.string.webapp_network_message)
                .setPositiveButton(
                        R.string.webapp_network_open_network_settings,
                        (dialog, which) ->
                                openNetworkSettings(
                                        activity))
                .setNeutralButton(
                        R.string.webapp_network_open_settings,
                        (dialog, which) ->
                                openAppSettings(
                                        activity))
                .setNegativeButton(
                        R.string.action_cancel,
                        null)
                .show();
    }

    static void openAppSettings(
            Context context) {

        Intent intent =
                new Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts(
                                "package",
                                context.getPackageName(),
                                null));

        tryStartActivity(
                context,
                intent);
    }

    private static void openNetworkSettings(
            Context context) {

        if (tryStartActivity(
                context,
                new Intent(
                        Settings.ACTION_WIRELESS_SETTINGS))) {

            return;
        }

        tryStartActivity(
                context,
                new Intent(
                        Settings.ACTION_SETTINGS));
    }

    private static boolean tryStartActivity(
            Context context,
            Intent intent) {

        try {
            context.startActivity(intent);
            return true;

        } catch (RuntimeException exception) {
            return false;
        }
    }
}
