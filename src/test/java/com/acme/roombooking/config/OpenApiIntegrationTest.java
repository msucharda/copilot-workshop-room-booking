package com.acme.roombooking.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acme.roombooking.ApiTestSupport;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OpenApiIntegrationTest extends ApiTestSupport {
  @Test
  void tokenAcquisitionAndReadOnlyOperationsRequireBasicAuthenticationOnly() throws Exception {
    String body =
        mvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    var document = json.readTree(body);
    var global = document.get("security");
    assertThat(global.size()).isEqualTo(1);
    assertThat(global.get(0).has("basicAuth")).isTrue();
    assertThat(global.get(0).has("csrfToken")).isFalse();
    assertThat(document.get("paths").get("/api/csrf").get("get").has("security")).isFalse();
  }

  @Test
  void everyWriteDocumentsBasicAndCsrfTogetherRatherThanAsAlternatives() throws Exception {
    String body =
        mvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    var paths = json.readTree(body).get("paths");
    var writes =
        Map.of(
            "/api/buildings", "post",
            "/api/buildings/{id}", "put",
            "/api/buildings/{id}/floors", "post",
            "/api/rooms", "post",
            "/api/rooms/{id}", "put",
            "/api/bookings", "post",
            "/api/bookings/{id}", "patch",
            "/api/bookings/{id}/cancellation", "post");
    writes.forEach(
        (path, method) -> {
          var security = paths.get(path).get(method).get("security");
          assertThat(security).as("%s %s security", method, path).isNotNull();
          assertThat(security.size()).isEqualTo(1);
          assertThat(security.get(0).size()).isEqualTo(2);
          assertThat(security.get(0).has("basicAuth")).isTrue();
          assertThat(security.get(0).has("csrfToken")).isTrue();
        });
  }
}
