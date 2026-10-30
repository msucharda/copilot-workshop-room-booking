package com.acme.roombooking.common;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acme.roombooking.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;

class RequestValidationIntegrationTest extends ApiTestSupport {
  @Test
  void mixedValidAndBlankEquipmentIsAClientErrorRatherThanAnUnhandledFailure() throws Exception {
    mvc.perform(
            get("/api/availability")
                .param("startsAt", "2026-11-03T08:00:00Z")
                .param("endsAt", "2026-11-03T09:00:00Z")
                .param("equipment", "DISPLAY", "")
                .with(httpBasic("alice", "alice-local-only")))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
  }

  @ParameterizedTest
  @ValueSource(strings = {"30000000", "2147483647"})
  void bookingPageNumbersCannotOverflowThePersistenceOffset(String page) throws Exception {
    mvc.perform(
            get("/api/bookings")
                .param("page", page)
                .param("size", "100")
                .with(httpBasic("alice", "alice-local-only")))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
  }

  @Test
  void largestSupportedPageNumberRemainsUsable() throws Exception {
    mvc.perform(
            get("/api/bookings")
                .param("page", "21474836")
                .param("size", "100")
                .with(httpBasic("alice", "alice-local-only")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isEmpty());
  }

  @Test
  void bookingAndAvailabilityRejectIntervalsFinerThanTheDatabaseCanStore() throws Exception {
    String start = "2026-11-03T08:00:00.000000100Z";
    String end = "2026-11-03T08:00:00.000000200Z";
    book("alice", 1, start, end)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.type").value("urn:room-booking:problem:unsupported-time-precision"));
    mvc.perform(
            get("/api/availability")
                .param("startsAt", start)
                .param("endsAt", end)
                .with(httpBasic("alice", "alice-local-only")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.type").value("urn:room-booking:problem:unsupported-time-precision"));
  }

  @Test
  void supportsMicrosecondPrecisionInCreateAndReadResponses() throws Exception {
    String start = "2026-11-03T08:00:00.123456Z";
    String end = "2026-11-03T09:00:00.123456Z";
    String created =
        book("alice", 1, start, end)
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String id = json.readTree(created).get("id").asText();
    mvc.perform(get("/api/bookings/{id}", id).with(httpBasic("alice", "alice-local-only")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.startsAt").value(start))
        .andExpect(jsonPath("$.endsAt").value(end));
  }

  @Test
  void duplicateFloorLevelsHaveAConstraintNeutralConflictExplanation() throws Exception {
    mvc.perform(
            post("/api/buildings/1/floors")
                .with(httpBasic("facilities", "facilities-local-only"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {"level":1,"name":"Already registered"}
                """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.type").value("urn:room-booking:problem:duplicate-resource"))
        .andExpect(jsonPath("$.detail").value("The request conflicts with an existing resource"))
        .andExpect(content().string(not(containsString("code already exists"))));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " "})
  void anExplicitlyBlankEquipmentFilterIsInvalid(String equipment) throws Exception {
    mvc.perform(
            get("/api/availability")
                .param("startsAt", "2026-11-03T08:00:00Z")
                .param("endsAt", "2026-11-03T09:00:00Z")
                .param("equipment", equipment)
                .with(httpBasic("alice", "alice-local-only")))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
  }
}
