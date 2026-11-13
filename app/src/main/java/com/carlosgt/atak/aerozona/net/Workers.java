package com.carlosgt.atak.aerozona.net;

import com.atakmap.coremap.log.Log;

import java.util.concurrent.ThreadFactory;

/**
 * Threads for the plugin's background work. An error escaping a task (an
 * OutOfMemoryError while parsing a big download, say) ends that thread only and is
 * logged; the executor replaces the thread for the next task.
 *
 * <p>The error is caught inside the thread's own run(), so it is never "uncaught". A
 * per-thread UncaughtExceptionHandler is not enough: it ran (2026-11-13, emulator) and
 * ATAK's crash reporter (ACRA) still killed the app right after.
 */
public final class Workers implements ThreadFactory {

    private static final String TAG = "AeroZona";

    private final String name;
    private int n;

    public Workers(String name) {
        this.name = name;
    }

    @Override
    public synchronized Thread newThread(final Runnable r) {
        final Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    r.run();
                } catch (Throwable e) {
                    // the executor has already counted this worker out and starts another
                    try {
                        Log.e(TAG, "error in " + Thread.currentThread().getName() + ": " + e);
                    } catch (Throwable ignored) {
                        // out of memory even for the log line: nothing more to do
                    }
                }
            }
        }, "AeroZona-" + name + "-" + (++n));
        t.setDaemon(true);
        t.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread th, Throwable e) {
                Log.e(TAG, "error in " + th.getName() + ": " + e, e);
            }
        });
        return t;
    }
}
