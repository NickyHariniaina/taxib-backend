package com.nicky.hariniaina.security;

import com.nicky.hariniaina.repository.model.ApiKey;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Requires a valid {@code X-Api-Key} on {@code /api/**}.
 *
 * <p>Fail-open while the {@code api_keys} table holds no active row, so the code can ship before
 * the key exists. Inserting the first key switches enforcement on; revoking it switches it off.
 * CORS preflight and the health probes are never challenged.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ApiKeyFilter extends OncePerRequestFilter {

  public static final String HEADER = "X-Api-Key";
  public static final String KEY_ATTRIBUTE = "taxib.apiKey";

  private final ApiKeyRegistry registry;

  public ApiKeyFilter(ApiKeyRegistry registry) {
    this.registry = registry;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return HttpMethod.OPTIONS.matches(request.getMethod()) || !ErrorResponses.isApi(request);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    var keys = registry.activeKeys();
    if (keys.isEmpty()) {
      chain.doFilter(request, response);
      return;
    }

    String header = request.getHeader(HEADER);
    if (header == null || header.isBlank()) {
      ErrorResponses.write(
          response,
          HttpStatus.UNAUTHORIZED.value(),
          "Unauthorized",
          "Missing " + HEADER + " header");
      return;
    }

    ApiKey key = keys.get(ApiKeyRegistry.hash(header.strip()));
    if (key == null) {
      ErrorResponses.write(
          response, HttpStatus.UNAUTHORIZED.value(), "Unauthorized", "Invalid or revoked API key");
      return;
    }

    request.setAttribute(KEY_ATTRIBUTE, key);
    chain.doFilter(request, response);
  }
}
