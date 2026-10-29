package com.acme.roombooking.security;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Session", description = "Authenticated identity and CSRF token for write requests")
class SessionController {
  @GetMapping("/me")
  @Operation(summary = "Get your identity and granted roles")
  Identity identity(Authentication authentication) {
    return new Identity(
        authentication.getName(),
        authentication.getAuthorities().stream()
            .map(authority -> authority.getAuthority())
            .filter(authority -> authority.startsWith("ROLE_"))
            .sorted()
            .toList());
  }

  @GetMapping("/csrf")
  @Operation(
      summary =
          "Get a CSRF token; preserve the session cookie and send the returned header on writes")
  Token csrf(CsrfToken token) {
    return new Token(token.getHeaderName(), token.getToken());
  }

  record Identity(String username, List<String> authorities) {}

  record Token(String headerName, String token) {}
}
