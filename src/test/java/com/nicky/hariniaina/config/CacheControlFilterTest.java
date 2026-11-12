package com.nicky.hariniaina.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CacheControlFilterTest {

  private final CacheControlFilter filter = new CacheControlFilter();

  private MockHttpServletResponse run(String method, String path, int status) throws Exception {
    var request = new MockHttpServletRequest(method, path);
    var response = new MockHttpServletResponse();
    FilterChain chain = (req, res) -> ((HttpServletResponse) res).setStatus(status);
    filter.doFilter(request, response, chain);
    return response;
  }

  @Test
  void shouldCacheReferenceEndpoints() throws Exception {
    assertThat(run("GET", "/api/lines", 200).getHeader("Cache-Control"))
        .isEqualTo("public, max-age=300");
  }

  @Test
  void shouldCacheStopsForShorter() throws Exception {
    assertThat(run("GET", "/api/stops", 200).getHeader("Cache-Control"))
        .isEqualTo("public, max-age=60");
  }

  @Test
  void shouldNotCacheHealthEndpoints() throws Exception {
    assertThat(run("GET", "/ping", 200).getHeader("Cache-Control")).isNull();
  }

  @Test
  void shouldNotCacheWrites() throws Exception {
    assertThat(run("POST", "/api/lines", 200).getHeader("Cache-Control")).isNull();
  }

  @Test
  void shouldNotCacheErrors() throws Exception {
    assertThat(run("GET", "/api/stops/42/lines", 404).getHeader("Cache-Control")).isNull();
  }
}
