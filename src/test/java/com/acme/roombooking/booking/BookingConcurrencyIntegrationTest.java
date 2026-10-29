package com.acme.roombooking.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acme.roombooking.ApiTestSupport;
import com.acme.roombooking.RoomBookingApplication;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
    classes = RoomBookingApplication.class,
    properties = "spring.datasource.url=jdbc:h2:mem:booking-race;DB_CLOSE_DELAY=-1")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class BookingConcurrencyIntegrationTest extends ApiTestSupport {
  @Test
  void exactlyOneOfTwoSimultaneousOverlappingRequestsCanReserveTheRoom() throws Exception {
    var ready = new CountDownLatch(2);
    var start = new CountDownLatch(1);
    List<Attempt> attempts = new ArrayList<>();
    try (var executor = Executors.newFixedThreadPool(2)) {
      var requests =
          List.of("alice", "bob").stream()
              .map(
                  owner ->
                      executor.submit(
                          () -> {
                            ready.countDown();
                            if (!start.await(20, TimeUnit.SECONDS)) {
                              throw new IllegalStateException("Race start barrier timed out");
                            }
                            var response =
                                book(owner, 13, "2026-11-06T08:00:00Z", "2026-11-06T09:00:00Z")
                                    .andReturn()
                                    .getResponse();
                            String id =
                                response.getStatus() == 201
                                    ? json.readTree(response.getContentAsString())
                                        .get("id")
                                        .asText()
                                    : null;
                            return new Attempt(owner, response.getStatus(), id);
                          }))
              .toList();
      assertThat(ready.await(20, TimeUnit.SECONDS)).isTrue();
      start.countDown();
      for (var request : requests) {
        attempts.add(request.get(30, TimeUnit.SECONDS));
      }
      assertThat(attempts).extracting(Attempt::status).containsExactlyInAnyOrder(201, 409);
    } finally {
      start.countDown();
      for (Attempt attempt : attempts) {
        if (attempt.id() != null) {
          mvc.perform(
                  post("/api/bookings/{id}/cancellation", attempt.id())
                      .with(httpBasic(attempt.owner(), attempt.owner() + "-local-only"))
                      .with(csrf()))
              .andExpect(status().isOk());
        }
      }
    }
  }

  private record Attempt(String owner, int status, String id) {}
}
