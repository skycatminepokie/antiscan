package com.skycatdev.antiscan.impl;

import com.skycatdev.antiscan.Antiscan;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

/** Bounded, deduplicated work. Never runs rejected work on the packet thread. */
public final class BackgroundTasks {
    private static final Set<String> PENDING = new HashSet<>();
    private static ThreadPoolExecutor workers;
    private static ScheduledExecutorService timer;

    public static synchronized void start(Runnable maintenance) {
        if (workers != null) return;
        workers = new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(128), r -> {
            Thread t = new Thread(r, "Antiscan background");
            t.setDaemon(true);
            return t;
        });
        timer = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Antiscan maintenance");
            t.setDaemon(true);
            return t;
        });
        timer.scheduleWithFixedDelay(() -> submit("maintenance", maintenance), 0, 60, TimeUnit.SECONDS);
    }

    public static synchronized boolean submit(String key, Runnable work) {
        if (workers == null || !PENDING.add(key)) return false;
        try {
            workers.execute(() -> {
                try { work.run(); }
                catch (RuntimeException e) { Antiscan.LOGGER.warn("Background task {} failed", key, e); }
                finally { synchronized (BackgroundTasks.class) { PENDING.remove(key); } }
            });
            return true;
        } catch (RejectedExecutionException e) {
            PENDING.remove(key);
            return false;
        }
    }

    public static void stop() {
        ThreadPoolExecutor old;
        synchronized (BackgroundTasks.class) {
            if (timer != null) timer.shutdownNow();
            old = workers;
            workers = null;
            if (old != null) old.shutdownNow();
        }
        if (old != null) {
            try { old.awaitTermination(10, TimeUnit.SECONDS); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        synchronized (BackgroundTasks.class) { PENDING.clear(); }
    }
}
