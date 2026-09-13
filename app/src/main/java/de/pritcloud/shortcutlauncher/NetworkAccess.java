package de.pritcloud.shortcutlauncher;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
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

        if (Build.VERSION.SDK_INT
                >= Build.VERSION_CODES.M) {

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

        NetworkInfo networkInfo =
                manager.getActiveNetworkInfo();

        return networkInfo != null
                && networkInfo.isConnected();
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

        context.startActivity(intent);
    }
}
