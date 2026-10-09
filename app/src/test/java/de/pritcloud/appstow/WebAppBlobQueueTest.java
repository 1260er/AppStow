package de.pritcloud.appstow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class WebAppBlobQueueTest {

    @Test
    public void rejectsInvalidChunksBeforeEnqueue() {
        assertFalse(
                WebAppBlobDownloadBridge.isAcceptedChunk(null));
        assertFalse(
                WebAppBlobDownloadBridge.isAcceptedChunk(new byte[0]));
        assertTrue(
                WebAppBlobDownloadBridge.isAcceptedChunk(new byte[1]));
        assertTrue(
                WebAppBlobDownloadBridge.isAcceptedChunk(
                        new byte[64 * 1024]));
        assertFalse(
                WebAppBlobDownloadBridge.isAcceptedChunk(
                        new byte[64 * 1024 + 1]));
    }

    @Test
    public void rejectsFloodWhenWorkerQueueIsFull()
            throws Exception {

        ThreadPoolExecutor executor =
                WebAppBlobDownloadBridge.createDownloadExecutor();

        CountDownLatch workerEntered = new CountDownLatch(1);
        CountDownLatch releaseWorker = new CountDownLatch(1);

        try {
            executor.execute(() -> {
                workerEntered.countDown();

                try {
                    releaseWorker.await();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
            });

            assertTrue(workerEntered.await(5, TimeUnit.SECONDS));

            int capacity =
                    executor.getQueue().remainingCapacity();

            assertTrue(capacity > 0);
            assertTrue(capacity <= 8);

            for (int index = 0; index < capacity; index++) {
                executor.execute(() -> {});
            }

            assertEquals(0, executor.getQueue().remainingCapacity());

            try {
                executor.execute(() -> {});
                fail("Full worker queue must reject messages");
            } catch (RejectedExecutionException expected) {
                // Kein unbegrenztes Puffern von Blob-Nachrichten.
            }

        } finally {
            releaseWorker.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }
}
