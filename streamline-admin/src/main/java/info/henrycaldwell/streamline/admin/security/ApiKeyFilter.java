package info.henrycaldwell.streamline.admin.security;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ApiKeyFilter extends OncePerRequestFilter {

  private static final String BEARER_PREFIX = "Bearer ";

  private static final List<String> WHITELIST = List.of(
      "/health/**",
      "/info",
      "/docs",
      "/docs/**",
      "/api-docs",
      "/api-docs/**",
      "/swagger-ui/**",
      "/swagger-ui.html");

  private static final AntPathMatcher MATCHER = new AntPathMatcher();

  private final ObjectMapper mapper;

  private final String apiKey;

  public ApiKeyFilter(@Value("${streamline.security.api-key}") String apiKey, ObjectMapper mapper) {
    this.apiKey = apiKey;
    this.mapper = mapper;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String path = request.getRequestURI();

    for (String pattern : WHITELIST) {
      if (MATCHER.match(pattern, path)) {
        chain.doFilter(request, response);

        return;
      }
    }

    String header = request.getHeader("Authorization");
    if (header == null) {
      writeUnauthorized(request, response, "Missing Authorization header");

      return;
    }

    if (!header.startsWith(BEARER_PREFIX)) {
      writeUnauthorized(request, response, "Incorrect Authorization header scheme (expected Bearer)");

      return;
    }

    String key = header.substring(BEARER_PREFIX.length());
    if (apiKey.isBlank() || !key.equals(apiKey)) {
      writeUnauthorized(request, response, "Invalid API key");

      return;
    }

    chain.doFilter(request, response);
  }

  private void writeUnauthorized(HttpServletRequest request, HttpServletResponse response, String message)
      throws IOException {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, message);
    problem.setInstance(URI.create(request.getRequestURI()));

    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType("application/problem+json");
    mapper.writeValue(response.getWriter(), problem);
  }

}
