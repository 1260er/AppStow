package de.pritcloud.appstow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.net.Uri;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class BackupRestoreConcurrencyTest {

    private Context context;

    @Before
    public void setUp() throws Exception {
        context = RuntimeEnvironment.getApplication();
        BackupRestoreJournal.complete(context);
        for (String name : Arrays.asList(
                "categories", "favorites", "shortcuts",
                "overview_order", "section_item_order",
                "overview_display", "sorting_settings",
                "ui_settings", "statistics_display",
                "usage_statistics")) {
            assertTrue(context.getSharedPreferences(
                            name, Context.MODE_PRIVATE)
                    .edit().clear().commit());
        }
    }

    @After
    public void tearDown() throws Exception {
        BackupRestoreJournal.complete(context);
    }

    @Test
    public void restoreAndStartupRecoveryUseSameClassMonitor()
            throws Exception {
        Method restore = BackupManager.class.getDeclaredMethod(
                "restoreBackup", Context.class,
                JSONObject.class, Set.class);
        Method recovery = BackupManager.class.getDeclaredMethod(
                "recoverInterruptedRestore", Context.class);

        assertTrue(Modifier.isStatic(restore.getModifiers()));
        assertTrue(Modifier.isStatic(recovery.getModifiers()));
        assertTrue(Modifier.isSynchronized(restore.getModifiers()));
        assertTrue(Modifier.isSynchronized(recovery.getModifiers()));
    }

    @Test
    public void startupDuringRestoreMustNotConsumeLiveJournal()
            throws Exception {
        File backupFile = File.createTempFile(
                "appstow-restore-concurrency-", ".json",
                context.getCacheDir());
        JSONObject backup;
        try {
            Uri uri = Uri.fromFile(backupFile);
            BackupManager.writeBackup(context, uri);
            backup = BackupManager.readBackup(context, uri);
        } finally {
            backupFile.delete();
        }

        SharedPreferences categories = context.getSharedPreferences(
                "categories", Context.MODE_PRIVATE);
        assertTrue(categories.edit()
                .putString("category_list", "previous local state")
                .commit());

        CountDownLatch commitEntered = new CountDownLatch(1);
        CountDownLatch releaseCommit = new CountDownLatch(1);
        CountDownLatch recoveryStarted = new CountDownLatch(1);
        AtomicBoolean blocked = new AtomicBoolean(false);

        SharedPreferences wrappedCategories = pauseFirstCommit(
                categories, commitEntered, releaseCommit, blocked);
        Context delayedContext = new ContextWrapper(context) {
            @Override
            public SharedPreferences getSharedPreferences(
                    String name, int mode) {
                if ("categories".equals(name)) {
                    return wrappedCategories;
                }
                return super.getSharedPreferences(name, mode);
            }
        };

        ExecutorService workers = Executors.newFixedThreadPool(2);
        File journal = new File(
                context.getFilesDir(),
                "appstow-restore-journal.json");

        try {
            Future<?> restoring = workers.submit(() -> {
                BackupManager.restoreBackup(
                        delayedContext, backup, Set.of());
                return null;
            });

            assertTrue("Restore did not reach category commit",
                    commitEntered.await(15, TimeUnit.SECONDS));
            assertTrue("Recovery journal must exist during restore",
                    journal.isFile());

            // Simuliert MainActivity.onCreate nach einem Sprachwechsel.
            Future<Boolean> startupRecovery = workers.submit(() -> {
                recoveryStarted.countDown();
                return BackupManager.recoverInterruptedRestore(context);
            });

            assertTrue(recoveryStarted.await(5, TimeUnit.SECONDS));
            try {
                startupRecovery.get(250, TimeUnit.MILLISECONDS);
                fail("Startup recovery ran during an active restore");
            } catch (TimeoutException expected) {
                // Die Journal-Ruecksicherung wartet korrekt.
            }

            assertTrue("Running restore must keep its journal",
                    journal.isFile());
            releaseCommit.countDown();

            restoring.get(20, TimeUnit.SECONDS);
            assertFalse("Completed restore must not roll back",
                    startupRecovery.get(20, TimeUnit.SECONDS));

            assertEquals("[]",
                    categories.getString("category_list", null));
            assertFalse(journal.exists());

        } finally {
            releaseCommit.countDown();
            workers.shutdownNow();
            assertTrue(workers.awaitTermination(
                    20, TimeUnit.SECONDS));
        }
    }

    private static SharedPreferences pauseFirstCommit(
            SharedPreferences original,
            CountDownLatch entered,
            CountDownLatch release,
            AtomicBoolean blocked) {

        return (SharedPreferences) Proxy.newProxyInstance(
                SharedPreferences.class.getClassLoader(),
                new Class<?>[]{SharedPreferences.class},
                (ignoredProxy, method, arguments) -> {
                    if (!"edit".equals(method.getName())) {
                        return invoke(method, original, arguments);
                    }

                    SharedPreferences.Editor realEditor = original.edit();
                    return Proxy.newProxyInstance(
                            SharedPreferences.Editor.class.getClassLoader(),
                            new Class<?>[]{SharedPreferences.Editor.class},
                            (editorProxy, editorMethod, editorArguments) -> {
                                if ("commit".equals(editorMethod.getName())
                                        && blocked.compareAndSet(false, true)) {
                                    entered.countDown();
                                    if (!release.await(15, TimeUnit.SECONDS)) {
                                        throw new IllegalStateException(
                                                "Test commit was not released");
                                    }
                                }
                                Object result = invoke(
                                        editorMethod, realEditor, editorArguments);
                                return result == realEditor
                                        ? editorProxy : result;
                            });
                });
    }

    private static Object invoke(
            Method method, Object target, Object[] args)
            throws Throwable {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException exception) {
            throw exception.getCause();
        }
    }
}
