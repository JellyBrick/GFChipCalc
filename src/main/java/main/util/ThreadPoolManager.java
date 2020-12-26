package main.util;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadPoolManager {
    private static final ExecutorService executorService = Executors.newCachedThreadPool();

    @NotNull
    public static ExecutorService getThreadPool() {
        return executorService;
    }
}
