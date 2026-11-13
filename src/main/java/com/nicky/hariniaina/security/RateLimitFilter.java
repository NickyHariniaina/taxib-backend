package com.nicky.hariniaina.security;

import com.nicky.hariniaina.repository.model.ApiKey;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Token buckets on {@code /api/**}:
 *
 * <ul>
 *   <li>per client IP, key or not: 60 requests per minute — the public web key is shared by every
 *       visitor, so the per-key cap alone cannot protect the API;
 *   <li>per API key: the allowance stored on the row (600/min and 20 000/h by default);
 *   <li>per instance: 3 000 requests per minute as a backstop.
 * </ul>
 *
 * <p>Counters live in the container, so a Lambda scaled to N containers allows N times these
 * numbers; the API Gateway throttle is the authoritative global limit.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class RateLimitFilter extends OncePerRequestFilter {

  static final int IP_PER_MINUTE = 60;
  static final int KEY_PER_MINUTE = 600;
  static final int KEY_PER_HOUR = 20_000;
  static final int GLOBAL_PER_MINUTE = 3_000;
  static final int MAX_TRACKED_CLIENTS = 10_000;

  private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
  private final Bucket global = bucket(GLOBAL_PER_MINUTE, Duration.ofMinutes(1));

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return HttpMethod.OPTIONS.matches(request.getMethod()) || !ErrorResponses.isApi(request);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (!global.tryConsume(1)) {
      reject(response, global);
      return;
    }

    evictStaleClients();
    Bucket perIp = buckets.computeIfAbsent("ip:" + clientIp(request), id -> ipBucket());
    if (!perIp.tryConsume(1)) {
      reject(response, perIp);
      return;
    }

    Object key = request.getAttribute(ApiKeyFilter.KEY_ATTRIBUTE);
    if (key instanceof ApiKey apiKey && !consumeKeyed(apiKey)) {
      reject(response, buckets.get("key:" + apiKey.getId()));
      return;
    }

    chain.doFilter(request, response);
  }

  private boolean consumeKeyed(ApiKey apiKey) {
    Bucket bucket =
        buckets.computeIfAbsent(
            "key:" + apiKey.getId(),
            id ->
                bucket(
                    apiKey.getRequestsPerMinute(),
                    Duration.ofMinutes(1),
                    apiKey.getRequestsPerHour(),
                    Duration.ofHours(1)));
    return bucket.tryConsume(1);
  }

  /** API Gateway appends the real source address last, so a forged header lands first. */
  static String clientIp(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      String[] hops = forwarded.split(",");
      String last = hops[hops.length - 1].strip();
      if (!last.isEmpty()) {
        return last;
      }
    }
    return request.getRemoteAddr();
  }

  private void evictStaleClients() {
    if (buckets.size() > MAX_TRACKED_CLIENTS) {
      buckets.clear();
    }
  }

  private void reject(HttpServletResponse response, Bucket bucket) {
    long seconds =
        bucket == null
            ? 1
            : Math.max(
                1,
                TimeUnit.NANOSECONDS.toSeconds(
                    bucket.estimateAbilityToConsume(1).getNanosToWaitForRefill()));
    response.setHeader("Retry-After", Long.toString(seconds));
    ErrorResponses.write(
        response, HttpStatus.TOO_MANY_REQUESTS.value(), "Too Many Requests", "Rate limit exceeded");
  }

  private static Bucket ipBucket() {
    return bucket(IP_PER_MINUTE, Duration.ofMinutes(1));
  }

  private static Bucket bucket(int perPeriod, Duration period) {
    return Bucket.builder().addLimit(greedy(perPeriod, period)).build();
  }

  private static Bucket bucket(int perMinute, Duration minute, int perHour, Duration hour) {
    return Bucket.builder()
        .addLimit(greedy(perMinute, minute))
        .addLimit(greedy(perHour, hour))
        .build();
  }

  private static Bandwidth greedy(int tokens, Duration period) {
    return Bandwidth.builder().capacity(tokens).refillGreedy(tokens, period).build();
  }
}
