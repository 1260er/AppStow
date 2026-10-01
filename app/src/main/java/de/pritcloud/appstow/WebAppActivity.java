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
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;
import android.webkit.CookieManager;
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

@SuppressLint({"SetJavaScriptEnabled", "WebViewApiAvailability", "ObsoleteSdkInt"})
public class WebAppActivity extends Activity {

    static final String EXTRA_URL =
            "web_app_url";

    private static final int REQUEST_FILE_CHOOSER = 1001;
    private static final int REQUEST_FILE_CAMERA_PERMISSION = 1002;
    private static final int REQUEST_WEB_CAMERA_PERMISSION = 1003;

    private WebView webView;
    private AlertDialog errorDialog;

    private ValueCallback<Uri[]> filePathCallback;
    private WebChromeClient.FileChooserParams pendingFileChooserParams;
    private Uri pendingCameraCaptureUri;
    private File pendingCameraCaptureFile;
    private PermissionRequest pendingWebPermissionRequest;

    private final OnBackInvokedCallback webHistoryBackCallback =
            () -> {
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

        try {
            configureWebView();

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
                webView != null
                        && webView.canGoBack();

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

    private void configureWebView() {

        WebSettings settings =
                webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);

        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setMixedContentMode(
                WebSettings.MIXED_CONTENT_NEVER_ALLOW);

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
                });

        webView.setWebViewClient(
                new WebViewClient() {

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

                        try {
                            CookieManager.getInstance()
                                    .flush();
                        } catch (RuntimeException ignored) {
                        }
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

        String action =
                params.getMode()
                        == WebChromeClient.FileChooserParams.MODE_SAVE
                        ? Intent.ACTION_CREATE_DOCUMENT
                        : Intent.ACTION_OPEN_DOCUMENT;

        Intent intent =
                new Intent(
                        action);

        intent.addCategory(
                Intent.CATEGORY_OPENABLE);

        intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION);

        /*
         * Web apps can report overly restrictive accept types in WebView.
         * Keep capture handling separate, but let the normal document picker
         * expose all openable files. The web app remains responsible for
         * validating whether the selected file type is supported.
         */
        intent.setType(
                "*/*");

        if (params.getMode()
                == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE) {

            intent.putExtra(
                    Intent.EXTRA_ALLOW_MULTIPLE,
                    true);
        }

        if (Intent.ACTION_CREATE_DOCUMENT.equals(
                action)) {

            String filenameHint =
                    params.getFilenameHint();

            if (filenameHint != null
                    && !filenameHint.trim().isEmpty()) {

                intent.putExtra(
                        Intent.EXTRA_TITLE,
                        filenameHint);
            }
        }

        return intent;
    }

    private static String[] normalizeAcceptTypes(
            String[] acceptTypes) {

        ArrayList<String> mimeTypes =
                new ArrayList<>();

        if (acceptTypes == null) {
            return new String[0];
        }

        for (String rawType : acceptTypes) {

            if (rawType == null) {
                continue;
            }

            String[] parts =
                    rawType.split(",");

            for (String part : parts) {

                String value =
                        part.trim();

                if (value.isEmpty()) {
                    continue;
                }

                if ("*/*".equals(
                        value)) {

                    return new String[]{
                            "*/*"
                    };
                }

                String mimeType =
                        null;

                if (value.startsWith(".")) {

                    String extension =
                            value.substring(1);

                    mimeType =
                            MimeTypeMap.getSingleton()
                                    .getMimeTypeFromExtension(
                                            extension);

                } else if (value.contains("/")) {

                    mimeType =
                            value;
                }

                if (mimeType != null
                        && !mimeTypes.contains(
                                mimeType)) {

                    mimeTypes.add(
                            mimeType);
                }
            }
        }

        return mimeTypes.toArray(
                new String[0]);
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

    private void handleWebPermissionRequest(
            PermissionRequest request) {

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
                || !isSameHttpsOrigin(
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
                    results =
                            extractFileChooserResults(
                                    data);
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

        if (webHistoryBackCallbackRegistered) {
            getOnBackInvokedDispatcher()
                    .unregisterOnBackInvokedCallback(
                            webHistoryBackCallback);

            webHistoryBackCallbackRegistered = false;
        }

        dismissErrorDialog();

        completeFileChooser(
                null);

        denyPendingWebPermissionRequest();

        disposeWebView(
                webView);

        super.onDestroy();
    }
}
