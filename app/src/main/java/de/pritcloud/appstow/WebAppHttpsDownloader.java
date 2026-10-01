package de.pritcloud.appstow;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.webkit.URLUtilCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

final class WebAppHttpsDownloader
        implements AutoCloseable {

    private static final long MAX_BYTES =
            512L * 1024 * 1024;

    private static final int MAX_REDIRECTS = 5;

    private static final int CONNECT_TIMEOUT_MS =
            15000;

    private static final int READ_TIMEOUT_MS =
            30000;

    private static final long MAX_DURATION_MS =
            60L * 60 * 1000;

    private final Activity activity;

    private final ExecutorService executor =
            new ThreadPoolExecutor(
                    1,
                    1,
                    0L,
                    TimeUnit.MILLISECONDS,
                    new ArrayBlockingQueue<>(2));

    private volatile boolean closed;
    private volatile HttpsURLConnection activeConnection;

    WebAppHttpsDownloader(Activity activity) {
        this.activity = activity;
    }

    static boolean isAllowedHttpsUri(Uri uri) {

        if (uri == null
                || !"https".equalsIgnoreCase(
                        uri.getScheme())
                || uri.getHost() == null
                || uri.getHost().isEmpty()
                || uri.getUserInfo() != null
                || uri.getPort() == 0
                || uri.getPort() > 65535) {

            return false;
        }

        try {
            URI parsed =
                    new URI(uri.toString());

            return "https".equalsIgnoreCase(
                    parsed.getScheme())
                    && parsed.getHost() != null
                    && parsed.getRawUserInfo() == null;

        } catch (URISyntaxException exception) {
            return false;
        }
    }

    static boolean sameHttpsOrigin(
            Uri first,
            Uri second) {

        if (!isAllowedHttpsUri(first)
                || !isAllowedHttpsUri(second)) {

            return false;
        }

        int firstPort =
                first.getPort() == -1
                        ? 443
                        : first.getPort();

        int secondPort =
                second.getPort() == -1
                        ? 443
                        : second.getPort();

        return firstPort == secondPort
                && first.getHost().equalsIgnoreCase(
                        second.getHost());
    }

    static boolean maySendCredentials(
            Uri configured,
            Uri current,
            Uri target) {

        return sameHttpsOrigin(
                configured,
                current)
                && sameHttpsOrigin(
                current,
                target);
    }

    static Uri resolveRedirect(
            Uri previous,
            String location) {

        if (!isAllowedHttpsUri(previous)
                || location == null
                || location.trim().isEmpty()
                || location.length() > 8192) {

            return null;
        }

        try {
            URI base =
                    new URI(previous.toString());

            URI target =
                    base.resolve(
                            new URI(location));

            Uri result =
                    Uri.parse(
                            target.toString())
                            .buildUpon()
                            .fragment(null)
                            .build();

            return isAllowedHttpsUri(result)
                    ? result
                    : null;

        } catch (URISyntaxException
                 | RuntimeException exception) {

            return null;
        }
    }

    static String sanitizeFileName(String input) {

        if (input == null
                || input.trim().isEmpty()) {

            return "download";
        }

        StringBuilder result =
                new StringBuilder();

        for (int index = 0;
             index < input.length();
             index++) {

            char character =
                    input.charAt(index);

            if (character == 47
                    || character == 92
                    || Character.isISOControl(
                            character)) {

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

    private static String normalizeMime(
            String input) {

        if (input == null) {
            return "application/octet-stream";
        }

        String value =
                input.split(";", 2)[0]
                        .trim()
                        .toLowerCase(Locale.ROOT);

        return value.matches(
                "[a-z0-9.+-]+/[a-z0-9.+-]+")
                ? value
                : "application/octet-stream";
    }

    static boolean isUnexpectedHtml(
            String responseMime,
            String expectedMime,
            String requestedFileName) {

        if (!"text/html".equals(
                normalizeMime(responseMime))) {

            return false;
        }

        if ("text/html".equals(
                normalizeMime(expectedMime))) {

            return false;
        }

        String name =
                requestedFileName == null
                        ? ""
                        : requestedFileName.toLowerCase(
                                Locale.ROOT);

        return !name.endsWith(".html")
                && !name.endsWith(".htm");
    }

    private static boolean isRedirect(
            int status) {

        return status == 301
                || status == 302
                || status == 303
                || status == 307
                || status == 308;
    }

    void enqueue(
            Uri uri,
            String userAgent,
            String contentDisposition,
            String mimeType,
            String cookies,
            String referer) {

        if (closed
                || !isAllowedHttpsUri(uri)) {

            showToast(
                    R.string.webapp_download_failed);
            return;
        }

        try {
            executor.execute(
                    () -> performDownload(
                            uri,
                            userAgent,
                            contentDisposition,
                            mimeType,
                            cookies,
                            referer));

            showToast(
                    R.string.webapp_download_started);

        } catch (RejectedExecutionException exception) {

            showToast(
                    R.string.webapp_download_failed);
        }
    }

    private void performDownload(
            Uri original,
            String userAgent,
            String contentDisposition,
            String requestedMime,
            String cookies,
            String referer) {

        Uri current =
                original;

        boolean forwardSensitiveHeaders =
                true;

        long started =
                android.os.SystemClock.elapsedRealtime();

        boolean completed =
                false;

        Uri outputUri =
                null;

        HttpsURLConnection connection =
                null;

        try {
            for (int redirects = 0;
                 redirects <= MAX_REDIRECTS;
                 redirects++) {

                if (closed
                        || Thread.currentThread().isInterrupted()) {

                    throw new IOException(
                            "Download cancelled");
                }

                URL url =
                        new URL(
                                current.toString());

                java.net.URLConnection opened =
                        url.openConnection();

                if (!(opened
                        instanceof HttpsURLConnection)) {

                    throw new IOException(
                            "HTTPS required");
                }

                connection =
                        (HttpsURLConnection) opened;

                activeConnection =
                        connection;

                connection.setInstanceFollowRedirects(
                        false);

                connection.setConnectTimeout(
                        CONNECT_TIMEOUT_MS);

                connection.setReadTimeout(
                        READ_TIMEOUT_MS);

                connection.setUseCaches(false);

                connection.setRequestMethod("GET");

                connection.setRequestProperty(
                        "Accept-Encoding",
                        "identity");

                connection.setRequestProperty(
                        "Connection",
                        "close");

                if (userAgent != null
                        && !userAgent.isEmpty()
                        && userAgent.length() <= 512
                        && !userAgent.contains("\r")
                        && !userAgent.contains("\n")) {

                    connection.setRequestProperty(
                            "User-Agent",
                            userAgent);
                }

                if (forwardSensitiveHeaders) {

                    if (cookies != null
                            && !cookies.trim().isEmpty()
                            && !cookies.contains("\r")
                            && !cookies.contains("\n")) {

                        connection.setRequestProperty(
                                "Cookie",
                                cookies);
                    }

                    if (referer != null
                            && !referer.isEmpty()
                            && !referer.contains("\r")
                            && !referer.contains("\n")) {

                        connection.setRequestProperty(
                                "Referer",
                                referer);
                    }
                }

                int status =
                        connection.getResponseCode();

                if (isRedirect(status)) {

                    Uri next =
                            resolveRedirect(
                                    current,
                                    connection.getHeaderField(
                                            "Location"));

                    if (next == null
                            || redirects == MAX_REDIRECTS) {

                        throw new IOException(
                                "Unsupported redirect");
                    }

                    if (!sameHttpsOrigin(
                            original,
                            next)) {

                        forwardSensitiveHeaders =
                                false;
                    }

                    connection.disconnect();
                    activeConnection = null;
                    connection = null;

                    current = next;
                    continue;
                }

                if (status != HttpURLConnection.HTTP_OK) {

                    throw new IOException(
                            "HTTP status " + status);
                }

                long expectedLength =
                        connection.getContentLengthLong();

                if (expectedLength > MAX_BYTES) {

                    throw new IOException(
                            "Download too large");
                }

                String responseMime =
                        connection.getContentType();

                String finalDisposition =
                        connection.getHeaderField(
                                "Content-Disposition");

                if (finalDisposition == null
                        || finalDisposition.trim().isEmpty()) {

                    finalDisposition =
                            contentDisposition;
                }

                String finalMime =
                        responseMime == null
                                ? normalizeMime(requestedMime)
                                : normalizeMime(responseMime);

                String fileName =
                        sanitizeFileName(
                                URLUtilCompat.guessFileName(
                                        original.toString(),
                                        finalDisposition,
                                        finalMime));

                if (isUnexpectedHtml(
                        responseMime,
                        requestedMime,
                        fileName)) {

                    throw new IOException(
                            "Unexpected HTML response");
                }

                ContentResolver resolver =
                        activity.getContentResolver();

                ContentValues values =
                        new ContentValues();

                values.put(
                        MediaStore.MediaColumns.DISPLAY_NAME,
                        fileName);

                values.put(
                        MediaStore.MediaColumns.MIME_TYPE,
                        finalMime);

                values.put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_DOWNLOADS);

                values.put(
                        MediaStore.MediaColumns.IS_PENDING,
                        1);

                values.put(
                        MediaStore.MediaColumns.DATE_EXPIRES,
                        System.currentTimeMillis() / 1000L
                                + 86400L);

                try (InputStream input =
                             connection.getInputStream()) {

                    outputUri =
                            resolver.insert(
                                    MediaStore.Downloads
                                            .EXTERNAL_CONTENT_URI,
                                    values);

                    if (outputUri == null) {

                        throw new IOException(
                                "Could not create download");
                    }

                    OutputStream openedOutput =
                            resolver.openOutputStream(
                                    outputUri,
                                    "w");

                    if (openedOutput == null) {

                        throw new IOException(
                                "Could not open download");
                    }

                    long written =
                            0L;

                    try (OutputStream output =
                                 openedOutput) {

                        byte[] buffer =
                                new byte[32768];

                        int count;

                        while ((count =
                                input.read(buffer)) != -1) {

                            if (closed
                                    || Thread.currentThread()
                                            .isInterrupted()) {

                                throw new IOException(
                                        "Download cancelled");
                            }

                            if (android.os.SystemClock
                                    .elapsedRealtime()
                                    - started
                                    > MAX_DURATION_MS) {

                                throw new IOException(
                                        "Download timeout");
                            }

                            if (written
                                    > MAX_BYTES - count) {

                                throw new IOException(
                                        "Download too large");
                            }

                            output.write(
                                    buffer,
                                    0,
                                    count);

                            written += count;
                        }

                        output.flush();
                    }

                    if (expectedLength >= 0
                            && written != expectedLength) {

                        throw new IOException(
                                "Incomplete download");
                    }
                }

                if (closed) {

                    throw new IOException(
                            "Download cancelled");
                }

                ContentValues publish =
                        new ContentValues();

                publish.put(
                        MediaStore.MediaColumns.IS_PENDING,
                        0);

                publish.putNull(
                        MediaStore.MediaColumns.DATE_EXPIRES);

                int updated =
                        resolver.update(
                                outputUri,
                                publish,
                                null,
                                null);

                if (updated <= 0) {

                    throw new IOException(
                            "Could not publish download");
                }

                completed = true;

                showToast(
                        R.string.webapp_download_completed);

                return;
            }

            throw new IOException(
                    "Too many redirects");

        } catch (IOException
                 | RuntimeException exception) {

            if (!closed) {
                showToast(
                        R.string.webapp_download_failed);
            }

        } finally {

            if (connection != null) {
                connection.disconnect();
            }

            activeConnection = null;

            if (!completed
                    && outputUri != null) {

                try {
                    activity.getContentResolver()
                            .delete(
                                    outputUri,
                                    null,
                                    null);

                } catch (RuntimeException ignored) {
                }
            }
        }
    }

    private void showToast(int resource) {

        activity.runOnUiThread(() -> {

            if (!closed
                    && !activity.isFinishing()
                    && !activity.isDestroyed()) {

                Toast.makeText(
                        activity,
                        resource,
                        Toast.LENGTH_SHORT)
                        .show();
            }
        });
    }

    @Override
    public void close() {

        if (closed) {
            return;
        }

        closed = true;

        executor.shutdownNow();

        HttpsURLConnection connection =
                activeConnection;

        if (connection != null) {
            connection.disconnect();
        }
    }
}
