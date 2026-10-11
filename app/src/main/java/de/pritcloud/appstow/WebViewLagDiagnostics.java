package de.pritcloud.appstow;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewRenderProcess;
import android.webkit.WebViewRenderProcessClient;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Temporary, development-only WebView diagnostic.
 *
 * No URL, site title, DOM, JavaScript, keystroke, user-agent,
 * network request, exception text, or chat content is collected.
 * No WebSettings or page code are modified.
 * Delete this class and its small hooks in WebAppActivity to remove it.
 */
final class WebViewLagDiagnostics implements AutoCloseable {

    static final int MAX_EVENTS = 180;
    private static final long SAMPLE_EVERY_MS = 2000L;
    private static final long UI_DELAY_THRESHOLD_MS = 350L;

    static boolean enabledForPackage(String packageName) {
        return packageName != null && packageName.endsWith(".dev");
    }

    static final class EventBuffer {
        private final int capacity;
        private final ArrayDeque<String> entries = new ArrayDeque<>();

        EventBuffer(int capacity) {
            if (capacity < 1) {
                throw new IllegalArgumentException("capacity must be positive");
            }
            this.capacity = capacity;
        }

        void add(String event) {
            if (entries.size() == capacity) {
                entries.removeFirst();
            }
            entries.addLast(event);
        }

        List<String> snapshot() {
            return new ArrayList<>(entries);
        }
    }

    private final Activity activity;
    private final WebView webView;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final EventBuffer events = new EventBuffer(MAX_EVENTS);
    private final long startedAtMs = SystemClock.elapsedRealtime();

    private TextView copyButton;
    private boolean running;
    private boolean closed;
    private long lastTickUptimeMs;
    private long lastCpuMs;

    private final Runnable sampler = new Runnable() {
        @Override
        public void run() {
            if (!running || closed) {
                return;
            }

            long now = SystemClock.uptimeMillis();
            long cpu = Process.getElapsedCpuTime();
            long delay = Math.max(0L, now - lastTickUptimeMs
                    - SAMPLE_EVERY_MS);
            long cpuDelta = Math.max(0L, cpu - lastCpuMs);
            long elapsed = Math.max(1L, now - lastTickUptimeMs);

            Runtime runtime = Runtime.getRuntime();
            long heapMiB = (runtime.totalMemory()
                    - runtime.freeMemory()) / (1024L * 1024L);

            note("sample cpuMs=" + cpuDelta + "/" + elapsed
                    + " heapMiB=" + heapMiB
                    + " mainDelayMs=" + delay);

            if (delay >= UI_DELAY_THRESHOLD_MS) {
                note("MAIN_THREAD_DELAY delayMs=" + delay);
            }

            lastTickUptimeMs = now;
            lastCpuMs = cpu;
            handler.postDelayed(this, SAMPLE_EVERY_MS);
        }
    };

    private WebViewLagDiagnostics(Activity activity, WebView webView) {
        this.activity = activity;
        this.webView = webView;
    }

    static WebViewLagDiagnostics attach(Activity activity, WebView webView) {
        WebViewLagDiagnostics diagnostics =
                new WebViewLagDiagnostics(activity, webView);
        diagnostics.note("diagnostic_attached");
        diagnostics.installRendererObserver();
        diagnostics.installCopyButton();
        return diagnostics;
    }

    private void installRendererObserver() {
        try {
            webView.setWebViewRenderProcessClient(
                    new WebViewRenderProcessClient() {
                        @Override
                        public void onRenderProcessUnresponsive(
                                WebView view, WebViewRenderProcess renderer) {
                            note("RENDERER_UNRESPONSIVE");
                        }

                        @Override
                        public void onRenderProcessResponsive(
                                WebView view, WebViewRenderProcess renderer) {
                            note("RENDERER_RESPONSIVE");
                        }
                    });
            note("renderer_observer_active");
        } catch (RuntimeException exception) {
            note("renderer_observer_unavailable");
        }
    }

