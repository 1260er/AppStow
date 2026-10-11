package de.pritcloud.appstow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class WebViewLagDiagnosticsTest {

    @Test
    public void enabledOnlyInSeparateDevelopmentApp() {
        assertTrue(WebViewLagDiagnostics.enabledForPackage(
                "de.pritcloud.appstow.dev"));
        assertFalse(WebViewLagDiagnostics.enabledForPackage(
                "de.pritcloud.appstow"));
        assertFalse(WebViewLagDiagnostics.enabledForPackage(null));
        assertFalse(WebViewLagDiagnostics.enabledForPackage(""));
    }

    @Test
    public void oldSamplesAreBoundedAndChronological() {
        WebViewLagDiagnostics.EventBuffer buffer =
                new WebViewLagDiagnostics.EventBuffer(3);
        buffer.add("sample-1");
        buffer.add("sample-2");
        buffer.add("sample-3");
        buffer.add("sample-4");
        assertEquals(List.of("sample-2", "sample-3", "sample-4"),
                buffer.snapshot());
    }

    @Test
    public void reportSnapshotIsIndependentFromSubsequentSamples() {
        WebViewLagDiagnostics.EventBuffer buffer =
                new WebViewLagDiagnostics.EventBuffer(2);
        buffer.add("startup");
        List<String> copy = buffer.snapshot();
        buffer.add("later");
        assertEquals(List.of("startup"), copy);
        assertEquals(List.of("startup", "later"), buffer.snapshot());
    }

    @Test(expected = IllegalArgumentException.class)
    public void logBufferRejectsInvalidCapacity() {
        new WebViewLagDiagnostics.EventBuffer(0);
    }
}
