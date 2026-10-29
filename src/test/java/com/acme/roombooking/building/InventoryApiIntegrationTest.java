package com.acme.roombooking.building;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acme.roombooking.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class InventoryApiIntegrationTest extends ApiTestSupport {
  @Test
  void employeeCanBrowseSeededBuildingsFloorsAndActiveRooms() throws Exception {
    mvc.perform(get("/api/buildings").with(httpBasic("alice", "alice-local-only")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(3));
    mvc.perform(get("/api/buildings/1/floors").with(httpBasic("alice", "alice-local-only")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].level").value(0));
    mvc.perform(
            get("/api/rooms")
                .param("buildingId", "1")
                .param("minimumCapacity", "8")
                .with(httpBasic("alice", "alice-local-only")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(3))
        .andExpect(jsonPath("$[*].capacity", everyItem(greaterThanOrEqualTo(8))));
    mvc.perform(get("/api/rooms/7").with(httpBasic("alice", "alice-local-only")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(false));
  }

  @Test
  void facilityAdministratorCanRegisterAndUpdateAnOffice() throws Exception {
    var building =
        mvc.perform(
                post("/api/buildings")
                    .with(httpBasic("facilities", "facilities-local-only"))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                {"code":"TEST-OFF","name":"New office","address":"Station Street 5"}
                """))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    long buildingId = json.readTree(building).get("id").asLong();
    mvc.perform(
            put("/api/buildings/{id}", buildingId)
                .with(httpBasic("facilities", "facilities-local-only"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {"code":"TEST-OFF","name":"Regional office","address":"Station Street 5"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Regional office"));
    var floor =
        mvc.perform(
                post("/api/buildings/{id}/floors", buildingId)
                    .with(httpBasic("facilities", "facilities-local-only"))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                {"level":1,"name":"Engineering"}
                """))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    long floorId = json.readTree(floor).get("id").asLong();
    String roomBody =
        """
        {"floorId":%d,"code":"TEST-ROOM","name":"Planning","capacity":8,
         "active":true,"equipment":["WHITEBOARD","DISPLAY"]}
        """
            .formatted(floorId);
    String room =
        mvc.perform(
                post("/api/rooms")
                    .with(httpBasic("facilities", "facilities-local-only"))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(roomBody))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    long roomId = json.readTree(room).get("id").asLong();
    mvc.perform(
            put("/api/rooms/{id}", roomId)
                .with(httpBasic("facilities", "facilities-local-only"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(roomBody.replace("\"active\":true", "\"active\":false")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(false));
    mvc.perform(get("/api/rooms/{id}", roomId).with(httpBasic("alice", "alice-local-only")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Planning"));
  }

  @Test
  void duplicateCodesAndUnknownResourcesProduceClientErrors() throws Exception {
    mvc.perform(
            post("/api/buildings")
                .with(httpBasic("facilities", "facilities-local-only"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {"code":"BA-HQ","name":"Duplicate","address":"Station Street 5"}
                """))
        .andExpect(status().isConflict());
    mvc.perform(get("/api/buildings/999").with(httpBasic("alice", "alice-local-only")))
        .andExpect(status().isNotFound());
    mvc.perform(
            get("/api/rooms")
                .param("minimumCapacity", "0")
                .with(httpBasic("alice", "alice-local-only")))
        .andExpect(status().isBadRequest());
  }

  @Test
  void bookingPolicyIsDiscoverableWithoutHardCodingLocalTimeAssumptions() throws Exception {
    mvc.perform(get("/api/booking-policy").with(httpBasic("alice", "alice-local-only")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.zone").value("Europe/Bratislava"))
        .andExpect(jsonPath("$.maximumDuration").value("PT4H"));
  }
}
