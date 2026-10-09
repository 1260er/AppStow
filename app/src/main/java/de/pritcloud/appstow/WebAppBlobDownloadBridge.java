package de.pritcloud.appstow;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.webkit.JavaScriptReplyProxy;
import androidx.webkit.WebMessageCompat;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

final class WebAppBlobDownloadBridge
        implements AutoCloseable {

    private static final String BRIDGE_NAME =
            "AppStowBlobDownload";

    private static final int MAX_CHUNK =
            64 * 1024;

    private static final int MAX_QUEUED_MESSAGES = 8;

    private static final long MAX_FILE_SIZE =
            512L * 1024 * 1024;

    private static final long PENDING_FILE_LIFETIME_SECONDS =
            24L * 60L * 60L;

    private final Activity activity;
    private final Uri allowedOrigin;
    private final ThreadPoolExecutor executor =
            createDownloadExecutor();

    private volatile boolean closed;
    private volatile Session session;

    static ThreadPoolExecutor createDownloadExecutor() {
        return new ThreadPoolExecutor(
                1,
                1,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(
                        MAX_QUEUED_MESSAGES));
    }

    static boolean isAcceptedChunk(byte[] data) {
        return data != null
                && data.length > 0
                && data.length <= MAX_CHUNK;
    }

    private WebAppBlobDownloadBridge(
            Activity activity,
            Uri allowedOrigin) {

        this.activity = activity;
        this.allowedOrigin = allowedOrigin;
    }

    static WebAppBlobDownloadBridge install(
            Activity activity,
            WebView webView,
            Uri initialUri) {

        if (!WebViewFeature.isFeatureSupported(
                WebViewFeature.WEB_MESSAGE_LISTENER)
                || !WebViewFeature.isFeatureSupported(
                WebViewFeature.WEB_MESSAGE_ARRAY_BUFFER)
                || !WebViewFeature.isFeatureSupported(
                WebViewFeature.DOCUMENT_START_SCRIPT)) {

            return null;
        }

        String originRule =
                originRule(initialUri);

        if (originRule == null) {
            return null;
        }

        String script;

        try (InputStream input =
                     activity.getAssets().open(
                             "appstow_blob_download.js");
             ByteArrayOutputStream output =
                     new ByteArrayOutputStream()) {

            byte[] buffer =
                    new byte[4096];

            int count;

            while ((count = input.read(buffer)) != -1) {
                output.write(
                        buffer,
                        0,
                        count);
            }

            script =
                    new String(
                            output.toByteArray(),
                            StandardCharsets.UTF_8);

        } catch (IOException exception) {
            return null;
        }

        WebAppBlobDownloadBridge bridge =
                new WebAppBlobDownloadBridge(
                        activity,
                        Uri.parse(originRule));

        try {
            WebViewCompat.addWebMessageListener(
                    webView,
                    BRIDGE_NAME,
                    Collections.singleton(originRule),
                    (view, message, sourceOrigin,
                     isMainFrame, replyProxy) ->
                            bridge.handleMessage(
                                    view,
                                    message,
                                    sourceOrigin,
                                    isMainFrame,
                                    replyProxy));

            WebViewCompat.addDocumentStartJavaScript(
                    webView,
                    script,
                    Collections.singleton(originRule));

            return bridge;

        } catch (RuntimeException exception) {

            bridge.close();
            return null;
        }
    }

    static String originRule(Uri uri) {

        if (uri == null
                || !"https".equalsIgnoreCase(
                        uri.getScheme())
                || uri.getHost() == null) {

            return null;
        }

        String host =
                uri.getHost();

        if (!host.matches("[A-Za-z0-9.-]+")) {
            return null;
        }

        int port =
                uri.getPort();

        if (port > 65535) {
            return null;
        }

        String origin =
                "https://" + host;

        if (port != -1 && port != 443) {
            origin += ":" + port;
        }

        return origin;
    }

    private static boolean sameOrigin(
            Uri first,
            Uri second) {

        String a = originRule(first);
        String b = originRule(second);

        return a != null
                && a.equalsIgnoreCase(b);
    }

    static String sanitizeFileName(
            String input) {

        if (input == null
                || input.trim().isEmpty()) {

            return "download";
        }

        String value = input.trim();

        StringBuilder result =
                new StringBuilder();

        for (int i = 0; i < value.length(); i++) {

            char character =
                    value.charAt(i);

            if (character == 47
                    || character == 92
                    || Character.isISOControl(character)) {

                result.append("_");

            } else {

                result.append(character);
            }
        }

        String name =
                result.toString().trim();

        if (name.isEmpty()
                || ".".equals(name)
                || "..".equals(name)) {

            return "download";
        }

        if (name.length() > 180) {
            name = name.substring(0, 180);
        }

        return name;
    }

    static String firstAvailableFileName(
            String requested,
            Set<String> existingNames) {

        Set<String> taken =
                new HashSet<>();

        for (String name : existingNames) {
            if (name != null) {
                taken.add(
                        name.toLowerCase(
                                Locale.ROOT));
            }
        }

        if (!taken.contains(
                requested.toLowerCase(
                        Locale.ROOT))) {

            return requested;
        }

        int dot =
                requested.lastIndexOf(".");

        String base =
                dot > 0
                        ? requested.substring(0, dot)
                        : requested;

        String extension =
                dot > 0
                        ? requested.substring(dot)
                        : "";

        for (int number = 1;
             number <= 10000;
             number++) {

            String suffix =
                    " (" + number + ")";

            int maxBaseLength =
                    Math.max(
                            1,
                            180 - suffix.length()
                                    - extension.length());

            String candidateBase =
                    base.length() > maxBaseLength
                            ? base.substring(
                                    0,
                                    maxBaseLength)
                            : base;

            String candidate =
                    candidateBase
                            + suffix
                            + extension;

            if (!taken.contains(
                    candidate.toLowerCase(
                            Locale.ROOT))) {

                return candidate;
            }
        }

        return requested;
    }

    private static String chooseDownloadFileName(
            ContentResolver resolver,
            String requested) {

        Set<String> existing =
                new HashSet<>();

        try (Cursor cursor =
                     resolver.query(
                             MediaStore.Downloads
                                     .EXTERNAL_CONTENT_URI,
                             new String[]{
                                     MediaStore.MediaColumns
                                             .DISPLAY_NAME
                             },
                             MediaStore.MediaColumns
                                     .RELATIVE_PATH + " = ?",
                             new String[]{
                                     Environment.DIRECTORY_DOWNLOADS
                                             + "/"
                             },
                             null)) {

            if (cursor == null) {
                return requested;
            }

            while (cursor.moveToNext()) {

                String name =
                        cursor.getString(0);

                if (name != null) {
                    existing.add(name);
                }
            }

        } catch (RuntimeException ignored) {

            // Bei einem nicht unterstützten Provider
            // bleibt das bisherige Downloadverhalten erhalten.
            return requested;
        }

        return firstAvailableFileName(
                requested,
                existing);
    }

    private static String sanitizeMimeType(
            String input) {

        if (input == null) {
            return "application/octet-stream";
        }

        String value = input.trim();

        if (!value.matches(
                "[A-Za-z0-9.+-]+/[A-Za-z0-9.+-]+")) {

            return "application/octet-stream";
        }

        return value;
    }

    private void handleMessage(
            WebView view,
            WebMessageCompat message,
            Uri sourceOrigin,
            boolean isMainFrame,
            JavaScriptReplyProxy replyProxy) {

        if (closed
                || !isMainFrame
                || !sameOrigin(
                        allowedOrigin,
                        sourceOrigin)
                || view.getUrl() == null
                || !sameOrigin(
                        allowedOrigin,
                        Uri.parse(view.getUrl()))) {

            return;
        }

        if (message.getType()
                == WebMessageCompat.TYPE_ARRAY_BUFFER) {

            byte[] data =
                    message.getArrayBuffer();

            if (!isAcceptedChunk(data)) {
                Session active = session;
                if (active != null) {
                    reply(replyProxy, "error", active.id);
                }
                submit(this::abortSession);
                return;
            }

            Session active = session;

            if (!submit(() ->
                    writeChunk(
                            data,
                            replyProxy))) {

                if (active != null) {
                    reply(replyProxy, "error", active.id);
                }
                showFailure();
            }

            return;
        }

        if (message.getType()
                != WebMessageCompat.TYPE_STRING) {

            return;
        }

        String text = message.getData();

        if (text == null
                || text.length() > 4096) {

            return;
        }

        try {
            JSONObject command =
                    new JSONObject(text);

            String kind =
                    command.optString("kind", "");

            long id =
                    command.optLong("id", 0);

            if (id <= 0) {
                return;
            }

            if ("start".equals(kind)) {

                String name =
                        sanitizeFileName(
                                command.optString(
                                        "fileName",
                                        "download"));

                String mime =
                        sanitizeMimeType(
                                command.optString(
                                        "mimeType",
                                        ""));

                if (!submit(() ->
                        startDownload(
                                id,
                                name,
                                mime,
                                replyProxy))) {

                    reply(replyProxy, "error", id);
                    showFailure();
                }

            } else if ("end".equals(kind)) {

                if (!submit(() ->
                        finishDownload(
                                id,
                                replyProxy))) {

                    reply(replyProxy, "error", id);
                    showFailure();
                }

            } else if ("abort".equals(kind)) {

                submit(() ->
                        abortIfMatching(id));
            }

        } catch (JSONException ignored) {
        }
    }

    private boolean submit(Runnable task) {

        if (closed) {
            return false;
        }

        try {
            executor.execute(task);
            return true;

        } catch (RejectedExecutionException ignored) {
            return false;
        }
    }

    private void startDownload(
            long id,
            String name,
            String mime,
            JavaScriptReplyProxy replyProxy) {

        abortSession();

        ContentResolver resolver =
                activity.getContentResolver();

        Uri uri = null;
        OutputStream output = null;

        try {
            ContentValues values =
                    new ContentValues();

            values.put(
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    chooseDownloadFileName(
                            resolver,
                            name));

            values.put(
                    MediaStore.MediaColumns.MIME_TYPE,
                    mime);

            values.put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS);

            values.put(
                    MediaStore.MediaColumns.IS_PENDING,
                    1);

            values.put(
                    MediaStore.MediaColumns.DATE_EXPIRES,
                    System.currentTimeMillis() / 1000L
                            + PENDING_FILE_LIFETIME_SECONDS);

            uri = resolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    values);

            if (uri == null) {
                throw new IOException(
                        "Download creation failed");
            }

            output =
                    resolver.openOutputStream(uri, "w");

            if (output == null) {
                throw new IOException(
                        "Download stream unavailable");
            }

            session =
                    new Session(id, uri, output);

            reply(replyProxy, "ready", id);

        } catch (IOException | RuntimeException exception) {

            closeQuietly(output);
            deleteQuietly(uri);

            reply(replyProxy, "error", id);
            showFailure();
        }
    }

    private void writeChunk(
            byte[] data,
            JavaScriptReplyProxy replyProxy) {

        Session current = session;

        if (current == null) {
            return;
        }

        if (!isAcceptedChunk(data)
                || current.bytes > MAX_FILE_SIZE - data.length) {

            long id = current.id;

            abortSession();
            reply(replyProxy, "error", id);
            showFailure();
            return;
        }

        try {
            current.output.write(data);

            current.bytes += data.length;

            reply(
                    replyProxy,
                    "next",
                    current.id);

        } catch (IOException | RuntimeException exception) {

            long id = current.id;

            abortSession();

            reply(replyProxy, "error", id);
            showFailure();
        }
    }

    private void finishDownload(
            long id,
            JavaScriptReplyProxy replyProxy) {

        Session current = session;

        if (current == null
                || current.id != id) {

            reply(replyProxy, "error", id);
            return;
        }

        session = null;

        try {
            current.output.flush();
            current.output.close();

            ContentValues values =
                    new ContentValues();

            values.put(
                    MediaStore.MediaColumns.IS_PENDING,
                    0);

            values.putNull(
                    MediaStore.MediaColumns.DATE_EXPIRES);

            int updated =
                    activity.getContentResolver().update(
                            current.uri,
                            values,
                            null,
                            null);

            if (updated <= 0) {
                throw new IOException(
                        "Download publication failed");
            }

            reply(replyProxy, "done", id);

            showToast(
                    R.string.webapp_download_completed);

        } catch (IOException | RuntimeException exception) {

            closeQuietly(current.output);
            deleteQuietly(current.uri);

            reply(replyProxy, "error", id);
            showFailure();
        }
    }

    private void abortIfMatching(long id) {

        if (session != null
                && session.id == id) {

            abortSession();
        }
    }

    void cancelActive() {
        submit(this::abortSession);
    }

    private void abortSession() {

        Session current = session;
        session = null;

        if (current != null) {

            closeQuietly(current.output);
            deleteQuietly(current.uri);
        }
    }

    private void deleteQuietly(Uri uri) {

        if (uri == null) {
            return;
        }

        try {
            activity.getContentResolver().delete(
                    uri,
                    null,
                    null);

        } catch (RuntimeException ignored) {
        }
    }

    private static void closeQuietly(
            OutputStream output) {

        if (output == null) {
            return;
        }

        try {
            output.close();
        } catch (IOException | RuntimeException ignored) {
        }
    }

    private void reply(
            JavaScriptReplyProxy proxy,
            String kind,
            long id) {

        activity.runOnUiThread(() -> {

            if (closed
                    || activity.isFinishing()
                    || activity.isDestroyed()) {

                return;
            }

            String message =
                    "{\"kind\":\""
                            + kind
                            + "\",\"id\":"
                            + id
                            + "}";

            try {
                proxy.postMessage(message);
            } catch (RuntimeException ignored) {
            }
        });
    }

    private void showFailure() {
        showToast(
                R.string.webapp_download_failed);
    }

    private void showToast(int resource) {

        activity.runOnUiThread(() -> {

            if (!closed
                    && !activity.isFinishing()
                    && !activity.isDestroyed()) {

                Toast.makeText(
                        activity,
                        resource,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void close() {

        if (closed) {
            return;
        }

        closed = true;

        // Bei ueberfuellter Queue zuerst wartende
        // Nachrichten verwerfen, dann sauber abbrechen.
        executor.getQueue().clear();

        try {
            executor.execute(this::abortSession);
        } catch (RejectedExecutionException ignored) {
            // Der laufende Worker kann noch blockiert sein.
            // Die Pending-Datei besitzt ein Ablaufdatum.
        }

        executor.shutdown();
    }

    private static final class Session {

        final long id;
        final Uri uri;
        final OutputStream output;

        long bytes;

        Session(
                long id,
                Uri uri,
                OutputStream output) {

            this.id = id;
            this.uri = uri;
            this.output = output;
        }
    }
}
