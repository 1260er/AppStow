package de.pritcloud.appstow;

import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class AppIconLoader {

    private final PackageManager packageManager;
    private final Drawable.ConstantState defaultIconState;

    private final ExecutorService executor =
            Executors.newFixedThreadPool(2);

    private final Handler mainHandler =
            new Handler(
                    Looper.getMainLooper());

    private volatile boolean shutdown;

    AppIconLoader(
            PackageManager packageManager) {

        this.packageManager =
                packageManager;

        Drawable defaultIcon =
                packageManager
                        .getDefaultActivityIcon();

        defaultIconState =
                defaultIcon != null
                        ? defaultIcon.getConstantState()
                        : null;
    }

    void bind(
            ImageView target,
            AppEntry app) {

        String key =
                app.packageName
                        + "/"
                        + app.resolveInfo.activityInfo.name;

        target.setTag(
                key);

        Drawable.ConstantState cachedState =
                app.iconState;

        if (cachedState != null) {
            target.setImageDrawable(
                    cachedState.newDrawable(
                            target.getResources()));
            return;
        }

        setDefaultIcon(
                target);

        if (shutdown) {
            return;
        }

        try {
            executor.execute(() ->
                    loadIcon(
                            target,
                            app,
                            key));

        } catch (RuntimeException ignored) {
        }
    }

    private void loadIcon(
            ImageView target,
            AppEntry app,
            String key) {

        Drawable.ConstantState loadedState =
                null;

        try {
            Drawable icon =
                    app.resolveInfo.loadIcon(
                            packageManager);

            if (icon != null) {
                loadedState =
                        icon.getConstantState();
            }

        } catch (RuntimeException ignored) {
        }

        if (loadedState == null) {
            loadedState =
                    defaultIconState;
        }

        app.iconState =
                loadedState;

        Drawable.ConstantState result =
                loadedState;

        mainHandler.post(() -> {

            if (shutdown
                    || !key.equals(
                            target.getTag())) {

                return;
            }

            if (result != null) {
                target.setImageDrawable(
                        result.newDrawable(
                                target.getResources()));
            } else {
                setDefaultIcon(
                        target);
            }
        });
    }

    private void setDefaultIcon(
            ImageView target) {

        if (defaultIconState != null) {
            target.setImageDrawable(
                    defaultIconState.newDrawable(
                            target.getResources()));
            return;
        }

        target.setImageDrawable(
                packageManager
                        .getDefaultActivityIcon());
    }

    void shutdown() {
        shutdown = true;
        executor.shutdownNow();
        mainHandler.removeCallbacksAndMessages(
                null);
    }
}
