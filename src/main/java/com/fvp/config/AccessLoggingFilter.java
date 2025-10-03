package com.fvp.config;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Filter to log HTTP access information including URL, user agent, time taken, and response code
 */
@Component
@Order(1)
public class AccessLoggingFilter implements Filter {

  private static final Logger accessLogger = LoggerFactory.getLogger("ACCESS_LOG");
  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    // No initialization needed
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    
    if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
      chain.doFilter(request, response);
      return;
    }

    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;

    // Record start time
    long startTime = System.currentTimeMillis();
    
    // Get request details
    String method = httpRequest.getMethod();
    String requestURI = httpRequest.getRequestURI();
    String queryString = httpRequest.getQueryString();
    String userAgent = httpRequest.getHeader("User-Agent");
    String remoteAddr = getClientIpAddress(httpRequest);
    String referer = httpRequest.getHeader("Referer");
    
    // Build full URL
    String fullUrl = requestURI;
    if (queryString != null && !queryString.isEmpty()) {
      fullUrl += "?" + queryString;
    }

    try {
      // Continue with the request
      chain.doFilter(request, response);
    } finally {
      // Calculate processing time
      long endTime = System.currentTimeMillis();
      long processingTime = endTime - startTime;
      
      // Get response details
      int statusCode = httpResponse.getStatus();
      
      // Log access information in Apache Common Log Format with additional fields
      String accessLogEntry = String.format(
          "%s - - [%s] \"%s %s HTTP/1.1\" %d - %d \"%s\" \"%s\"",
          remoteAddr,
          LocalDateTime.now().format(DATE_TIME_FORMATTER),
          method,
          fullUrl,
          statusCode,
          processingTime,
          referer != null ? referer : "-",
          userAgent != null ? userAgent : "-"
      );
      
      accessLogger.info(accessLogEntry);
    }
  }

  @Override
  public void destroy() {
    // No cleanup needed
  }

  /**
   * Get the real client IP address, considering proxy headers
   */
  private String getClientIpAddress(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
      // X-Forwarded-For can contain multiple IPs, take the first one
      return xForwardedFor.split(",")[0].trim();
    }
    
    String xRealIp = request.getHeader("X-Real-IP");
    if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
      return xRealIp;
    }
    
    return request.getRemoteAddr();
  }
}
