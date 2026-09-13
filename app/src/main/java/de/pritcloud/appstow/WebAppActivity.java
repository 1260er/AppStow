package de.pritcloud.appstow;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class WebAppActivity extends Activity {

    static final String EXTRA_URL =
            "web_app_url";

    private WebView webView;

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

            webView.loadUrl(
                    url);

        } catch (RuntimeException exception) {

            showFatalWebViewError(
                    getString(
                            R.string.webapp_load_failed_title),
                    exception.toString());
        }
    }

    private void configureWebView() {

        WebSettings settings =
                webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);

        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(
                WebSettings.MIXED_CONTENT_NEVER_ALLOW);

        CookieManager cookieManager =
                CookieManager.getInstance();

        cookieManager.setAcceptCookie(true);

        cookieManager.setAcceptThirdPartyCookies(
                webView,
                true);

        webView.setWebChromeClient(
                new WebChromeClient());

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
                    public boolean shouldOverrideUrlLoading(
                            WebView view,
                            String url) {

                        return handleUri(
                                Uri.parse(url));
                    }

                    @Override
                    public void onPageFinished(
                            WebView view,
                            String url) {

                        super.onPageFinished(
                                view,
                                url);

                        CookieManager.getInstance()
                                .flush();
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

        if (isFinishing()
                || isDestroyed()) {

            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(
                        details
                                + "\n\n"
                                + getWebViewInformation())
                .setPositiveButton(
                        android.R.string.ok,
                        null)
                .show();
    }

    private void showFatalWebViewError(
            String title,
            String details) {

        if (isFinishing()
                || isDestroyed()) {

            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(
                        details
                                + "\n\n"
                                + getWebViewInformation())
                .setPositiveButton(
                        android.R.string.ok,
                        (dialog, which) ->
                                finish())
                .setOnCancelListener(
                        dialog ->
                                finish())
                .show();
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

    @Override
    public void onBackPressed() {

        if (webView != null
                && webView.canGoBack()) {

            webView.goBack();
            return;
        }

        super.onBackPressed();
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

        disposeWebView(
                webView);

        super.onDestroy();
    }
}
