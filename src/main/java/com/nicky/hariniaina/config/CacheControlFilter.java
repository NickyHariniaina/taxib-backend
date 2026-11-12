package com.nicky.hariniaina.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Marks the read endpoints as cacheable. The reference data barely changes, so letting the browser
 * and any CDN cache the answer removes most of the traffic before it reaches the database.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class CacheControlFilter extends OncePerRequestFilter {

  private static final Duration REFERENCE_TTL = Duration.ofMinutes(5);
  private static final Duration STOPS_TTL = Duration.ofMinutes(1);

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    chain.doFilter(request, response);

    if (!"GET".equals(request.getMethod()) || response.getStatus() != HttpServletResponse.SC_OK) {
      return;
    }
    String path = request.getRequestURI();
    if (!path.startsWith("/api/")) {
      return;
    }
    Duration ttl = path.startsWith("/api/stops") ? STOPS_TTL : REFERENCE_TTL;
    response.setHeader(HttpHeaders.CACHE_CONTROL, "public, max-age=" + ttl.toSeconds());
  }
}
