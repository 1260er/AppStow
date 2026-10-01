package de.pritcloud.appstow;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class WebAppActivityTest {

    @Test
    public void manifestDeclaresCameraPermission() throws Exception {

        Context context =
                ApplicationProvider
                        .getApplicationContext();

        PackageInfo packageInfo =
                context.getPackageManager()
                        .getPackageInfo(
                                context.getPackageName(),
                                PackageManager.GET_PERMISSIONS);

        assertNotNull(
                packageInfo.requestedPermissions);

        assertTrue(
                Arrays.asList(
                                packageInfo.requestedPermissions)
                        .contains(
                                Manifest.permission.CAMERA));
    }

    @Test
    public void webCameraOriginMustMatchCurrentPage() throws Exception {

        Method method =
                WebAppActivity.class
                        .getDeclaredMethod(
                                "isSameHttpsOrigin",
                                Uri.class,
                                Uri.class);

        method.setAccessible(
                true);

        assertTrue(
                (Boolean) method.invoke(
                        null,
                        Uri.parse(
                                "https://chatgpt.com/"),
                        Uri.parse(
                                "https://chatgpt.com")));

        assertTrue(
                (Boolean) method.invoke(
                        null,
                        Uri.parse(
                                "https://chatgpt.com:443/c/123"),
                        Uri.parse(
                                "https://chatgpt.com")));

        assertFalse(
                (Boolean) method.invoke(
                        null,
                        Uri.parse(
                                "https://chatgpt.com/"),
                        Uri.parse(
                                "https://example.com")));

        assertFalse(
                (Boolean) method.invoke(
                        null,
                        Uri.parse(
                                "https://chatgpt.com/"),
                        Uri.parse(
                                "http://chatgpt.com")));
    }


    @Test
    public void webViewAllowsSelectedContentButKeepsFileAccessDisabled() {

        Context context =
                ApplicationProvider
                        .getApplicationContext();

        Intent intent =
                new Intent(
                        context,
                        WebAppActivity.class);

        intent.putExtra(
                WebAppActivity.EXTRA_URL,
                "https://example.com");

        try (ActivityScenario<WebAppActivity> scenario =
                     ActivityScenario.launch(
                             intent)) {

            scenario.onActivity(
                    activity -> {

                        WebView webView =
                                activity.findViewById(
                                        R.id.webAppView);

                        assertNotNull(
                                webView);

                        assertTrue(
                                webView.getSettings()
                                        .getAllowContentAccess());

                        assertFalse(
                                webView.getSettings()
                                        .getAllowFileAccess());
                    });
        }
    }

    @Test
    public void imageRequestUsesPhotoPicker()
            throws Exception {

        WebChromeClient.FileChooserParams params =
                new TestFileChooserParams(
                        new String[]{
                                "image/*"
                        },
                        WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE);

        Method method =
                WebAppActivity.class
                        .getDeclaredMethod(
                                "createFileChooserIntent",
                                WebChromeClient.FileChooserParams.class);

        method.setAccessible(
                true);

        Intent intent =
                (Intent) method.invoke(
                        null,
                        params);

        assertEquals(
                MediaStore.ACTION_PICK_IMAGES,
                intent.getAction());

        assertEquals(
                "image/*",
                intent.getType());

        assertTrue(
                intent.getIntExtra(
                        MediaStore.EXTRA_PICK_IMAGES_MAX,
                        0)
                        > 1);
    }

    @Test
    public void documentRequestUsesDocumentPickerAndFilters()
            throws Exception {

        WebChromeClient.FileChooserParams params =
                new TestFileChooserParams(
                        new String[]{
                                "application/pdf",
                                "text/plain"
                        },
                        WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE);

        Method method =
                WebAppActivity.class
                        .getDeclaredMethod(
                                "createFileChooserIntent",
                                WebChromeClient.FileChooserParams.class);

        method.setAccessible(
                true);

        Intent intent =
                (Intent) method.invoke(
                        null,
                        params);

        assertEquals(
                Intent.ACTION_OPEN_DOCUMENT,
                intent.getAction());

        assertEquals(
                "application/pdf",
                intent.getType());

        assertArrayEquals(
                new String[]{
                        "application/pdf",
                        "text/plain"
                },
                intent.getStringArrayExtra(
                        Intent.EXTRA_MIME_TYPES));

        assertTrue(
                intent.getBooleanExtra(
                        Intent.EXTRA_ALLOW_MULTIPLE,
                        false));

        assertTrue(
                intent.getCategories()
                        .contains(
                                Intent.CATEGORY_OPENABLE));
    }

    @Test
    public void webChromeClientHandlesFilesAndCameraPermissions()
            throws Exception {

        Context context =
                ApplicationProvider
                        .getApplicationContext();

        Intent intent =
                new Intent(
                        context,
                        WebAppActivity.class);

        intent.putExtra(
                WebAppActivity.EXTRA_URL,
                "https://example.com");

        AtomicReference<WebChromeClient> clientReference =
                new AtomicReference<>();

        try (ActivityScenario<WebAppActivity> scenario =
                     ActivityScenario.launch(
                             intent)) {

            scenario.onActivity(
                    activity -> {

                        WebView webView =
                                activity.findViewById(
                                        R.id.webAppView);

                        assertNotNull(
                                webView);

                        clientReference.set(
                                webView.getWebChromeClient());
                    });
        }

        WebChromeClient client =
                clientReference.get();

        assertNotNull(
                client);

        Method fileChooser =
                client.getClass()
                        .getMethod(
                                "onShowFileChooser",
                                WebView.class,
                                ValueCallback.class,
                                WebChromeClient.FileChooserParams.class);

        Method permissionRequest =
                client.getClass()
                        .getMethod(
                                "onPermissionRequest",
                                PermissionRequest.class);

        assertNotEquals(
                WebChromeClient.class,
                fileChooser.getDeclaringClass());

        assertNotEquals(
                WebChromeClient.class,
                permissionRequest.getDeclaringClass());
    }
    private static final class TestFileChooserParams
            extends WebChromeClient.FileChooserParams {

        private final String[] acceptTypes;
        private final int mode;

        TestFileChooserParams(
                String[] acceptTypes,
                int mode) {

            this.acceptTypes =
                    acceptTypes;

            this.mode =
                    mode;
        }

        @Override
        public Intent createIntent() {

            Intent intent =
                    new Intent(
                            Intent.ACTION_GET_CONTENT);

            if (acceptTypes.length > 0) {

                intent.setType(
                        acceptTypes[0]);

                intent.putExtra(
                        Intent.EXTRA_MIME_TYPES,
                        acceptTypes);
            }

            if (mode
                    == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE) {

                intent.putExtra(
                        Intent.EXTRA_ALLOW_MULTIPLE,
                        true);
            }

            return intent;
        }

        @Override
        public String[] getAcceptTypes() {
            return acceptTypes;
        }

        @Override
        public String getFilenameHint() {
            return null;
        }

        @Override
        public int getMode() {
            return mode;
        }

        @Override
        public CharSequence getTitle() {
            return null;
        }

        @Override
        public boolean isCaptureEnabled() {
            return false;
        }
    }
}
