package main.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadPoolManager {
    private static final ExecutorService executorService = Executors.newCachedThreadPool();

    public static ExecutorService getThreadPool() {
        return executorService;
    }
}
