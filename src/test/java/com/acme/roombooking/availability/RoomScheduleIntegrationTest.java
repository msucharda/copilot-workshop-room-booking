package com.acme.roombooking.availability;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acme.roombooking.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class RoomScheduleIntegrationTest extends ApiTestSupport {
  @Test
  void rejectsDatesWhoseNextDayCannotBeRepresented() throws Exception {
    mvc.perform(
            get("/api/rooms/1/schedule")
                .param("date", "+999999999-12-31")
                .with(httpBasic("alice", "alice-local-only")))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.type").value("urn:room-booking:problem:unsupported-schedule-date"));
  }

  @Test
  void colleagueCanSeeBusyTimesButNotPrivateMeetingInformation() throws Exception {
    book("alice", 1, "2026-11-03T08:00:00Z", "2026-11-03T09:00:00Z")
        .andExpect(status().isCreated());
    mvc.perform(
            get("/api/rooms/1/schedule")
                .param("date", "2026-11-03")
                .with(httpBasic("bob", "bob-local-only")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.zone").value("Europe/Bratislava"))
        .andExpect(jsonPath("$.workingDay").value(true))
        .andExpect(jsonPath("$.busyIntervals[0].startsAt").value("2026-11-03T08:00:00Z"))
        .andExpect(content().string(not(containsString("alice"))))
        .andExpect(content().string(not(containsString("Delivery planning"))));
  }

  @Test
  void scheduleIdentifiesWeekendsAndValidatesRoomAndDate() throws Exception {
    mvc.perform(
            get("/api/rooms/1/schedule")
                .param("date", "2026-11-07")
                .with(httpBasic("alice", "alice-local-only")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.workingDay").value(false))
        .andExpect(jsonPath("$.busyIntervals").isEmpty());
    mvc.perform(
            get("/api/rooms/999/schedule")
                .param("date", "2026-11-03")
                .with(httpBasic("alice", "alice-local-only")))
        .andExpect(status().isNotFound());
    mvc.perform(
            get("/api/rooms/1/schedule")
                .param("date", "invalid")
                .with(httpBasic("alice", "alice-local-only")))
        .andExpect(status().isBadRequest());
  }
}