    private void installCopyButton() {
        try {
            FrameLayout root = activity.findViewById(android.R.id.content);
            if (root == null) {
                note("copy_button_unavailable");
                return;
            }

            TextView button = new TextView(activity);
            button.setText("LOG");
            button.setTextColor(Color.WHITE);
            button.setTextSize(12f);
            button.setGravity(Gravity.CENTER);
            button.setContentDescription(
                    "WebView-Diagnose in die Zwischenablage kopieren");
            button.setBackgroundColor(0xCC303030);
            button.setElevation(dp(3));
            button.setOnClickListener(view -> copyReport());

            FrameLayout.LayoutParams params =
                    new FrameLayout.LayoutParams(
                            dp(52), dp(36), Gravity.TOP | Gravity.END);
            params.topMargin = dp(8);
            params.rightMargin = dp(8);
            root.addView(button, params);

            copyButton = button;
            note("copy_button_ready");
        } catch (RuntimeException exception) {
            note("copy_button_unavailable");
        }
    }

    private int dp(int value) {
        return Math.round(value * activity.getResources()
                .getDisplayMetrics().density);
    }

    void note(String fixedTechnicalEvent) {
        if (closed) {
            return;
        }
        // Call only with locally generated technical labels and numbers.
        long offsetMs = SystemClock.elapsedRealtime() - startedAtMs;
        events.add("+" + offsetMs + "ms " + fixedTechnicalEvent);
    }

    void resume() {
        if (closed || running) {
            return;
        }
        note("activity_resumed");
        running = true;
        lastTickUptimeMs = SystemClock.uptimeMillis();
        lastCpuMs = Process.getElapsedCpuTime();
        handler.postDelayed(sampler, SAMPLE_EVERY_MS);
    }

    void pause() {
        if (running) {
            note("activity_paused");
        }
        running = false;
        handler.removeCallbacks(sampler);
    }

    private void copyReport() {
        note("USER_PRESSED_LOG_DURING_SUSPECTED_LAG");
        String report = buildReport();
        ClipboardManager clipboard = (ClipboardManager)
                activity.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) {
            Toast.makeText(activity,
                    "Zwischenablage nicht verfügbar", Toast.LENGTH_SHORT).show();
            return;
        }
        clipboard.setPrimaryClip(
                ClipData.newPlainText("AppStow WebView Diagnose", report));
        Toast.makeText(activity,
                "Technisches Protokoll kopiert", Toast.LENGTH_SHORT).show();
    }

    private String buildReport() {
        StringBuilder report = new StringBuilder();
        report.append("AppStow WebView Diagnose (nur technische Daten)\n")
                .append("androidSdk=").append(Build.VERSION.SDK_INT)
                .append("\nprocess=webapp")
                .append("\nsessionDurationMs=")
                .append(SystemClock.elapsedRealtime() - startedAtMs);

        PackageInfo provider = WebView.getCurrentWebViewPackage();
        report.append("\nwebViewVersion=")
                .append(provider == null ? "unknown" : provider.versionName);

        Runtime runtime = Runtime.getRuntime();
        report.append("\njavaHeapUsedMiB=")
                .append((runtime.totalMemory() - runtime.freeMemory())
                        / (1024L * 1024L));

        ActivityManager manager = (ActivityManager)
                activity.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            ActivityManager.MemoryInfo memory =
                    new ActivityManager.MemoryInfo();
            manager.getMemoryInfo(memory);
            report.append("\nsystemAvailableMiB=")
                    .append(memory.availMem / (1024L * 1024L));
            report.append("\nsystemLowMemory=").append(memory.lowMemory);
        }

        WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(webView);
        report.append("\nkeyboardVisible=")
                .append(insets == null ? "unknown"
                        : insets.isVisible(WindowInsetsCompat.Type.ime()));

        report.append("\neventsLast").append(MAX_EVENTS)
                .append("=\n");
        for (String event : events.snapshot()) {
            report.append(event).append('\n');
        }
        report.append("Hinweis: Werte sind App-Prozess-Messungen; ")
                .append("Chromium-Renderer hat eigene Threads/Prozesse.\n");
        return report.toString();
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        pause();
        closed = true;

        if (copyButton != null
                && copyButton.getParent() instanceof ViewGroup) {
            ((ViewGroup) copyButton.getParent()).removeView(copyButton);
        }
        copyButton = null;

        try {
            webView.setWebViewRenderProcessClient(null);
        } catch (RuntimeException ignored) {
            // The renderer may have already exited; never alter its lifecycle.
        }
    }
}
