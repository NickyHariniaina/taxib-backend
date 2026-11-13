package com.nicky.hariniaina.security;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class HealthGuardFilterTest {

  private final HealthGuardFilter filter = new HealthGuardFilter();

  private MockHttpServletResponse run(String path) throws Exception {
    var request = new MockHttpServletRequest("GET", path);
    var response = new MockHttpServletResponse();
    FilterChain chain = (req, res) -> ((HttpServletResponse) res).setStatus(200);
    filter.doFilter(request, response, chain);
    return response;
  }

  @Test
  void shouldBlockTheMailProbe() throws Exception {
    var response = run("/health/email");

    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(response.getContentType()).startsWith("application/problem+json");
  }

  @Test
  void shouldKeepTheDbProbeReachable() throws Exception {
    assertThat(run("/health/db").getStatus()).isEqualTo(200);
  }

  @Test
  void shouldKeepPingReachable() throws Exception {
    assertThat(run("/ping").getStatus()).isEqualTo(200);
  }

  @Test
  void shouldNotInterfereWithTheApi() throws Exception {
    assertThat(run("/api/lines").getStatus()).isEqualTo(200);
  }
}
