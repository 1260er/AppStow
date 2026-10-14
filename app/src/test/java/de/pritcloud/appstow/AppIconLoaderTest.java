package de.pritcloud.appstow;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Looper;
import android.widget.ImageView;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
@LooperMode(LooperMode.Mode.PAUSED)
public class AppIconLoaderTest {

    private static AppEntry entry(
            String packageName,
            int generation,
            int color,
            CountDownLatch entered,
            CountDownLatch release,
            AtomicInteger loads) {

        ResolveInfo resolveInfo = new ResolveInfo() {
            @Override
            public Drawable loadIcon(PackageManager manager) {
                loads.incrementAndGet();
                entered.countDown();

                try {
                    if (!release.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException(
                                "Icon test timed out");
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    return null;
                }

                return new ColorDrawable(color);
            }
        };

        resolveInfo.activityInfo = new ActivityInfo();
        resolveInfo.activityInfo.packageName = packageName;
        resolveInfo.activityInfo.name = "ExampleActivity";

        return new AppEntry(
                "Example", packageName, resolveInfo, null, generation);
    }

    private static void awaitLoaded(AppEntry entry)
            throws InterruptedException {

        long deadline = System.nanoTime()
                + TimeUnit.SECONDS.toNanos(5);

        while (entry.iconState == null
                && System.nanoTime() < deadline) {
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            Thread.sleep(10);
        }

        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertNotNull(entry.iconState);
    }

    @Test
    public void duplicateBindingsShareOneIconLoad()
            throws Exception {

        Context context = ApplicationProvider.getApplicationContext();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger loads = new AtomicInteger();

        AppEntry app = entry(
                "test.icon.shared", 1, Color.RED,
                entered, release, loads);

        AppIconLoader loader =
                new AppIconLoader(context.getPackageManager());

        ImageView first = new ImageView(context);
        ImageView second = new ImageView(context);

        try {
            loader.bind(first, app);
            assertTrue(entered.await(5, TimeUnit.SECONDS));

            loader.bind(second, app);
            release.countDown();

            awaitLoaded(app);

            assertEquals(1, loads.get());
            assertEquals(Color.RED,
                    ((ColorDrawable) first.getDrawable()).getColor());
            assertEquals(Color.RED,
                    ((ColorDrawable) second.getDrawable()).getColor());
        } finally {
            release.countDown();
            loader.shutdown();
        }
    }

    @Test
    public void staleResultCannotReplaceNewBindingWithSamePackage()
            throws Exception {

        Context context = ApplicationProvider.getApplicationContext();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger loads = new AtomicInteger();

        AppEntry oldEntry = entry(
                "test.icon.same", 1, Color.RED,
                entered, release, loads);

        AppEntry newEntry = entry(
                "test.icon.same", 2, Color.BLUE,
                new CountDownLatch(0),
                new CountDownLatch(0),
                new AtomicInteger());

        newEntry.iconState =
                new ColorDrawable(Color.BLUE).getConstantState();

        AppIconLoader loader =
                new AppIconLoader(context.getPackageManager());

        ImageView target = new ImageView(context);

        try {
            loader.bind(target, oldEntry);
            assertTrue(entered.await(5, TimeUnit.SECONDS));

            loader.bind(target, newEntry);
            assertEquals(Color.BLUE,
                    ((ColorDrawable) target.getDrawable()).getColor());

            release.countDown();
            awaitLoaded(oldEntry);

            assertEquals(Color.BLUE,
                    ((ColorDrawable) target.getDrawable()).getColor());
        } finally {
            release.countDown();
            loader.shutdown();
        }
    }
}
