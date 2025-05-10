package com.fvp.config;

import org.hibernate.dialect.MySQL8Dialect;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.stereotype.Component;

/**
 * Custom MySQL dialect that adds a safe date subtraction function
 */
@Component
public class CustomMySQLDialect extends MySQL8Dialect {

    public CustomMySQLDialect() {
        super();
        
        // Register the DATE_SUB_SAFE function that safely subtracts days from a date
        registerFunction("DATE_SUB_SAFE", 
            new SQLFunctionTemplate(StandardBasicTypes.DATE, 
                "DATE_SUB(?1, INTERVAL ?2 DAY)"));
    }
} 