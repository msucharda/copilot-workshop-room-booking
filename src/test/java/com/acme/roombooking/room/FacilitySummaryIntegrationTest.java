package com.acme.roombooking.room;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acme.roombooking.ApiTestSupport;
import org.junit.jupiter.api.Test;

class FacilitySummaryIntegrationTest extends ApiTestSupport {
  @Test
  void administratorCanSeeInventoryTotalsAndEmployeesCannot() throws Exception {
    mvc.perform(get("/api/facility-summary").with(httpBasic("facilities", "facilities-local-only")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.buildingCount").value(3))
        .andExpect(jsonPath("$.floorCount").value(6))
        .andExpect(jsonPath("$.activeRoomCount").value(12))
        .andExpect(jsonPath("$.inactiveRoomCount").value(1))
        .andExpect(jsonPath("$.buildings[0].code").value("BA-HQ"))
        .andExpect(jsonPath("$.buildings[0].activeRoomCount").value(6));
    mvc.perform(get("/api/facility-summary").with(httpBasic("alice", "alice-local-only")))
        .andExpect(status().isForbidden());
  }
}
