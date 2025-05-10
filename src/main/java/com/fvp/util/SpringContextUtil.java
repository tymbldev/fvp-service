package com.fvp.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Utility class that provides static access to Spring beans.
 * This is used to access the dynamic query builder from repository default methods.
 */
@Component
public class SpringContextUtil implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        applicationContext = context;
    }

    /**
     * Get a Spring bean by class type.
     *
     * @param beanClass The class type of the bean to retrieve
     * @param <T>       The bean type
     * @return The bean instance
     */
    public static <T> T getBean(Class<T> beanClass) {
        if (applicationContext == null) {
            throw new IllegalStateException("ApplicationContext has not been set");
        }
        return applicationContext.getBean(beanClass);
    }

    /**
     * Get a Spring bean by name and class type.
     *
     * @param beanName  The name of the bean to retrieve
     * @param beanClass The class type of the bean to retrieve
     * @param <T>       The bean type
     * @return The bean instance
     */
    public static <T> T getBean(String beanName, Class<T> beanClass) {
        if (applicationContext == null) {
            throw new IllegalStateException("ApplicationContext has not been set");
        }
        return applicationContext.getBean(beanName, beanClass);
    }
} 