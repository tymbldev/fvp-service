package com.fvp.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Thread-safe random number generator that provides the same random value
 * for all threads within a 6-hour window. The random value refreshes every 6 hours.
 */
@Component
public class ThreadSafeRandomGenerator {
    
    private static final Logger log = LoggerFactory.getLogger(ThreadSafeRandomGenerator.class);
    
    // 6 hours in milliseconds
    private static final long REFRESH_INTERVAL_MS = 6 * 60 * 60 * 1000L;
    
    private volatile int currentRandomValue;
    private volatile long lastRefreshTime;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "ThreadSafeRandomGenerator-Refresh");
        t.setDaemon(true);
        return t;
    });
    
    public ThreadSafeRandomGenerator() {
        log.info("Initializing ThreadSafeRandomGenerator with refresh interval of {} hours", 
                REFRESH_INTERVAL_MS / (60 * 60 * 1000));
        
        // Initialize with current time-based random value
        refreshRandomValue();
        
        // Schedule periodic refresh every 6 hours
        scheduler.scheduleAtFixedRate(
            this::refreshRandomValue,
            REFRESH_INTERVAL_MS,
            REFRESH_INTERVAL_MS,
            TimeUnit.MILLISECONDS
        );
        
        log.info("ThreadSafeRandomGenerator successfully initialized. Current random value: {}, " +
                "Next refresh scheduled in {} hours", 
                currentRandomValue, REFRESH_INTERVAL_MS / (60 * 60 * 1000));
    }
    
    /**
     * Gets the current random value. All threads will get the same value
     * within the 6-hour window.
     * 
     * @param maxValue the maximum value (exclusive) for the random number
     * @return a random integer between 0 (inclusive) and maxValue (exclusive)
     */
    public int getRandomValue(int maxValue) {
        if (maxValue <= 0) {
            log.warn("getRandomValue called with invalid maxValue: {}. Returning 0", maxValue);
            return 0;
        }
        
        long startTime = System.currentTimeMillis();
        lock.readLock().lock();
        try {
            // Check if we need to refresh (safety check in case scheduler fails)
            long currentTime = System.currentTimeMillis();
            long timeSinceLastRefresh = currentTime - lastRefreshTime;
            
            if (timeSinceLastRefresh > REFRESH_INTERVAL_MS) {
                log.info("Random value refresh needed. Time since last refresh: {} ms (threshold: {} ms). " +
                        "Upgrading to write lock for refresh", timeSinceLastRefresh, REFRESH_INTERVAL_MS);
                
                // Upgrade to write lock for refresh
                lock.readLock().unlock();
                lock.writeLock().lock();
                try {
                    // Double-check after acquiring write lock
                    if (currentTime - lastRefreshTime > REFRESH_INTERVAL_MS) {
                        log.info("Performing emergency refresh of random value");
                        refreshRandomValue();
                    }
                } finally {
                    lock.writeLock().unlock();
                    lock.readLock().lock();
                }
            }
            
            int result = Math.abs(currentRandomValue) % maxValue;
            long duration = System.currentTimeMillis() - startTime;
            
            log.debug("getRandomValue called with maxValue: {}, returned: {}, " +
                    "currentRandomValue: {}, timeSinceLastRefresh: {} ms, duration: {} ms",
                    maxValue, result, currentRandomValue, timeSinceLastRefresh, duration);
            
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Gets the current random value as a double between 0.0 and 1.0.
     * All threads will get the same value within the 6-hour window.
     * 
     * @return a random double between 0.0 (inclusive) and 1.0 (exclusive)
     */
    public double getRandomDouble() {
        long startTime = System.currentTimeMillis();
        lock.readLock().lock();
        try {
            // Check if we need to refresh (safety check in case scheduler fails)
            long currentTime = System.currentTimeMillis();
            long timeSinceLastRefresh = currentTime - lastRefreshTime;
            
            if (timeSinceLastRefresh > REFRESH_INTERVAL_MS) {
                log.info("Random value refresh needed for getRandomDouble. Time since last refresh: {} ms " +
                        "(threshold: {} ms). Upgrading to write lock for refresh", 
                        timeSinceLastRefresh, REFRESH_INTERVAL_MS);
                
                // Upgrade to write lock for refresh
                lock.readLock().unlock();
                lock.writeLock().lock();
                try {
                    // Double-check after acquiring write lock
                    if (currentTime - lastRefreshTime > REFRESH_INTERVAL_MS) {
                        log.info("Performing emergency refresh of random value for getRandomDouble");
                        refreshRandomValue();
                    }
                } finally {
                    lock.writeLock().unlock();
                    lock.readLock().lock();
                }
            }
            
            // Convert to double between 0.0 and 1.0
            double result = (double) Math.abs(currentRandomValue) / Integer.MAX_VALUE;
            long duration = System.currentTimeMillis() - startTime;
            
            log.debug("getRandomDouble called, returned: {}, currentRandomValue: {}, " +
                    "timeSinceLastRefresh: {} ms, duration: {} ms",
                    result, currentRandomValue, timeSinceLastRefresh, duration);
            
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Refreshes the random value. This method is thread-safe and can be called
     * by multiple threads safely.
     */
    private void refreshRandomValue() {
        long refreshStartTime = System.currentTimeMillis();
        lock.writeLock().lock();
        try {
            // Use current time as seed for deterministic but changing values
            long seed = System.currentTimeMillis();
            int previousValue = currentRandomValue;
            currentRandomValue = (int) (seed ^ (seed >>> 32));
            lastRefreshTime = System.currentTimeMillis();
            
            long refreshDuration = lastRefreshTime - refreshStartTime;
            long timeUntilNextRefresh = REFRESH_INTERVAL_MS;
            
            log.info("Random value refreshed successfully. Previous value: {}, new value: {}, " +
                    "refresh duration: {} ms, next refresh in: {} hours",
                    previousValue, currentRandomValue, refreshDuration, 
                    timeUntilNextRefresh / (60 * 60 * 1000));
            
            log.debug("Random value refresh details - seed: {}, timestamp: {}, " +
                    "next refresh scheduled for: {}", 
                    seed, lastRefreshTime, lastRefreshTime + REFRESH_INTERVAL_MS);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Gets the time until the next refresh in milliseconds.
     * 
     * @return milliseconds until next refresh
     */
    public long getTimeUntilNextRefresh() {
        lock.readLock().lock();
        try {
            long timeSinceLastRefresh = System.currentTimeMillis() - lastRefreshTime;
            long timeUntilNextRefresh = Math.max(0, REFRESH_INTERVAL_MS - timeSinceLastRefresh);
            
            log.debug("getTimeUntilNextRefresh called - time since last refresh: {} ms, " +
                    "time until next refresh: {} ms ({} hours)", 
                    timeSinceLastRefresh, timeUntilNextRefresh, 
                    timeUntilNextRefresh / (60 * 60 * 1000));
            
            return timeUntilNextRefresh;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Shuts down the scheduler. Should be called when the application is shutting down.
     */
    public void shutdown() {
        log.info("Initiating ThreadSafeRandomGenerator shutdown. Current random value: {}, " +
                "time since last refresh: {} ms", 
                currentRandomValue, System.currentTimeMillis() - lastRefreshTime);
        
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("Scheduler did not terminate gracefully within 5 seconds, forcing shutdown");
                scheduler.shutdownNow();
            } else {
                log.info("Scheduler terminated gracefully");
            }
        } catch (InterruptedException e) {
            log.warn("Shutdown interrupted, forcing immediate shutdown", e);
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        log.info("ThreadSafeRandomGenerator shutdown completed successfully");
    }
    
    /**
     * Gets the current random value for monitoring purposes.
     * This method is thread-safe and provides read-only access.
     * 
     * @return the current random value
     */
    public int getCurrentRandomValue() {
        lock.readLock().lock();
        try {
            log.debug("getCurrentRandomValue called, returning: {}", currentRandomValue);
            return currentRandomValue;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Gets the timestamp of the last refresh for monitoring purposes.
     * This method is thread-safe and provides read-only access.
     * 
     * @return the timestamp of the last refresh in milliseconds
     */
    public long getLastRefreshTime() {
        lock.readLock().lock();
        try {
            log.debug("getLastRefreshTime called, returning: {}", lastRefreshTime);
            return lastRefreshTime;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Gets detailed status information for monitoring and debugging.
     * This method is thread-safe and provides comprehensive status.
     * 
     * @return a formatted string with current status information
     */
    public String getStatus() {
        lock.readLock().lock();
        try {
            long currentTime = System.currentTimeMillis();
            long timeSinceLastRefresh = currentTime - lastRefreshTime;
            long timeUntilNextRefresh = Math.max(0, REFRESH_INTERVAL_MS - timeSinceLastRefresh);
            
            String status = String.format(
                "ThreadSafeRandomGenerator Status - Current Value: %d, " +
                "Last Refresh: %d ms ago, Next Refresh: %d ms (%d hours), " +
                "Refresh Interval: %d ms (%d hours), Scheduler Active: %s",
                currentRandomValue,
                timeSinceLastRefresh,
                timeUntilNextRefresh,
                timeUntilNextRefresh / (60 * 60 * 1000),
                REFRESH_INTERVAL_MS,
                REFRESH_INTERVAL_MS / (60 * 60 * 1000),
                !scheduler.isShutdown()
            );
            
            log.debug("getStatus called, returning: {}", status);
            return status;
        } finally {
            lock.readLock().unlock();
        }
    }
}
