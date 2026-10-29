package com.acme.roombooking.security;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acme.roombooking.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

class AuthorizationIntegrationTest extends ApiTestSupport {
  @Test
  void anonymousInventoryRequestReceivesAProblemAndBasicChallenge() throws Exception {
    mvc.perform(get("/api/rooms"))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string("WWW-Authenticate", containsString("Basic")))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.instance").value("/api/rooms"));
  }

  @Test
  void wrongPasswordDoesNotGrantAccess() throws Exception {
    mvc.perform(get("/api/me").with(httpBasic("alice", "wrong")))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void healthAndOpenApiArePublicButOtherActuatorEndpointsAreNot() throws Exception {
    mvc.perform(get("/actuator/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
    mvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.openapi").exists());
    mvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
  }

  @ParameterizedTest
  @CsvSource({
    "POST,/api/buildings,building",
    "PUT,/api/buildings/1,building",
    "POST,/api/buildings/1/floors,floor",
    "POST,/api/rooms,room",
    "PUT,/api/rooms/1,room"
  })
  void employeeCannotMutateFacilityInventory(String method, String path, String resource)
      throws Exception {
    String body =
        switch (resource) {
          case "building" ->
              """
          {"code":"BA-HQ","name":"Changed","address":"Changed address"}
          """;
          case "floor" ->
              """
          {"level":9,"name":"New floor"}
          """;
          default ->
              """
          {"floorId":1,"code":"BA-DANUBE","name":"Changed","capacity":12,"active":true,"equipment":[]}
          """;
        };
    mvc.perform(
            request(HttpMethod.valueOf(method), path)
                .with(httpBasic("alice", "alice-local-only"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403));
  }

  @Test
  void writesRequireCsrfEvenWhenBasicCredentialsAreValid() throws Exception {
    mvc.perform(
            post("/api/bookings")
                .with(httpBasic("alice", "alice-local-only"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isForbidden())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
  }

  @Test
  void clientCanObtainACsrfTokenAndUseItWithTheSessionCookie() throws Exception {
    var response =
        mvc.perform(get("/api/csrf").with(httpBasic("alice", "alice-local-only")))
            .andExpect(status().isOk())
            .andReturn();
    var token = json.readTree(response.getResponse().getContentAsString());
    var session = (MockHttpSession) response.getRequest().getSession(false);
    mvc.perform(
            post("/api/bookings")
                .session(session)
                .with(httpBasic("alice", "alice-local-only"))
                .header(token.get("headerName").asText(), token.get("token").asText())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {"roomId":1,"title":"Planning","attendees":4,
                 "startsAt":"2026-11-03T08:00:00Z","endsAt":"2026-11-03T09:00:00Z"}
                """))
        .andExpect(status().isCreated());
  }

  @Test
  void oneCsrfTokenRemainsValidAcrossConsecutiveAuthenticatedWrites() throws Exception {
    var response =
        mvc.perform(get("/api/csrf").with(httpBasic("alice", "alice-local-only")))
            .andExpect(status().isOk())
            .andReturn();
    var token = json.readTree(response.getResponse().getContentAsString());
    var session = (MockHttpSession) response.getRequest().getSession(false);
    for (int attempt = 0; attempt < 2; attempt++) {
      mvc.perform(
              post("/api/bookings")
                  .session(session)
                  .with(httpBasic("alice", "alice-local-only"))
                  .header(token.get("headerName").asText(), token.get("token").asText())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{}"))
          .andExpect(status().isBadRequest());
    }
    var created =
        mvc.perform(
                post("/api/bookings")
                    .session(session)
                    .with(httpBasic("alice", "alice-local-only"))
                    .header(token.get("headerName").asText(), token.get("token").asText())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                {"roomId":1,"title":"Corrected request","attendees":4,
                 "startsAt":"2026-11-03T08:00:00Z","endsAt":"2026-11-03T09:00:00Z"}
                """))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String id = json.readTree(created).get("id").asText();
    mvc.perform(
            post("/api/bookings/{id}/cancellation", id)
                .session(session)
                .with(httpBasic("alice", "alice-local-only"))
                .header(token.get("headerName").asText(), token.get("token").asText()))
        .andExpect(status().isOk());
    mvc.perform(get("/api/me").session(session)).andExpect(status().isUnauthorized());
  }

  @Test
  void aCsrfTokenWithoutItsMatchingCookieOrWithATamperedValueDoesNotGrantWriteAccess()
      throws Exception {
    var response =
        mvc.perform(get("/api/csrf").with(httpBasic("alice", "alice-local-only")))
            .andExpect(status().isOk())
            .andReturn();
    var token = json.readTree(response.getResponse().getContentAsString());
    var session = (MockHttpSession) response.getRequest().getSession(false);
    mvc.perform(
            post("/api/bookings")
                .with(httpBasic("alice", "alice-local-only"))
                .header(token.get("headerName").asText(), token.get("token").asText())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/bookings")
                .session(session)
                .with(httpBasic("alice", "alice-local-only"))
                .header(token.get("headerName").asText(), "tampered")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void identityListsOnlyTheAuthenticatedUsersRoles() throws Exception {
    mvc.perform(get("/api/me").with(httpBasic("alice", "alice-local-only")))
        .andExpect(jsonPath("$.username").value("alice"))
        .andExpect(jsonPath("$.authorities[0]").value("ROLE_EMPLOYEE"));
    mvc.perform(get("/api/me").with(httpBasic("facilities", "facilities-local-only")))
        .andExpect(jsonPath("$.authorities.length()").value(2));
  }
}
