package de.pritcloud.appstow;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.ValueCallback;
import android.webkit.MimeTypeMap;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressLint({"SetJavaScriptEnabled", "WebViewApiAvailability", "ObsoleteSdkInt"})
public class WebAppActivity extends Activity {

    static final String EXTRA_URL =
            "web_app_url";

    private static final int REQUEST_FILE_CHOOSER = 1001;
    private static final int REQUEST_FILE_CAMERA_PERMISSION = 1002;
    private static final int REQUEST_WEB_CAMERA_PERMISSION = 1003;
    private static final int REQUEST_WEB_MEDIA_PERMISSION = 1004;
    private static final int REQUEST_WEB_GEOLOCATION_PERMISSION = 1005;

    private static final long TEMP_FILE_MAX_AGE_MS =
            7L * 24L * 60L * 60L * 1000L;

    private WebView webView;
    private Uri webAppInitialOrigin;
    private AlertDialog errorDialog;
    private View customFullscreenView;
    private WebChromeClient.CustomViewCallback customFullscreenCallback;
    private WebAppBlobDownloadBridge blobDownloadBridge;
    private WebAppHttpsDownloader httpsDownloader;

    private ValueCallback<Uri[]> filePathCallback;
    private WebChromeClient.FileChooserParams pendingFileChooserParams;
    private Uri pendingCameraCaptureUri;
    private File pendingCameraCaptureFile;
    private PermissionRequest pendingWebPermissionRequest;
    private String pendingGeolocationOrigin;
    private GeolocationPermissions.Callback pendingGeolocationCallback;

    private final ExecutorService uploadCopyExecutor =
            Executors.newSingleThreadExecutor();

    private final Set<File> managedTempArtifacts =
            Collections.synchronizedSet(
                    new HashSet<>());

    private final OnBackInvokedCallback webHistoryBackCallback =
            () -> {

                if (customFullscreenView
                        != null) {

                    hideCustomFullscreenView();
                    return;
                }

                if (webView == null
                        || !webView.canGoBack()) {

                    return;
                }

                webView.goBack();
                webView.post(
                        this::updateWebHistoryBackCallback);
            };

    private boolean webHistoryBackCallbackRegistered;

    @Override
    protected void onCreate(
            Bundle savedInstanceState) {

        super.onCreate(
                savedInstanceState);

        try {
            setContentView(
                    R.layout.activity_web_app);

            webView =
                    findViewById(
                            R.id.webAppView);

            applySafeAreaInsets();

        } catch (RuntimeException exception) {

            showFatalWebViewError(
                    getString(
                            R.string.webview_start_failed_title),
                    exception.toString());

            return;
        }

        String url =
                getIntent()
                        .getStringExtra(
                                EXTRA_URL);

        if (url == null
                || url.trim().isEmpty()) {

            finish();
            return;
        }

        Uri initialUri =
                Uri.parse(
                        url.trim());

        if (!isHttpsUri(initialUri)) {
            Toast.makeText(
                    this,
                    R.string.shortcut_launch_failed,
                    Toast.LENGTH_SHORT)
                    .show();

            finish();
            return;
        }

        url =
                initialUri.toString();

        cleanupStaleWebAppTempFilesAsync();

        try {
            configureWebView(initialUri);

            boolean restored =
                    false;

            if (savedInstanceState != null) {
                restored =
                        webView.restoreState(
                                savedInstanceState)
                                != null;
            }

            if (!restored) {
                webView.loadUrl(
                        url);
            }

            updateWebHistoryBackCallback();

        } catch (RuntimeException exception) {

            showFatalWebViewError(
                    getString(
                            R.string.webapp_load_failed_title),
                    exception.toString());
        }
    }

    private void applySafeAreaInsets() {

        View contentRoot =
                findViewById(
                        android.R.id.content);

        if (contentRoot == null) {
            return;
        }

        ViewCompat.setOnApplyWindowInsetsListener(
                contentRoot,
                (view, insets) -> {

                    Insets safeInsets =
                            insets.getInsets(
                                    WindowInsetsCompat.Type.systemBars()
                                            | WindowInsetsCompat.Type.displayCutout());

                    view.setPadding(
                            safeInsets.left,
                            safeInsets.top,
                            safeInsets.right,
                            safeInsets.bottom);

                    return insets;
                });

        ViewCompat.requestApplyInsets(
                contentRoot);
    }

    private void updateWebHistoryBackCallback() {

        boolean shouldRegister =
                customFullscreenView != null
                        || (webView != null
                        && webView.canGoBack());

        OnBackInvokedDispatcher dispatcher =
                getOnBackInvokedDispatcher();

        if (shouldRegister
                && !webHistoryBackCallbackRegistered) {

            dispatcher.registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                    webHistoryBackCallback);

            webHistoryBackCallbackRegistered = true;
            return;
        }

