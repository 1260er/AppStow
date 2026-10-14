package de.pritcloud.appstow;

import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

final class AppIconLoader {

    private final PackageManager packageManager;
    private final Drawable.ConstantState defaultIconState;

    private final ExecutorService executor =
            Executors.newFixedThreadPool(2);

    private final Handler mainHandler =
            new Handler(
                    Looper.getMainLooper());

    // Accessed only on the main thread.
    private final Map<AppEntry, List<PendingBinding>> pendingBindings =
            new IdentityHashMap<>();

    private volatile boolean shutdown;

    private static final class PendingBinding {
        final WeakReference<ImageView> target;
        final Object token;

        PendingBinding(ImageView target, Object token) {
            this.target = new WeakReference<>(target);
            this.token = token;
        }
    }

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

        // A unique token also rejects stale results for the same package
        // after an AppEntry was recreated.
        Object token = new Object();
        target.setTag(token);

        Drawable.ConstantState cachedState = app.iconState;

        if (cachedState != null) {
            target.setImageDrawable(
                    cachedState.newDrawable(target.getResources()));
            return;
        }

        setDefaultIcon(target);

        if (shutdown) {
            return;
        }

        List<PendingBinding> waiting = pendingBindings.get(app);

        if (waiting != null) {
            waiting.add(new PendingBinding(target, token));
            return;
        }

        waiting = new ArrayList<>();
        waiting.add(new PendingBinding(target, token));
        pendingBindings.put(app, waiting);

        try {
            executor.execute(() -> loadIcon(app));
        } catch (RejectedExecutionException exception) {
            pendingBindings.remove(app);
        }
    }

    private void loadIcon(AppEntry app) {

        Drawable.ConstantState loadedState = null;

        try {
            Drawable icon =
                    app.resolveInfo.loadIcon(packageManager);

            if (icon != null) {
                loadedState = icon.getConstantState();
            }
        } catch (RuntimeException ignored) {
        }

        if (loadedState == null) {
            loadedState = defaultIconState;
        }

        Drawable.ConstantState result = loadedState;

        mainHandler.post(() -> {
            List<PendingBinding> waiting =
                    pendingBindings.remove(app);

            if (shutdown || waiting == null) {
                return;
            }

            app.iconState = result;

            for (PendingBinding binding : waiting) {
                ImageView target = binding.target.get();

                if (target == null || target.getTag() != binding.token) {
                    continue;
                }

                if (result != null) {
                    target.setImageDrawable(
                            result.newDrawable(target.getResources()));
                } else {
                    setDefaultIcon(target);
                }
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
        pendingBindings.clear();
    }
}
