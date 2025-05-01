package com.fvp.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * Utility class for logging operations with timing information
 */
public class LoggingUtil {
    
    /**
     * Executes a supplier and logs the time taken
     * 
     * @param logger The logger to use
     * @param operationName The name of the operation being performed
     * @param supplier The supplier to execute
     * @param <T> The return type of the supplier
     * @return The result of the supplier
     */
    public static <T> T logOperationTime(Logger logger, String operationName, Supplier<T> supplier) {
        long startTime = System.currentTimeMillis();
        try {
            logger.info("Starting {} operation", operationName);
            T result = supplier.get();
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            logger.info("Completed {} operation in {} ms", operationName, duration);
            return result;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            logger.error("Error in {} operation after {} ms: {}", operationName, duration, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Executes a runnable and logs the time taken
     * 
     * @param logger The logger to use
     * @param operationName The name of the operation being performed
     * @param runnable The runnable to execute
     */
    public static void logOperationTime(Logger logger, String operationName, Runnable runnable) {
        long startTime = System.currentTimeMillis();
        try {
            logger.info("Starting {} operation", operationName);
            runnable.run();
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            if(duration < 100){
                logger.info("Completed {} operation in {} ms", operationName, duration);
            }else{
                logger.info("High time Completed {} operation in {} ms", operationName, duration);
            }
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            logger.error("Error in {} operation after {} ms: {}", operationName, duration, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Creates a logger for the specified class
     * 
     * @param clazz The class to create a logger for
     * @return The logger
     */
    public static Logger getLogger(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }
} 