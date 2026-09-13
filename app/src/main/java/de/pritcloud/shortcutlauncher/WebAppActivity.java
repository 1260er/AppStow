package de.pritcloud.shortcutlauncher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
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
                    "WebView konnte nicht gestartet werden",
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

        try {
            configureWebView();

            webView.loadUrl(
                    url);

        } catch (RuntimeException exception) {

            showFatalWebViewError(
                    "Web-App konnte nicht geladen werden",
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
                                "WebView-Ladefehler",
                                "Fehlercode: "
                                        + error.getErrorCode()
                                        + "\n\nBeschreibung:\n"
                                        + error.getDescription()
                                        + "\n\nURL:\n"
                                        + request.getUrl());
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
                                "HTTP-Fehler",
                                "HTTP "
                                        + response.getStatusCode()
                                        + " "
                                        + response.getReasonPhrase()
                                        + "\n\nURL:\n"
                                        + request.getUrl());
                    }

                    @Override
                    public void onReceivedSslError(
                            WebView view,
                            SslErrorHandler handler,
                            SslError error) {

                        handler.cancel();

                        showWebViewError(
                                "SSL-Fehler",
                                "SSL-Fehlercode: "
                                        + error.getPrimaryError()
                                        + "\n\nURL:\n"
                                        + error.getUrl());
                    }

                    @Override
                    public boolean onRenderProcessGone(
                            WebView view,
                            RenderProcessGoneDetail detail) {

                        showWebViewError(
                                "WebView-Prozess beendet",
                                detail.didCrash()
                                        ? "Der WebView-Renderer ist abgestürzt."
                                        : "Der WebView-Renderer wurde vom System beendet.");

                        return true;
                    }
                });
    }

    private boolean handleUri(
            Uri uri) {

        String scheme =
                uri.getScheme();

        if ("http".equalsIgnoreCase(scheme)
                || "https".equalsIgnoreCase(scheme)) {

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

        result.append("Android ")
                .append(Build.VERSION.RELEASE)
                .append(" (API ")
                .append(Build.VERSION.SDK_INT)
                .append(")");

        if (Build.VERSION.SDK_INT
                >= Build.VERSION_CODES.O) {

            PackageInfo packageInfo =
                    WebView.getCurrentWebViewPackage();

            if (packageInfo != null) {
                result.append("\n\nWebView-Paket:\n")
                        .append(packageInfo.packageName)
                        .append("\nVersion:\n")
                        .append(packageInfo.versionName);
            } else {
                result.append(
                        "\n\nWebView-Paket:\nKeines gefunden");
            }
        }

        return result.toString();
    }

    @Override
    protected void onPause() {

        CookieManager.getInstance()
                .flush();

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

    @Override
    protected void onDestroy() {

        if (webView != null) {
            webView.stopLoading();
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            webView.destroy();
            webView = null;
        }

        super.onDestroy();
    }
}
