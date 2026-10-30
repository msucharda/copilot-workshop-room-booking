package com.acme.roombooking;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(classes = RoomBookingApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ApiTestSupport.FixedTime.class)
@Transactional
public abstract class ApiTestSupport {
  @Autowired protected MockMvc mvc;
  @Autowired protected ObjectMapper json;

  protected ResultActions book(String owner, int room, String start, String end) throws Exception {
    return mvc.perform(
        post("/api/bookings")
            .with(httpBasic(owner, owner + "-local-only"))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
            {"roomId":%d,"title":"Delivery planning","attendees":4,"startsAt":"%s","endsAt":"%s"}
            """
                    .formatted(room, start, end)));
  }

  @TestConfiguration
  static class FixedTime {
    @Bean
    @Primary
    Clock fixedClock() {
      return Clock.fixed(Instant.parse("2026-11-02T07:00:00Z"), ZoneOffset.UTC);
    }
  }
}
