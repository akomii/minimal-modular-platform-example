package org.example.modular.core.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Servlet filter that captures each handled {@code /api/**} request (excluding the SSE streams) into the request-log store for the management UI's live stream.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLogFilter extends OncePerRequestFilter {

  private final RequestLogStore store;

  public RequestLogFilter(RequestLogStore store) {
    this.store = store;
  }

  /**
   * Lets the request proceed, then records its method, path (with query) and response status, skipping the SSE stream endpoints.
   */
  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
    try {
      chain.doFilter(request, response);
    } finally {
      String uri = request.getRequestURI();
      if (uri.startsWith("/api/") && !uri.contains("/stream")) {
        String path = request.getQueryString() == null ? uri : uri + "?" + request.getQueryString();
        store.record(request.getMethod(), path, response.getStatus());
      }
    }
  }
}
