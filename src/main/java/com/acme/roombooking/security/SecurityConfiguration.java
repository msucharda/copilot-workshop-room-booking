package com.acme.roombooking.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableMethodSecurity
class SecurityConfiguration {
  private static final Logger LOG = LoggerFactory.getLogger(SecurityConfiguration.class);

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  InMemoryUserDetailsManager developmentUserDetails(
      DevelopmentUsers configuration, PasswordEncoder encoder) {
    LOG.warn(
        "Development-only HTTP Basic accounts are enabled. Do not expose this service publicly.");
    var names = new HashSet<String>();
    var users =
        configuration.users().stream()
            .map(
                user -> {
                  if (!names.add(user.username())) {
                    throw new IllegalStateException(
                        "Duplicate development username: " + user.username());
                  }
                  String[] roles =
                      user.role().equals("FACILITY_ADMIN")
                          ? new String[] {"EMPLOYEE", "FACILITY_ADMIN"}
                          : new String[] {"EMPLOYEE"};
                  return User.withUsername(user.username())
                      .password(encoder.encode(user.password()))
                      .roles(roles)
                      .build();
                })
            .toList();
    return new InMemoryUserDetailsManager(users);
  }

  @Bean
  SecurityFilterChain apiSecurity(HttpSecurity http, ObjectMapper mapper) throws Exception {
    return http.sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        // Basic reauthenticates every request; the session holds only the CSRF token.
        .csrf(csrf -> csrf.sessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy()))
        .requestCache(cache -> cache.disable())
        .httpBasic(
            basic ->
                basic.authenticationEntryPoint(
                    (request, response, exception) -> {
                      response.setHeader(
                          HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"room-booking\"");
                      writeProblem(
                          mapper,
                          request,
                          response,
                          HttpStatus.UNAUTHORIZED,
                          "Authentication is required");
                    }))
        .exceptionHandling(
            errors ->
                errors.accessDeniedHandler(
                    (request, response, exception) ->
                        writeProblem(
                            mapper,
                            request,
                            response,
                            HttpStatus.FORBIDDEN,
                            "You cannot perform this action")))
        .authorizeHttpRequests(
            authorize ->
                authorize
                    .requestMatchers(
                        HttpMethod.GET,
                        "/actuator/health",
                        "/actuator/health/**",
                        "/v3/api-docs",
                        "/v3/api-docs/**",
                        "/swagger-ui.html",
                        "/swagger-ui/**")
                    .permitAll()
                    .requestMatchers("/api/**")
                    .hasRole("EMPLOYEE")
                    .anyRequest()
                    .denyAll())
        .cors(Customizer.withDefaults())
        .build();
  }

  private void writeProblem(
      ObjectMapper mapper,
      HttpServletRequest request,
      HttpServletResponse response,
      HttpStatus status,
      String message)
      throws IOException {
    ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, message);
    detail.setType(
        URI.create(
            "urn:room-booking:problem:"
                + (status == HttpStatus.UNAUTHORIZED ? "unauthorized" : "forbidden")));
    detail.setInstance(URI.create(request.getRequestURI()));
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    mapper.writeValue(response.getOutputStream(), detail);
  }
}
