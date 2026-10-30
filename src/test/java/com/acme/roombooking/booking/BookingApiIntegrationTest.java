package com.acme.roombooking.booking;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acme.roombooking.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;

class BookingApiIntegrationTest extends ApiTestSupport {
  @Test
  void employeeCreatesAConfirmedBookingOwnedByTheirAuthenticatedIdentity() throws Exception {
    book("alice", 1, "2026-11-03T08:00:00Z", "2026-11-03T09:00:00Z")
        .andExpect(status().isCreated())
        .andExpect(header().exists("Location"))
        .andExpect(jsonPath("$.ownerUsername").value("alice"))
        .andExpect(jsonPath("$.status").value("CONFIRMED"))
        .andExpect(jsonPath("$.room.code").value("BA-DANUBE"));
  }

  @Test
  void realOverlapIsRejectedButAnotherRoomIsIndependent() throws Exception {
    book("alice", 1, "2026-11-03T08:00:00Z", "2026-11-03T10:00:00Z")
        .andExpect(status().isCreated());
    book("bob", 1, "2026-11-03T09:00:00Z", "2026-11-03T11:00:00Z")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.type").value("urn:room-booking:problem:booking-conflict"));
    book("bob", 3, "2026-11-03T09:00:00Z", "2026-11-03T11:00:00Z").andExpect(status().isCreated());
  }

  @Test
  void ownerCanCancelAndTheRoomBecomesBookableAgain() throws Exception {
    String id = createAliceBooking();
    mvc.perform(
            post("/api/bookings/{id}/cancellation", id)
                .with(httpBasic("alice", "alice-local-only"))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"))
        .andExpect(jsonPath("$.cancelledAt").value("2026-11-02T07:00:00Z"));
    mvc.perform(
            post("/api/bookings/{id}/cancellation", id)
                .with(httpBasic("alice", "alice-local-only"))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"));
    book("bob", 1, "2026-11-03T08:00:00Z", "2026-11-03T09:00:00Z").andExpect(status().isCreated());
  }

  @ParameterizedTest
  @ValueSource(strings = {"bob", "facilities"})
  void anotherUserCannotReadOrCancelABookingEvenWithAnAdministratorRole(String user)
      throws Exception {
    String id = createAliceBooking();
    mvc.perform(get("/api/bookings/{id}", id).with(httpBasic(user, user + "-local-only")))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/bookings/{id}/cancellation", id)
                .with(httpBasic(user, user + "-local-only"))
                .with(csrf()))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/bookings/{id}", id).with(httpBasic("alice", "alice-local-only")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CONFIRMED"));
  }

  @Test
  void listingIsPrivatePaginatedAndFilterableByStatus() throws Exception {
    createAliceBooking();
    book("bob", 3, "2026-11-03T08:00:00Z", "2026-11-03T09:00:00Z").andExpect(status().isCreated());
    mvc.perform(
            get("/api/bookings").param("size", "1").with(httpBasic("alice", "alice-local-only")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.items[0].ownerUsername").value("alice"));
    mvc.perform(
            get("/api/bookings")
                .param("status", "CANCELLED")
                .with(httpBasic("alice", "alice-local-only")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(0));
  }

  @Test
  void invalidFieldsReturnProblemDetailsWithFieldViolations() throws Exception {
    mvc.perform(
            post("/api/bookings")
                .with(httpBasic("alice", "alice-local-only"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {"roomId":0,"title":" ","attendees":0,"startsAt":null,"endsAt":null}
                """))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.violations").isArray());
  }

  @Test
  void inactiveAndUndersizedRoomsCannotBeBooked() throws Exception {
    book("alice", 7, "2026-11-03T08:00:00Z", "2026-11-03T09:00:00Z")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.type").value("urn:room-booking:problem:room-unavailable"));
    book("alice", 5, "2026-11-03T08:00:00Z", "2026-11-03T09:00:00Z")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.type").value("urn:room-booking:problem:capacity-exceeded"));
  }

  @Test
  void missingRoomAndInvalidIdentifiersProduceClientErrors() throws Exception {
    book("alice", 999, "2026-11-03T08:00:00Z", "2026-11-03T09:00:00Z")
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/bookings/not-a-uuid").with(httpBasic("alice", "alice-local-only")))
        .andExpect(status().isBadRequest());
    mvc.perform(
            get("/api/bookings").param("size", "101").with(httpBasic("alice", "alice-local-only")))
        .andExpect(status().isBadRequest());
  }

  private String createAliceBooking() throws Exception {
    String body =
        book("alice", 1, "2026-11-03T08:00:00Z", "2026-11-03T09:00:00Z")
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return json.readTree(body).get("id").asText();
  }

  @Test
  void ownerCanEditMeetingDetailsButCannotExceedRoomCapacity() throws Exception {
    String id = createAliceBooking();
    mvc.perform(
            patch("/api/bookings/{id}", id)
                .with(httpBasic("alice", "alice-local-only"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {"title":"Architecture discussion","attendees":8}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Architecture discussion"))
        .andExpect(jsonPath("$.attendees").value(8));
    mvc.perform(
            patch("/api/bookings/{id}", id)
                .with(httpBasic("alice", "alice-local-only"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                {"title":"Architecture discussion","attendees":13}
                """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void meetingDetailsCannotBeEditedByAnotherUserOrAfterCancellation() throws Exception {
    String id = createAliceBooking();
    String body =
        """
        {"title":"Changed","attendees":4}
        """;
    mvc.perform(
            patch("/api/bookings/{id}", id)
                .with(httpBasic("bob", "bob-local-only"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/bookings/{id}/cancellation", id)
                .with(httpBasic("alice", "alice-local-only"))
                .with(csrf()))
        .andExpect(status().isOk());
    mvc.perform(
            patch("/api/bookings/{id}", id)
                .with(httpBasic("alice", "alice-local-only"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isConflict());
  }
}
