package com.fvp.util;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Utility class to handle cache bypass using ThreadLocal.
 * This class ensures proper propagation of cache bypass flag to background threads
 * and cleanup of ThreadLocal to prevent memory leaks.
 */
public class CacheBypassUtil {
    private static final ThreadLocal<Boolean> CACHE_BYPASS = new ThreadLocal<>();

    /**
     * Set cache bypass flag for current thread
     * @param bypass true to bypass cache, false otherwise
     */
    public static void setCacheBypass(boolean bypass) {
        CACHE_BYPASS.set(bypass);
    }

    /**
     * Get cache bypass flag for current thread
     * @return true if cache should be bypassed, false otherwise
     */
    public static boolean isCacheBypass() {
        Boolean bypass = CACHE_BYPASS.get();
        return bypass != null && bypass;
    }

    /**
     * Clear cache bypass flag for current thread
     */
    public static void clearCacheBypass() {
        CACHE_BYPASS.remove();
    }

    /**
     * Execute a task with cache bypass flag propagated to the background thread
     * @param executor The executor service to use
     * @param task The task to execute
     * @param <T> The return type of the task
     * @return Future representing the task
     */
    public static <T> Future<T> executeWithCacheBypass(ExecutorService executor, Callable<T> task) {
        boolean bypass = isCacheBypass();
        return executor.submit(() -> {
            try {
                if (bypass) {
                    setCacheBypass(true);
                }
                return task.call();
            } finally {
                clearCacheBypass();
            }
        });
    }

    /**
     * Execute a task with cache bypass flag propagated to the background thread
     * @param executor The executor service to use
     * @param task The task to execute
     */
    public static void executeWithCacheBypass(ExecutorService executor, Runnable task) {
        boolean bypass = isCacheBypass();
        executor.execute(() -> {
            try {
                if (bypass) {
                    setCacheBypass(true);
                }
                task.run();
            } finally {
                clearCacheBypass();
            }
        });
    }
} 