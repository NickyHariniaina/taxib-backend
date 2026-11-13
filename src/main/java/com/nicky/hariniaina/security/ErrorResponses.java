package com.nicky.hariniaina.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** RFC 7807 body for the requests rejected by the filters, matching the controller errors. */
final class ErrorResponses {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final String PROBLEM_JSON = "application/problem+json";

  private ErrorResponses() {}

  static void write(HttpServletResponse response, int status, String title, String detail) {
    response.setStatus(status);
    response.setContentType(PROBLEM_JSON);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("type", "about:blank");
    body.put("title", title);
    body.put("status", status);
    body.put("detail", detail);
    try {
      MAPPER.writeValue(response.getOutputStream(), body);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  static boolean isApi(HttpServletRequest request) {
    return path(request).startsWith("/api/");
  }

  static String path(HttpServletRequest request) {
    String uri = request.getRequestURI();
    String context = request.getContextPath();
    if (context != null && !context.isEmpty() && uri.startsWith(context)) {
      return uri.substring(context.length());
    }
    return uri;
  }
}
