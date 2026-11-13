package com.nicky.hariniaina.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Closes the mail probe without touching its generated controller: {@code /health/email} sends five
 * real e-mails to any address via SES, which is an open relay as long as it answers.
 *
 * <p>{@code /ping} and {@code /health/db} stay reachable: they only report liveness, and the
 * deployment health check may depend on them.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class HealthGuardFilter extends OncePerRequestFilter {

  static final String MAIL_PROBE = "/health/email";

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !MAIL_PROBE.equals(ErrorResponses.path(request));
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    ErrorResponses.write(
        response, HttpStatus.FORBIDDEN.value(), "Forbidden", "The mail probe is disabled");
  }
}
