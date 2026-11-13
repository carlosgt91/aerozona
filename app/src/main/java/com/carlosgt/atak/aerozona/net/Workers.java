package com.carlosgt.atak.aerozona.net;

import com.atakmap.coremap.log.Log;

import java.util.concurrent.ThreadFactory;

/**
 * Threads for the plugin's background work. An error escaping a task (an
 * OutOfMemoryError while parsing a big download, say) ends that thread only and is
 * logged: without this, ATAK's default handler would take the whole app down. The
 * executor replaces the thread for the next task.
 */
public final class Workers implements ThreadFactory {

    private static final String TAG = "AeroZona";

    private final String name;
    private int n;

    public Workers(String name) {
        this.name = name;
    }

    @Override
    public synchronized Thread newThread(Runnable r) {
        final Thread t = new Thread(r, "AeroZona-" + name + "-" + (++n));
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
