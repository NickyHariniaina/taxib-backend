package com.nicky.hariniaina.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nicky.hariniaina.repository.ApiKeyRepository;
import com.nicky.hariniaina.repository.model.ApiKey;
import jakarta.servlet.FilterChain;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ApiKeyFilterTest {

  private static final String PLAIN_KEY = "taxib-secret";

  private final ApiKeyRepository repository = mock(ApiKeyRepository.class);
  private final ApiKeyFilter filter = new ApiKeyFilter(new ApiKeyRegistry(repository));

  private static MockHttpServletResponse run(
      ApiKeyFilter filter, MockHttpServletRequest request, boolean[] passed) throws Exception {
    var response = new MockHttpServletResponse();
    FilterChain chain = (req, res) -> passed[0] = true;
    filter.doFilter(request, response, chain);
    return response;
  }

  private static ApiKey activeKey() {
    return ApiKey.builder()
        .id(1L)
        .name("web")
        .keyHash(ApiKeyRegistry.hash(PLAIN_KEY))
        .requestsPerMinute(600)
        .requestsPerHour(20_000)
        .createdAt(Instant.parse("2026-11-13T00:00:00Z"))
        .build();
  }

  @Test
  void shouldAcceptAValidKey() throws Exception {
    when(repository.findAllByRevokedAtIsNull()).thenReturn(List.of(activeKey()));
    var request = new MockHttpServletRequest("GET", "/api/lines");
    request.addHeader(ApiKeyFilter.HEADER, PLAIN_KEY);
    boolean[] passed = {false};

    var response = run(filter, request, passed);

    assertThat(passed[0]).isTrue();
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(request.getAttribute(ApiKeyFilter.KEY_ATTRIBUTE)).isInstanceOf(ApiKey.class);
  }

  @Test
  void shouldRejectAMissingKey() throws Exception {
    when(repository.findAllByRevokedAtIsNull()).thenReturn(List.of(activeKey()));
    boolean[] passed = {false};

    var response = run(filter, new MockHttpServletRequest("GET", "/api/lines"), passed);

    assertThat(passed[0]).isFalse();
    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(response.getContentType()).startsWith("application/problem+json");
  }

  @Test
  void shouldRejectAnUnknownKey() throws Exception {
    when(repository.findAllByRevokedAtIsNull()).thenReturn(List.of(activeKey()));
    var request = new MockHttpServletRequest("GET", "/api/lines");
    request.addHeader(ApiKeyFilter.HEADER, "not-the-key");
    boolean[] passed = {false};

    var response = run(filter, request, passed);

    assertThat(passed[0]).isFalse();
    assertThat(response.getStatus()).isEqualTo(401);
  }

  @Test
  void shouldFailOpenWhileNoKeyIsStored() throws Exception {
    when(repository.findAllByRevokedAtIsNull()).thenReturn(List.of());
    boolean[] passed = {false};

    var response = run(filter, new MockHttpServletRequest("GET", "/api/lines"), passed);

    assertThat(passed[0]).isTrue();
    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Test
  void shouldLetTheCorsPreflightThrough() throws Exception {
    when(repository.findAllByRevokedAtIsNull()).thenReturn(List.of(activeKey()));
    boolean[] passed = {false};

    var response = run(filter, new MockHttpServletRequest("OPTIONS", "/api/lines"), passed);

    assertThat(passed[0]).isTrue();
    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Test
  void shouldLeaveTheHealthProbesAlone() throws Exception {
    when(repository.findAllByRevokedAtIsNull()).thenReturn(List.of(activeKey()));
    boolean[] passed = {false};

    var response = run(filter, new MockHttpServletRequest("GET", "/ping"), passed);

    assertThat(passed[0]).isTrue();
    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Test
  void shouldHashWithSha256Hex() {
    assertThat(ApiKeyRegistry.hash("abc"))
        .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
  }
}
