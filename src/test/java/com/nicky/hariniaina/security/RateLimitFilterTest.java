package com.nicky.hariniaina.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.nicky.hariniaina.repository.model.ApiKey;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitFilterTest {

  private final RateLimitFilter filter = new RateLimitFilter();

  private static MockHttpServletResponse call(
      RateLimitFilter filter, MockHttpServletRequest request) throws Exception {
    var response = new MockHttpServletResponse();
    FilterChain chain = (req, res) -> ((HttpServletResponse) res).setStatus(200);
    filter.doFilter(request, response, chain);
    return response;
  }

  private static MockHttpServletRequest anonymous(String ip) {
    var request = new MockHttpServletRequest("GET", "/api/lines");
    request.setRemoteAddr(ip);
    return request;
  }

  private static MockHttpServletRequest keyed(String ip, int perMinute) {
    var request = anonymous(ip);
    request.setAttribute(
        ApiKeyFilter.KEY_ATTRIBUTE,
        ApiKey.builder()
            .id(7L)
            .name("web")
            .keyHash("hash")
            .requestsPerMinute(perMinute)
            .requestsPerHour(20_000)
            .createdAt(Instant.parse("2026-11-13T00:00:00Z"))
            .build());
    return request;
  }

  @Test
  void shouldLimitAnAnonymousIpTo60PerMinute() throws Exception {
    for (int i = 0; i < RateLimitFilter.IP_PER_MINUTE; i++) {
      assertThat(call(filter, anonymous("1.2.3.4")).getStatus()).isEqualTo(200);
    }

    var blocked = call(filter, anonymous("1.2.3.4"));

    assertThat(blocked.getStatus()).isEqualTo(429);
    assertThat(blocked.getHeader("Retry-After")).isNotNull();
    assertThat(blocked.getContentType()).startsWith("application/problem+json");
  }

  @Test
  void shouldKeepTheClientsIndependent() throws Exception {
    for (int i = 0; i < RateLimitFilter.IP_PER_MINUTE; i++) {
      call(filter, anonymous("1.2.3.4"));
    }

    assertThat(call(filter, anonymous("5.6.7.8")).getStatus()).isEqualTo(200);
  }

  @Test
  void shouldApplyTheAllowanceStoredOnTheKey() throws Exception {
    assertThat(call(filter, keyed("1.2.3.4", 2)).getStatus()).isEqualTo(200);
    assertThat(call(filter, keyed("1.2.3.4", 2)).getStatus()).isEqualTo(200);

    assertThat(call(filter, keyed("1.2.3.4", 2)).getStatus()).isEqualTo(429);
  }

  @Test
  void shouldNotBucketKeyedTrafficPerIp() throws Exception {
    for (int i = 0; i < RateLimitFilter.IP_PER_MINUTE + 1; i++) {
      assertThat(call(filter, keyed("1.2.3.4", 600)).getStatus()).isEqualTo(200);
    }
  }

  @Test
  void shouldUseTheLastForwardedForEntry() throws Exception {
    for (int i = 0; i < RateLimitFilter.IP_PER_MINUTE; i++) {
      call(filter, anonymous("1.2.3.4"));
    }

    var forgedFirst = anonymous("10.0.0.1");
    forgedFirst.addHeader("X-Forwarded-For", "1.2.3.4, 9.9.9.9");
    var sameClient = anonymous("10.0.0.1");
    sameClient.addHeader("X-Forwarded-For", "9.9.9.9, 1.2.3.4");

    assertThat(call(filter, forgedFirst).getStatus()).isEqualTo(200);
    assertThat(call(filter, sameClient).getStatus()).isEqualTo(429);
  }

  @Test
  void shouldNotLimitTheCorsPreflight() throws Exception {
    for (int i = 0; i < RateLimitFilter.IP_PER_MINUTE; i++) {
      call(filter, anonymous("1.2.3.4"));
    }

    var preflight = new MockHttpServletRequest("OPTIONS", "/api/lines");
    preflight.setRemoteAddr("1.2.3.4");

    assertThat(call(filter, preflight).getStatus()).isEqualTo(200);
  }
}
