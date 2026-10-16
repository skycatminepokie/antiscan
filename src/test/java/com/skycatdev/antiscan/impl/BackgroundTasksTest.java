package com.skycatdev.antiscan.impl;
import org.junit.jupiter.api.Test;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
class BackgroundTasksTest {
    @Test void queueIsBoundedDeduplicatedAndStopped() throws Exception {
        CountDownLatch entered = new CountDownLatch(2), release = new CountDownLatch(1);
        Runnable wait = () -> { entered.countDown(); try { release.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } };
        BackgroundTasks.start(() -> {});
        try {
            assertThat(BackgroundTasks.submit("one", wait)).isTrue();
            assertThat(BackgroundTasks.submit("two", wait)).isTrue();
            assertThat(entered.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(BackgroundTasks.submit("one", wait)).isFalse();
            int accepted = 0;
            for (int i = 0; i < 200; i++) if (BackgroundTasks.submit("queued" + i, wait)) accepted++;
            assertThat(accepted).isBetween(127, 128);
        } finally { release.countDown(); BackgroundTasks.stop(); }
        assertThat(BackgroundTasks.submit("stopped", () -> {})).isFalse();
    }
}