        if (!shouldRegister
                && webHistoryBackCallbackRegistered) {

            dispatcher.unregisterOnBackInvokedCallback(
                    webHistoryBackCallback);

            webHistoryBackCallbackRegistered = false;
        }
    }

    private void configureWebView(Uri initialUri) {

        webAppInitialOrigin = initialUri;

        WebSettings settings =
                webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);

        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setGeolocationEnabled(true);

        if (WebViewFeature.isFeatureSupported(
                WebViewFeature.WEB_AUTHENTICATION)) {

            WebSettingsCompat.setWebAuthenticationSupport(
                    settings,
                    WebSettingsCompat.WEB_AUTHENTICATION_SUPPORT_FOR_APP);
        }

        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setMixedContentMode(
                WebSettings.MIXED_CONTENT_NEVER_ALLOW);

        blobDownloadBridge =
                WebAppBlobDownloadBridge.install(
                        this,
                        webView,
                        initialUri);

        httpsDownloader =
                new WebAppHttpsDownloader(this);

        CookieManager cookieManager =
                CookieManager.getInstance();

        cookieManager.setAcceptCookie(true);

        cookieManager.setAcceptThirdPartyCookies(
                webView,
                true);

        webView.setWebChromeClient(
                new WebChromeClient() {

                    @Override
                    public boolean onShowFileChooser(
                            WebView view,
                            ValueCallback<Uri[]> callback,
                            FileChooserParams params) {

                        return handleFileChooser(
                                callback,
                                params);
                    }

                    @Override
                    public void onShowCustomView(
                            View view,
                            CustomViewCallback callback) {

                        showCustomFullscreenView(
                                view,
                                callback);
                    }

                    @Override
                    public void onHideCustomView() {

                        hideCustomFullscreenView();
                    }

                    @Override
                    public void onPermissionRequest(
                            PermissionRequest request) {

                        handleWebPermissionRequest(
                                request);
                    }

                    @Override
                    public void onPermissionRequestCanceled(
                            PermissionRequest request) {

                        if (pendingWebPermissionRequest
                                == request) {

                            pendingWebPermissionRequest =
                                    null;
                        }
                    }

                    @Override
                    public void onGeolocationPermissionsShowPrompt(
                            String origin,
                            GeolocationPermissions.Callback callback) {

                        handleGeolocationPermissionRequest(
                                origin,
                                callback);
                    }

                    @Override
                    public void onGeolocationPermissionsHidePrompt() {

                        denyPendingGeolocationRequest();
                    }
                });

        webView.setDownloadListener(
                this::handleDownload);

        webView.setWebViewClient(
                new WebViewClient() {

                    @Override
                    public void onPageStarted(
                            WebView view,
                            String url,
                            Bitmap favicon) {

                        super.onPageStarted(
                                view,
                                url,
                                favicon);

                        if (blobDownloadBridge != null) {
                            blobDownloadBridge.cancelActive();
                        }
                    }

                    @Override
                    public boolean shouldOverrideUrlLoading(
                            WebView view,
                            WebResourceRequest request) {

                        return handleUri(
                                request.getUrl());
                    }

                    @Override
                    public void doUpdateVisitedHistory(
                            WebView view,
                            String url,
                            boolean isReload) {

                        super.doUpdateVisitedHistory(
                                view,
                                url,
                                isReload);

                        updateWebHistoryBackCallback();
                    }

                    @Override
                    public void onPageFinished(
                            WebView view,
                            String url) {

                        super.onPageFinished(
                                view,
                                url);

                        updateWebHistoryBackCallback();
                    }

                    @Override
                    public void onReceivedError(
                            WebView view,
                            WebResourceRequest request,
                            WebResourceError error) {

                        super.onReceivedError(
                                view,
                                request,
                                error);

                        if (!request.isForMainFrame()) {
                            return;
                        }

                        showWebViewError(
                                getString(
                                        R.string.webview_load_error_title),
                                getString(
                                        R.string.webview_load_error_details,
                                        error.getErrorCode(),
                                        error.getDescription(),
                                        request.getUrl()));
                    }

                    @Override
                    public void onReceivedHttpError(
                            WebView view,
                            WebResourceRequest request,
                            WebResourceResponse response) {

                        super.onReceivedHttpError(
                                view,
                                request,
                                response);

                        if (!request.isForMainFrame()) {
                            return;
                        }

                        showWebViewError(
                                getString(
                                        R.string.webview_http_error_title),
                                getString(
                                        R.string.webview_http_error_details,
                                        response.getStatusCode(),
                                        response.getReasonPhrase(),
                                        request.getUrl()));
                    }

                    @Override
                    public void onReceivedSslError(
                            WebView view,
                            SslErrorHandler handler,
                            SslError error) {

                        handler.cancel();

                        showWebViewError(
                                getString(
                                        R.string.webview_ssl_error_title),
                                getString(
                                        R.string.webview_ssl_error_details,
                                        error.getPrimaryError(),
                                        error.getUrl()));
                    }

                    @Override
                    public boolean onRenderProcessGone(
                            WebView view,
                            RenderProcessGoneDetail detail) {

                        disposeWebView(
                                view);

                        showFatalWebViewError(
                                getString(
                                        R.string.webview_process_gone_title),
                                getString(
                                        detail.didCrash()
                                                ? R.string.webview_renderer_crashed
                                                : R.string.webview_renderer_terminated));

                        return true;
                    }
                });
    }

    private void showCustomFullscreenView(
            View view,
            WebChromeClient.CustomViewCallback callback) {

        if (view == null
                || callback == null) {

            return;
        }

        if (customFullscreenView != null) {

            try {
                callback.onCustomViewHidden();
            } catch (RuntimeException ignored) {
            }

            return;
        }

        View decorView =
                getWindow()
                        .getDecorView();

        if (!(decorView instanceof ViewGroup)) {
            return;
        }

        customFullscreenView =
                view;

        customFullscreenCallback =
                callback;

        ((ViewGroup) decorView)
                .addView(
                        view,
                        new ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT));

        if (webView != null) {
            webView.setVisibility(
                    View.INVISIBLE);
        }

        WindowInsetsController controller =
                getWindow()
                        .getInsetsController();

        if (controller != null) {

            controller.hide(
                    WindowInsets.Type.systemBars());

            controller.setSystemBarsBehavior(
                    WindowInsetsController
                            .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }

        updateWebHistoryBackCallback();
    }

    private void hideCustomFullscreenView() {

        View view =
                customFullscreenView;

        WebChromeClient.CustomViewCallback callback =
                customFullscreenCallback;

        customFullscreenView =
                null;

        customFullscreenCallback =
                null;

        if (view != null
                && view.getParent()
                instanceof ViewGroup) {

            try {
                ((ViewGroup) view.getParent())
                        .removeView(
                                view);

            } catch (RuntimeException ignored) {
            }
        }

        if (webView != null) {

            webView.setVisibility(
                    View.VISIBLE);
        }

        WindowInsetsController controller =
                getWindow()
                        .getInsetsController();

        if (controller != null) {

            controller.show(
                    WindowInsets.Type.systemBars());
        }

        if (callback != null) {

            try {
                callback.onCustomViewHidden();
            } catch (RuntimeException ignored) {
            }
        }

        updateWebHistoryBackCallback();
    }

    private static boolean isSupportedDownloadUri(
            Uri uri) {

        return isHttpsUri(
                uri);
    }

    private void handleDownload(
            String url,
            String userAgent,
            String contentDisposition,
            String mimeType,
            long contentLength) {

        if (url == null) {
            return;
        }

        Uri uri;

        try {
            uri = Uri.parse(url);

        } catch (RuntimeException exception) {

            showDownloadFailedToast();
            return;
        }

        if (!isSupportedDownloadUri(uri)) {
            return;
        }

        if (httpsDownloader == null) {

            showDownloadFailedToast();
            return;
        }

        String cookies = null;
        String referer = null;

        if (webView != null
                && webView.getUrl() != null) {

            String currentUrl =
                    webView.getUrl();

            Uri currentUri =
                    Uri.parse(currentUrl);

            if (WebAppHttpsDownloader.maySendCredentials(
                    webAppInitialOrigin,
                    currentUri,
                    uri)) {

                try {
                    cookies =
                            CookieManager.getInstance()
                                    .getCookie(url);

                    referer =
                            currentUri.buildUpon()
                                    .fragment(null)
                                    .build()
                                    .toString();

                } catch (RuntimeException ignored) {
                    cookies = null;
                    referer = null;
                }
            }
        }

        httpsDownloader.enqueue(
                uri,
                userAgent,
                contentDisposition,
                mimeType,
                cookies,
                referer);
    }

    private void showDownloadFailedToast() {

        Toast.makeText(
                this,
                R.string.webapp_download_failed,
                Toast.LENGTH_SHORT)
                .show();
    }

    private boolean handleFileChooser(
            ValueCallback<Uri[]> callback,
            WebChromeClient.FileChooserParams params) {

        if (callback == null) {
            return false;
        }

        completeFileChooser(
                null);

        filePathCallback =
                callback;

        pendingFileChooserParams =
                params;

        if (params == null) {
            completeFileChooser(
                    null);

            return true;
        }

        if (isImageCaptureRequest(
                params)) {

            if (checkSelfPermission(
                    Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                        new String[]{
                                Manifest.permission.CAMERA
                        },
                        REQUEST_FILE_CAMERA_PERMISSION);

                return true;
            }

            launchCameraCapture();
            return true;
        }

        launchFileChooser();
        return true;
    }

    private static boolean isImageCaptureRequest(
            WebChromeClient.FileChooserParams params) {

        if (params == null
                || !params.isCaptureEnabled()) {

            return false;
        }

        String[] acceptTypes =
                params.getAcceptTypes();

        if (acceptTypes == null
                || acceptTypes.length == 0) {

            return true;
        }

        boolean hasType =
                false;

        for (String type : acceptTypes) {

            if (type == null
                    || type.trim().isEmpty()) {

                continue;
            }

            hasType =
                    true;

            String normalized =
                    type.trim();

            if ("*/*".equals(
                    normalized)
                    || normalized.regionMatches(
                            true,
                            0,
                            "image/",
                            0,
                            6)) {

                return true;
            }
        }

        return !hasType;
    }

    private void launchFileChooser() {

        if (filePathCallback == null
                || pendingFileChooserParams == null) {

            return;
        }

        pendingCameraCaptureUri =
                null;

        pendingCameraCaptureFile =
                null;

        try {
            Intent intent =
                    createFileChooserIntent(
                            pendingFileChooserParams);

            startActivityForResult(
                    intent,
                    REQUEST_FILE_CHOOSER);

        } catch (RuntimeException exception) {

            completeFileChooser(
                    null);
        }
    }

    private static Intent createFileChooserIntent(
            WebChromeClient.FileChooserParams params) {

        Intent sourceIntent =
                params.createIntent();

        sourceIntent.addCategory(
                Intent.CATEGORY_OPENABLE);

        sourceIntent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION);

        CharSequence title =
                params.getTitle();

        return Intent.createChooser(
                sourceIntent,
                title);
    }

    private static Uri[] extractFileChooserResults(
            Intent data) {

        if (data == null) {
            return null;
        }

        ArrayList<Uri> results =
                new ArrayList<>();

        ClipData clipData =
                data.getClipData();

        if (clipData != null) {

            for (int index = 0;
                 index < clipData.getItemCount();
                 index++) {

                Uri uri =
                        clipData.getItemAt(
                                        index)
                                .getUri();

                if (uri != null
                        && !results.contains(
                                uri)) {

                    results.add(
                            uri);
                }
            }
        }

        Uri dataUri =
                data.getData();

        if (dataUri != null
                && !results.contains(
                        dataUri)) {

            results.add(
                    dataUri);
        }

        if (results.isEmpty()) {
            return null;
        }

        return results.toArray(
                new Uri[0]);
    }

    private void stabilizeFileChooserResultsAsync(
            Uri[] sourceUris) {

        if (sourceUris == null
                || sourceUris.length == 0) {

            completeFileChooser(
                    null);

            return;
        }

        try {
            uploadCopyExecutor.execute(
                    () -> {

                        Uri[] stableResults =
                                stabilizeFileChooserResults(
                                        sourceUris);

                        runOnUiThread(
                                () -> {

                                    if (isFinishing()
                                            || isDestroyed()) {

                                        return;
                                    }

                                    completeFileChooser(
                                            filterFileChooserResults(
                                                    stableResults));
                                });
                    });

        } catch (RuntimeException exception) {

            completeFileChooser(
                    null);
        }
    }

    private Uri[] stabilizeFileChooserResults(
            Uri[] sourceUris) {

        if (sourceUris == null
                || sourceUris.length == 0) {

            return null;
        }

        File uploadRoot =
                new File(
                        getCacheDir(),
                        "webapp-upload");

        if (!uploadRoot.exists()
                && !uploadRoot.mkdirs()) {

            return null;
        }

        File selectionDirectory =
                new File(
                        uploadRoot,
                        "selection-"
                                + System.nanoTime());

        if (!selectionDirectory.mkdirs()) {
            return null;
        }

        managedTempArtifacts.add(
                selectionDirectory);

        ArrayList<Uri> stableUris =
                new ArrayList<>();

        try {
            for (int index = 0;
                 index < sourceUris.length;
                 index++) {

                if (Thread.currentThread()
                        .isInterrupted()) {

                    throw new IOException(
                            "Upload copy cancelled.");
                }

                Uri sourceUri =
                        sourceUris[index];

                if (sourceUri == null
                        || !"content".equalsIgnoreCase(
                                sourceUri.getScheme())) {

                    throw new IOException(
                            "Unsupported upload URI.");
                }

                String fileName =
                        resolveUploadFileName(
                                sourceUri,
                                index);

                File target =
                        new File(
                                selectionDirectory,
                                fileName);

                try (InputStream input =
                             getContentResolver()
                                     .openInputStream(
                                             sourceUri);
                     FileOutputStream output =
                             new FileOutputStream(
                                     target)) {

                    if (input == null) {
                        throw new IOException(
                                "Could not open selected upload.");
                    }

                    byte[] buffer =
                            new byte[32768];

                    int count;

                    while ((count =
                            input.read(
                                    buffer))
                            != -1) {

                        if (Thread.currentThread()
                                .isInterrupted()) {

                            throw new IOException(
                                    "Upload copy cancelled.");
                        }

                        output.write(
                                buffer,
                                0,
                                count);
                    }

                    output.flush();
                }

                Uri stableUri =
                        FileProvider.getUriForFile(
                                this,
                                getPackageName()
                                        + ".fileprovider",
                                target);

                stableUris.add(
                        stableUri);
            }

        } catch (IOException
                 | RuntimeException exception) {

            managedTempArtifacts.remove(
                    selectionDirectory);

            deleteRecursively(
                    selectionDirectory);

            return null;
        }

        return stableUris.toArray(
                new Uri[0]);
    }

    private String resolveUploadFileName(
            Uri uri,
            int index) {

        String displayName =
                null;

        try (Cursor cursor =
                     getContentResolver()
                             .query(
                                     uri,
                                     new String[]{
                                             OpenableColumns.DISPLAY_NAME
                                     },
                                     null,
                                     null,
                                     null)) {

            if (cursor != null
                    && cursor.moveToFirst()) {

                int column =
                        cursor.getColumnIndex(
                                OpenableColumns.DISPLAY_NAME);

                if (column >= 0) {
                    displayName =
                            cursor.getString(
                                    column);
                }
            }

        } catch (RuntimeException ignored) {
        }

        if (displayName == null
                || displayName.trim().isEmpty()) {

            String extension =
                    null;

            try {
                String mimeType =
                        getContentResolver()
                                .getType(
                                        uri);

                if (mimeType != null) {
                    extension =
                            MimeTypeMap.getSingleton()
                                    .getExtensionFromMimeType(
                                            mimeType);
                }

            } catch (RuntimeException ignored) {
            }

            displayName =
                    "upload-"
                            + index
                            + (extension == null
                            ? ""
                            : "." + extension);
        }

        displayName =
                displayName
                        .replace(
                                "/",
                                "_")
                        .replace(
                                "\\",
                                "_");

        return index
                + "-"
                + displayName;
    }

    private void cleanupStaleWebAppTempFilesAsync() {

        try {
            uploadCopyExecutor.execute(
                    () -> {

                        long cutoff =
                                System.currentTimeMillis()
                                        - TEMP_FILE_MAX_AGE_MS;

                        deleteStaleChildren(
                                new File(
                                        getCacheDir(),
                                        "webapp-upload"),
                                cutoff);

                        deleteStaleChildren(
                                new File(
                                        getCacheDir(),
                                        "webapp-camera"),
                                cutoff);
                    });

        } catch (RuntimeException ignored) {
        }
    }

    private static void deleteStaleChildren(
            File root,
            long cutoff) {

        if (root == null
                || !root.isDirectory()) {

            return;
        }

        File[] children =
                root.listFiles();

        if (children == null) {
            return;
        }

        for (File child : children) {

            if (Thread.currentThread()
                    .isInterrupted()) {

                return;
            }

            long modified =
                    child.lastModified();

            if (modified > 0L
                    && modified < cutoff) {

                deleteRecursively(
                        child);
            }
        }
    }

    private void cleanupManagedTempArtifacts() {

        ArrayList<File> artifacts;

        synchronized (managedTempArtifacts) {

            artifacts =
                    new ArrayList<>(
                            managedTempArtifacts);

            managedTempArtifacts.clear();
        }

        for (File artifact : artifacts) {
            deleteRecursively(
                    artifact);
        }
    }

    private static void deleteRecursively(
            File file) {

        if (file == null
                || !file.exists()) {

            return;
        }

        if (file.isDirectory()) {

            File[] children =
                    file.listFiles();

            if (children != null) {

                for (File child : children) {
                    deleteRecursively(
                            child);
                }
            }
        }

        try {
            file.delete();
        } catch (RuntimeException ignored) {
        }
    }

    private void launchCameraCapture() {

        if (filePathCallback == null) {
            return;
        }

        try {
            File cameraDirectory =
                    new File(
                            getCacheDir(),
                            "webapp-camera");

            if (!cameraDirectory.exists()
                    && !cameraDirectory.mkdirs()) {

                throw new IOException(
                        "Could not create camera cache directory.");
            }

            pendingCameraCaptureFile =
                    File.createTempFile(
                            "capture-",
                            ".jpg",
                            cameraDirectory);

            managedTempArtifacts.add(
                    pendingCameraCaptureFile);

            pendingCameraCaptureUri =
                    FileProvider.getUriForFile(
                            this,
                            getPackageName()
                                    + ".fileprovider",
                            pendingCameraCaptureFile);

            Intent cameraIntent =
                    new Intent(
                            MediaStore.ACTION_IMAGE_CAPTURE);

            cameraIntent.putExtra(
                    MediaStore.EXTRA_OUTPUT,
                    pendingCameraCaptureUri);

            cameraIntent.setClipData(
                    ClipData.newRawUri(
                            "webapp-camera",
                            pendingCameraCaptureUri));

            cameraIntent.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                            | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            startActivityForResult(
                    cameraIntent,
                    REQUEST_FILE_CHOOSER);

        } catch (IOException
                 | RuntimeException exception) {

            completeFileChooser(
                    null);
        }
    }

    private void completeFileChooser(
            Uri[] results) {

        ValueCallback<Uri[]> callback =
                filePathCallback;

        File cameraFile =
                pendingCameraCaptureFile;

        filePathCallback =
                null;

        pendingFileChooserParams =
                null;

        pendingCameraCaptureUri =
                null;

        pendingCameraCaptureFile =
                null;

        if ((results == null
                || results.length == 0)
                && cameraFile != null) {

            managedTempArtifacts.remove(
                    cameraFile);

            try {
                cameraFile.delete();
            } catch (RuntimeException ignored) {
            }
        }

        if (callback != null) {
            callback.onReceiveValue(
                    results);
        }
    }

    private static Uri[] filterFileChooserResults(
            Uri[] results) {

        if (results == null
                || results.length == 0) {

            return null;
        }

        ArrayList<Uri> allowed =
                new ArrayList<>();

        for (Uri uri : results) {

            if (uri == null) {
                continue;
            }

            if ("content".equalsIgnoreCase(
                    uri.getScheme())) {

                allowed.add(
                        uri);
            }
        }

        if (allowed.isEmpty()) {
            return null;
        }

        return allowed.toArray(
                new Uri[0]);
    }

    private void handleGeolocationPermissionRequest(
            String origin,
            GeolocationPermissions.Callback callback) {

        if (callback == null
                || !isSecureCurrentWebOrigin(
                        origin)) {

            if (callback != null
                    && origin != null) {

                try {
                    callback.invoke(
                            origin,
                            false,
                            false);

                } catch (RuntimeException ignored) {
                }
            }

            return;
        }

        boolean locationGranted =
                checkSelfPermission(
                        Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED
                        || checkSelfPermission(
                        Manifest.permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;

        if (locationGranted) {

            callback.invoke(
                    origin,
                    true,
                    false);

            return;
        }

        denyPendingGeolocationRequest();

        pendingGeolocationOrigin =
                origin;

        pendingGeolocationCallback =
                callback;

        requestPermissions(
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                },
                REQUEST_WEB_GEOLOCATION_PERMISSION);
    }

    private boolean isSecureCurrentWebOrigin(
            String origin) {

        if (origin == null
                || webView == null) {

            return false;
        }

        Uri originUri =
                Uri.parse(
                        origin);

        if (!isHttpsUri(
                originUri)) {

            return false;
        }

        String currentUrl =
                webView.getUrl();

        if (currentUrl == null) {
            return false;
        }

        return isPermittedWebOrigin(
                webAppInitialOrigin,
                Uri.parse(currentUrl),
                originUri);
    }

    private void denyPendingGeolocationRequest() {

        String origin =
                pendingGeolocationOrigin;

        GeolocationPermissions.Callback callback =
                pendingGeolocationCallback;

        pendingGeolocationOrigin =
                null;

        pendingGeolocationCallback =
                null;

        if (origin == null
                || callback == null) {

            return;
        }

        try {
            callback.invoke(
                    origin,
                    false,
                    false);

        } catch (RuntimeException ignored) {
        }
    }

    private static boolean requestsAudioCapture(
            PermissionRequest request) {

        if (request == null
                || request.getResources() == null) {

            return false;
        }

        for (String resource :
                request.getResources()) {

            if (PermissionRequest.RESOURCE_AUDIO_CAPTURE
                    .equals(resource)) {

                return true;
            }
        }

        return false;
    }

    private boolean isSecureMediaPermissionRequest(
            PermissionRequest request) {

        if (request == null
                || webView == null
                || !isHttpsUri(
                        request.getOrigin())) {

            return false;
        }

        String currentUrl =
                webView.getUrl();

        return currentUrl != null
                && isPermittedWebOrigin(
                        webAppInitialOrigin,
                        Uri.parse(currentUrl),
                        request.getOrigin());
    }

    private boolean grantWebMediaRequestIfAllowed(
            PermissionRequest request) {

        if (!isSecureMediaPermissionRequest(
                request)) {

            return false;
        }

        String[] resources =
                request.getResources();

        if (resources == null
                || resources.length == 0) {

            return false;
        }

        ArrayList<String> allowedResources =
                new ArrayList<>();

        for (String resource : resources) {

            if (PermissionRequest.RESOURCE_AUDIO_CAPTURE
                    .equals(resource)) {

                if (checkSelfPermission(
                        Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {

                    return false;
                }

                if (!allowedResources.contains(
                        resource)) {

                    allowedResources.add(
                            resource);
                }

            } else if (PermissionRequest.RESOURCE_VIDEO_CAPTURE
                    .equals(resource)) {

                if (checkSelfPermission(
                        Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {

                    return false;
                }

                if (!allowedResources.contains(
                        resource)) {

                    allowedResources.add(
                            resource);
                }

            } else {

                return false;
            }
        }

        if (allowedResources.isEmpty()) {
            return false;
        }

        try {
            request.grant(
                    allowedResources.toArray(
                            new String[0]));

            return true;

        } catch (RuntimeException ignored) {

            return false;
        }
    }

    private void handleWebMediaPermissionRequest(
            PermissionRequest request) {

        if (!isSecureMediaPermissionRequest(
                request)) {

            request.deny();
            return;
        }

        String[] resources =
                request.getResources();

        if (resources == null
                || resources.length == 0) {

            request.deny();
            return;
        }

        ArrayList<String> missingPermissions =
                new ArrayList<>();

        for (String resource : resources) {

            if (PermissionRequest.RESOURCE_AUDIO_CAPTURE
                    .equals(resource)) {

                if (checkSelfPermission(
                        Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED
                        && !missingPermissions.contains(
                        Manifest.permission.RECORD_AUDIO)) {

                    missingPermissions.add(
                            Manifest.permission.RECORD_AUDIO);
                }

            } else if (PermissionRequest.RESOURCE_VIDEO_CAPTURE
                    .equals(resource)) {

                if (checkSelfPermission(
                        Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED
                        && !missingPermissions.contains(
                        Manifest.permission.CAMERA)) {

                    missingPermissions.add(
                            Manifest.permission.CAMERA);
                }

            } else {

                request.deny();
                return;
            }
        }

        if (missingPermissions.isEmpty()) {

            if (!grantWebMediaRequestIfAllowed(
                    request)) {

                request.deny();
            }

            return;
        }

        denyPendingWebPermissionRequest();

        pendingWebPermissionRequest =
                request;

        requestPermissions(
                missingPermissions.toArray(
                        new String[0]),
                REQUEST_WEB_MEDIA_PERMISSION);
    }

    private void handleWebPermissionRequest(
            PermissionRequest request) {

        if (requestsAudioCapture(
                request)) {

            handleWebMediaPermissionRequest(
                    request);

            return;
        }

        if (!isSecureVideoPermissionRequest(
                request)) {

            request.deny();
            return;
        }

        if (checkSelfPermission(
                Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {

            request.grant(
                    new String[]{
                            PermissionRequest.RESOURCE_VIDEO_CAPTURE
                    });

            return;
        }

        denyPendingWebPermissionRequest();

        pendingWebPermissionRequest =
                request;

        requestPermissions(
                new String[]{
                        Manifest.permission.CAMERA
                },
                REQUEST_WEB_CAMERA_PERMISSION);
    }

    private boolean isSecureVideoPermissionRequest(
            PermissionRequest request) {

        if (request == null
                || webView == null
                || !isHttpsUri(
                        request.getOrigin())) {

            return false;
        }

        String currentUrl =
                webView.getUrl();

        if (currentUrl == null
                || !isPermittedWebOrigin(
                        webAppInitialOrigin,
                        Uri.parse(currentUrl),
                        request.getOrigin())) {

            return false;
        }

        String[] resources =
                request.getResources();

        if (resources == null) {
            return false;
        }

        for (String resource : resources) {

            if (PermissionRequest.RESOURCE_VIDEO_CAPTURE
                    .equals(resource)) {

                return true;
            }
        }

        return false;
    }

    private static boolean isPermittedWebOrigin(
            Uri configuredOrigin,
            Uri currentOrigin,
            Uri requestedOrigin) {

        if (configuredOrigin == null
                || currentOrigin == null
                || requestedOrigin == null) {

            return false;
        }

        return isSameHttpsOrigin(
                configuredOrigin,
                requestedOrigin)
                && isSameHttpsOrigin(
                currentOrigin,
                requestedOrigin);
    }

    private static boolean isSameHttpsOrigin(
            Uri first,
            Uri second) {

        if (!isHttpsUri(first)
                || !isHttpsUri(second)) {

            return false;
        }

        if (!first.getHost()
                .equalsIgnoreCase(
                        second.getHost())) {

            return false;
        }

        return normalizedHttpsPort(first)
                == normalizedHttpsPort(second);
    }

    private static int normalizedHttpsPort(
            Uri uri) {

        int port =
                uri.getPort();

        return port == -1
                ? 443
                : port;
    }

    private void denyPendingWebPermissionRequest() {

        PermissionRequest request =
                pendingWebPermissionRequest;

        pendingWebPermissionRequest =
                null;

        if (request == null) {
            return;
        }

        try {
            request.deny();
        } catch (RuntimeException ignored) {
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        if (requestCode
                == REQUEST_FILE_CHOOSER) {

            Uri[] results =
                    null;

            if (resultCode
                    == RESULT_OK) {

                if (pendingCameraCaptureUri
                        != null) {

                    results =
                            new Uri[]{
                                    pendingCameraCaptureUri
                            };

                } else {
                    Uri[] sourceResults =
                            extractFileChooserResults(
                                    data);

                    stabilizeFileChooserResultsAsync(
                            sourceResults);

                    return;
                }
            }

            completeFileChooser(
                    filterFileChooserResults(
                            results));

            return;
        }

        super.onActivityResult(
                requestCode,
                resultCode,
                data);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {

        boolean granted =
                grantResults.length > 0
                        && grantResults[0]
                        == PackageManager.PERMISSION_GRANTED;

        if (requestCode
                == REQUEST_FILE_CAMERA_PERMISSION) {

            if (granted
                    && filePathCallback != null
                    && isImageCaptureRequest(
                            pendingFileChooserParams)) {

                launchCameraCapture();

            } else {
                completeFileChooser(
                        null);
            }

            return;
        }

        if (requestCode
                == REQUEST_WEB_CAMERA_PERMISSION) {

            PermissionRequest request =
                    pendingWebPermissionRequest;

            pendingWebPermissionRequest =
                    null;

            if (request == null) {
                return;
            }

            if (granted
                    && isSecureVideoPermissionRequest(
                            request)) {

                request.grant(
                        new String[]{
                                PermissionRequest.RESOURCE_VIDEO_CAPTURE
                        });

            } else {
                request.deny();
            }

            return;
        }

        if (requestCode
                == REQUEST_WEB_MEDIA_PERMISSION) {

            PermissionRequest request =
                    pendingWebPermissionRequest;

            pendingWebPermissionRequest =
                    null;

            if (request == null) {
                return;
            }

            if (!grantWebMediaRequestIfAllowed(
                    request)) {

                request.deny();
            }

            return;
        }

        if (requestCode
                == REQUEST_WEB_GEOLOCATION_PERMISSION) {

            String origin =
                    pendingGeolocationOrigin;

            GeolocationPermissions.Callback callback =
                    pendingGeolocationCallback;

            pendingGeolocationOrigin =
                    null;

            pendingGeolocationCallback =
                    null;

            if (origin == null
                    || callback == null) {

                return;
            }

            boolean locationGranted =
                    checkSelfPermission(
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED
                            || checkSelfPermission(
                            Manifest.permission.ACCESS_COARSE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED;

            boolean allow =
                    locationGranted
                            && isSecureCurrentWebOrigin(
                                    origin);

            try {
                callback.invoke(
                        origin,
                        allow,
                        false);

            } catch (RuntimeException ignored) {
            }

            return;
        }

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults);
    }

    private static boolean isHttpsUri(
            Uri uri) {

        return "https".equalsIgnoreCase(
                uri.getScheme())
                && uri.getHost() != null
                && !uri.getHost().isEmpty();
    }

    private boolean handleUri(
            Uri uri) {

        String scheme =
                uri.getScheme();

        if (isHttpsUri(uri)) {
            return false;
        }

        try {
            Intent intent;

            if ("intent".equalsIgnoreCase(
                    scheme)) {

                intent =
                        Intent.parseUri(
                                uri.toString(),
                                Intent.URI_INTENT_SCHEME);

            } else {
                intent =
                        new Intent(
                                Intent.ACTION_VIEW,
                                uri);
            }

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK);

            startActivity(
                    intent);

        } catch (ActivityNotFoundException exception) {

            Toast.makeText(
                    this,
                    R.string.shortcut_launch_failed,
                    Toast.LENGTH_SHORT)
                    .show();

        } catch (Exception exception) {

            Toast.makeText(
                    this,
                    R.string.shortcut_launch_failed,
                    Toast.LENGTH_SHORT)
                    .show();
        }

        return true;
    }

    private void showWebViewError(
            String title,
            String details) {

        showWebViewErrorDialog(
                title,
                details,
                false);
    }

    private void showFatalWebViewError(
            String title,
            String details) {

        showWebViewErrorDialog(
                title,
                details,
                true);
    }

    private void showWebViewErrorDialog(
            String title,
            String details,
            boolean fatal) {

        if (isFinishing()
                || isDestroyed()) {

            return;
        }

        dismissErrorDialog();

        String message = details;

        try {
            message +=
                    "\n\n"
                            + getWebViewInformation();
        } catch (RuntimeException ignored) {
        }

        AlertDialog.Builder builder =
                new AlertDialog.Builder(this)
                        .setTitle(title)
                        .setMessage(message);

        if (fatal) {
            builder.setPositiveButton(
                            android.R.string.ok,
                            (dialog, which) ->
                                    finish())
                    .setOnCancelListener(
                            dialog ->
                                    finish());
        } else {
            builder.setPositiveButton(
                    android.R.string.ok,
                    null);
        }

        AlertDialog dialog =
                builder.create();

        errorDialog = dialog;

        dialog.setOnDismissListener(ignored -> {
            if (errorDialog == dialog) {
                errorDialog = null;
            }
        });

        dialog.show();
    }

    private void dismissErrorDialog() {
        AlertDialog dialog =
                errorDialog;

        errorDialog = null;

        if (dialog == null) {
            return;
        }

        try {
            dialog.dismiss();
        } catch (RuntimeException ignored) {
        }
    }

    private String getWebViewInformation() {

        StringBuilder result =
                new StringBuilder();

        result.append(
                getString(
                        R.string.webview_android_info,
                        Build.VERSION.RELEASE,
                        Build.VERSION.SDK_INT));

        if (Build.VERSION.SDK_INT
                >= Build.VERSION_CODES.O) {

            PackageInfo packageInfo =
                    WebView.getCurrentWebViewPackage();

            if (packageInfo != null) {
                result.append("\n\n")
                        .append(
                                getString(
                                        R.string.webview_package_info,
                                        packageInfo.packageName,
                                        packageInfo.versionName));
            } else {
                result.append("\n\n")
                        .append(
                                getString(
                                        R.string.webview_package_missing));
            }
        }

        return result.toString();
    }

    @Override
    protected void onSaveInstanceState(
            Bundle outState) {

        if (webView != null) {
            try {
                webView.saveState(
                        outState);
            } catch (RuntimeException ignored) {
            }
        }

        super.onSaveInstanceState(
                outState);
    }

    @Override
    protected void onPause() {

        if (webView != null) {
            try {
                CookieManager.getInstance()
                        .flush();

            } catch (RuntimeException ignored) {
            }
        }

        super.onPause();
    }

    private void disposeWebView(
            WebView target) {

        if (target == null) {
            return;
        }

        if (webView == target) {
            webView = null;
        }

        try {
            target.stopLoading();
        } catch (RuntimeException ignored) {
        }

        try {
            target.setWebChromeClient(null);
            target.setWebViewClient(null);
        } catch (RuntimeException ignored) {
        }

        try {
            if (target.getParent()
                    instanceof ViewGroup) {

                ((ViewGroup) target.getParent())
                        .removeView(target);
            }
        } catch (RuntimeException ignored) {
        }

        try {
            target.destroy();
        } catch (RuntimeException ignored) {
        }
    }

    @Override
    protected void onDestroy() {

        dismissErrorDialog();

        hideCustomFullscreenView();

        uploadCopyExecutor.shutdownNow();

        completeFileChooser(
                null);

        denyPendingWebPermissionRequest();

        denyPendingGeolocationRequest();

        WebAppBlobDownloadBridge bridge =
                blobDownloadBridge;

        blobDownloadBridge = null;

        if (bridge != null) {
            bridge.close();
        }

        WebAppHttpsDownloader downloader =
                httpsDownloader;

        httpsDownloader = null;

        if (downloader != null) {
            downloader.close();
        }

        if (webHistoryBackCallbackRegistered) {
            getOnBackInvokedDispatcher()
                    .unregisterOnBackInvokedCallback(
                            webHistoryBackCallback);

            webHistoryBackCallbackRegistered = false;
        }

        disposeWebView(
                webView);

        cleanupManagedTempArtifacts();

        super.onDestroy();
    }
}
