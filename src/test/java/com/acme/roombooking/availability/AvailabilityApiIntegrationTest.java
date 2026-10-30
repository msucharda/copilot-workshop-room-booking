package com.acme.roombooking.availability;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acme.roombooking.ApiTestSupport;
import org.junit.jupiter.api.Test;

class AvailabilityApiIntegrationTest extends ApiTestSupport {
  @Test
  void freeRoomSearchCombinesLocationSeatsEquipmentAndActiveConflictsWithoutLeakingBookingDetails()
      throws Exception {
    book("alice", 1, "2026-11-03T08:00:00Z", "2026-11-03T10:00:00Z")
        .andExpect(status().isCreated());
    mvc.perform(
            get("/api/availability")
                .param("startsAt", "2026-11-03T08:30:00Z")
                .param("endsAt", "2026-11-03T09:30:00Z")
                .param("buildingId", "1")
                .param("minimumCapacity", "6")
                .param("equipment", "VIDEO_CONFERENCING")
                .with(httpBasic("bob", "bob-local-only")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.rooms[*].code", not(hasItem("BA-DANUBE"))))
        .andExpect(jsonPath("$.rooms[0].code").value("BA-TATRA"))
        .andExpect(jsonPath("$.rooms[1].code").value("BA-BOARD"))
        .andExpect(jsonPath("$.rooms[*].ownerUsername").isEmpty());
  }

  @Test
  void invalidSearchIntervalsAndEquipmentAreRejected() throws Exception {
    mvc.perform(
            get("/api/availability")
                .param("startsAt", "2026-11-03T10:00:00Z")
                .param("endsAt", "2026-11-03T09:00:00Z")
                .with(httpBasic("alice", "alice-local-only")))
        .andExpect(status().isBadRequest());
    mvc.perform(
            get("/api/availability")
                .param("startsAt", "2026-11-03T08:00:00Z")
                .param("endsAt", "2026-11-03T09:00:00Z")
                .param("equipment", "UNKNOWN")
                .with(httpBasic("alice", "alice-local-only")))
        .andExpect(status().isBadRequest());
  }
}
