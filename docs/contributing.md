# Contributing: Java Service Development Guide

How we build, test, log and ship Java services. Read it before your first pull request.

## 1. Stack

| Concern | Choice |
|---|---|
| Language | Java 25 LTS *(corporate enforced practice)* |
| Build | Maven, run in a pinned container; every plugin version pinned in `pom.xml` *(corporate enforced practice)* |
| Framework | Spring Boot, current supported minor, managed through the Spring Boot parent or BOM |
| Web | Spring Web MVC on virtual threads (`spring.threads.virtual.enabled: true`). Use WebFlux only for genuinely streaming workloads. |
| Validation | Jakarta Bean Validation (Hibernate Validator) |
| JSON | Jackson (Spring Boot default) |
| Persistence | Spring Data JPA (Hibernate) or `JdbcClient` for simple SQL; PostgreSQL |
| Migrations | Flyway |
| Mapping | Java records plus MapStruct for non-trivial mapping |
| HTTP clients | Spring `RestClient` or declarative HTTP interfaces, with explicit timeouts |
| API docs | springdoc-openapi, emitting OpenAPI 3.1 |
| Errors | RFC 9457 Problem Details (`ProblemDetail`) |
| Logging | SLF4J API, Logback, logstash-logback-encoder for JSON |
| Metrics | Micrometer with the Prometheus registry, Spring Boot Actuator |
| Tracing | OpenTelemetry Java agent, injected by the platform *(corporate enforced practice)* |
| Nullness | JSpecify annotations (`@NullMarked` packages) |
| Tests | JUnit Jupiter, AssertJ, Mockito (boundaries only), Testcontainers, WireMock, ArchUnit |
| Formatting | Spotless with google-java-format |
| Static analysis | Error Prone or SpotBugs in the build |

New libraries need a reason in the PR description: what it replaces, its maintenance status and its license.

**Avoid:**
- Lombok in new code (use records); `@Data` on JPA entities is never allowed.
- Hand-rolled HTTP clients, `java.util.Date` and `Calendar`, Joda-Time.
- Commons-lang where the JDK already does the job.
- Client-side retry or circuit-breaker libraries where the mesh or gateway already does it.

## 2. Local development

- All builds and tests run in a container, not on the host. *(corporate enforced practice)*

  ```bash
  docker run --rm --memory 3g --cpus 2 \
    --user "$(id -u):$(id -g)" -e HOME=/tmp \
    -v "$PWD":/w -w /w \
    <cache-registry>/maven:<maven-version>-eclipse-temurin-25 \
    mvn -B -Dmaven.repo.local=/w/.m2/repository verify
  ```

- Add `.m2/` to `.gitignore`. The local repository lives in the workspace so the container can run as your user.
- Run dependencies such as PostgreSQL or Kafka with `docker compose` or Testcontainers. Never against shared environments.
- `mvn -B verify` must pass locally before you push: format check, compile, unit and integration tests, static analysis.

## 3. Project layout

Package by feature, then by layer inside the feature:

```text
src/main/java/com/example/billing/
  BillingApplication.java
  invoice/
    api/            controllers, request and response records, OpenAPI annotations
    domain/         aggregates, value objects, domain services (no Spring web types)
    persistence/    repositories, JPA entities or JDBC code
    client/         outbound HTTP clients for other services
  shared/
    error/          ProblemDetail mapping, exception types
    logging/        correlation filter, MDC helpers
    config/         @ConfigurationProperties records
src/main/resources/
  application.yaml
  logback-spring.xml
  db/migration/     V1__create_invoice.sql, V2__...
src/test/java/...   mirrors main; integration tests end in *IT
helm/<service>/     Helm chart for the service
```

## 4. Code

### 4.1 Style

- Formatting is not discussed in review: Spotless (google-java-format) runs in the build and fails it on drift.
- Prefer immutable types: records for DTOs, events and value objects; `final` fields; constructor injection only (no field injection).
- `Optional` is for return values only, never for fields or parameters.
- Packages are `@NullMarked`; nullable values are annotated `@Nullable` explicitly.
- Time uses `java.time` only, stored and logged in UTC, exchanged as ISO-8601. Money is `BigDecimal` with an explicit scale, never `double`.
- Identifiers are UUIDs. Never expose database sequence ids in APIs.
- No `System.out`, no `printStackTrace()`, no swallowed exceptions.

### 4.2 Configuration

- Configuration is bound to validated `@ConfigurationProperties` records (`@Validated`). Fail at startup when a value is missing or invalid.
- `application.yaml` holds defaults only. Environment-specific values come from environment variables or mounted config set by the Helm values.
- Secrets come from environment variables populated by External Secrets. Never put them in `application.yaml`, the image or the repo. *(corporate enforced practice)*
- No Spring profile per environment for business behavior. Use feature flags or configuration values instead.

### 4.3 Errors

- Throw domain exceptions in `domain`. Map them in one `@RestControllerAdvice` to `ProblemDetail`:

  ```json
  {
    "type": "https://docs.example.com/problems/invoice-not-found",
    "title": "Invoice not found",
    "status": 404,
    "detail": "Invoice 6f1c2e0a-7b2d-4e1f-9a3b-2c8d9e0f1a2b does not exist",
    "instance": "/api/v1/invoices/6f1c2e0a-7b2d-4e1f-9a3b-2c8d9e0f1a2b",
    "correlation_id": "c-1029"
  }
  ```

- Never return stack traces, SQL or internal class names to clients.
- Log an exception once, at the boundary that handles it, with its stack trace. Don't log and rethrow.

### 4.4 Persistence

- Schema changes **MUST** be Flyway migrations: versioned, forward-only, never edited after merge.
- Use expand and contract for breaking changes. Add the column, deploy, backfill, switch reads, then drop it in a later release. A release must run against both the old and the new schema.
- `@Transactional` sits on service methods, with `readOnly = true` for queries. Never hold a transaction open across a remote call.
- No N+1 queries: use fetch joins, `@EntityGraph` or projections. Enable SQL logging only locally.
- Every list endpoint is paginated, with a server-side maximum page size.
- Keep the HikariCP pool small and sized to the database limit divided by replicas; start at 10, not 100.
- All SQL is parameterized. String-built SQL is rejected in review.

### 4.5 Outbound calls

- Every outbound call sets connect and read timeouts (default connect 2 s, read 5 s; document any exception).
- Retries, circuit breaking and mTLS belong to the gateway or service mesh, not to service code. *(corporate enforced practice)*
- `POST` endpoints that clients may retry accept an `Idempotency-Key` header and store the result per key.
- Treat every response from another service as untrusted input and validate it.

### 4.6 Concurrency

- Use virtual threads for request handling and blocking I/O. Don't build custom thread pools without a measured need.
- Avoid large `ThreadLocal` state; MDC is the one sanctioned use.
- Scheduled jobs **MUST** be safe with several replicas running: use a lock (ShedLock) or move the job to a Kubernetes CronJob.

---

## 5. APIs

- REST or gRPC only. REST resources are plural nouns:
  - `GET /patients`
  - `POST /patients`
  - `PUT|PATCH|DELETE /patients/{id}`
  - actions as `POST /patients/{id}/activate`
  - search as `POST /patients/search`

  *(corporate enforced practice)*
- Path versioning (`/api/v1/...`). Support at most the last 3 versions and keep changes backward compatible. *(corporate enforced practice: version window)*
- Status codes: `200`, `201`, `202`, `400`, `401`, `403`, `404`, `405`, `409`, `500` and `501`; `429` comes from the gateway only. *(corporate enforced practice)*
- The OpenAPI 3.1 document is generated by springdoc. Every operation has a summary, request and response examples, and every status code it can return. *(corporate enforced practice)*
- Validate request bodies with `@Valid` and constraint annotations on the request records. Reject unknown JSON fields for commands.
- The gateway validates tokens; services read claims for authorization and data filtering. No custom authorization schemes. *(corporate enforced practice)*

## 6. Logging

### 6.1 Format *(corporate enforced practice)*

JSON to stdout, one object per line, with these field names:

```json
{"time":"2026-10-30T12:00:00.000Z","level":"INFO","logger_name":"com.example.billing.invoice.InvoiceService","thread_name":"tomcat-handler-42","message":"Invoice created","trace_id":"4bf92f3577b34da6a3ce929d0e0e4736","span_id":"00f067aa0ba902b7","correlation_id":"c-1029","invoice_id":"6f1c2e0a-7b2d-4e1f-9a3b-2c8d9e0f1a2b"}
```

The pod **MUST** carry the label `logging.example.io/parser: json`.

### 6.2 Configuration (`logback-spring.xml`)

```xml
<configuration>
  <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
      <fieldNames>
        <timestamp>time</timestamp>
        <version>[ignore]</version>
        <levelValue>[ignore]</levelValue>
      </fieldNames>
    </encoder>
  </appender>
  <root level="INFO">
    <appender-ref ref="JSON"/>
  </root>
</configuration>
```

- The OpenTelemetry agent puts `trace_id` and `span_id` into the MDC. A servlet filter reads or creates `correlation_id` from the `X-Correlation-Id` header, puts it in the MDC, and returns it in the response.

### 6.3 Rules

| Level | Use for |
|---|---|
| `ERROR` | Something failed and a human must act. Includes the exception. |
| `WARN` | Degraded but handled: fallback used, retry exhausted upstream, validation of external data failed. |
| `INFO` | Lifecycle (start, stop, config summary without secrets) and business events (invoice created, payment captured). |
| `DEBUG` | Diagnostic detail. Off in production; enable per logger, temporarily. |

- Use parameterized messages (`log.info("Invoice {} created", id)`), never string concatenation.
- Add business ids as structured fields (`kv("invoice_id", id)` or MDC), so they are queryable.
- **MUST NOT** log secrets, tokens, passwords, full request or response bodies, or personal or health data. Log ids, never payloads. *(corporate enforced practice)*
- One event, one log line. No multi-line messages other than the stack trace field.

## 7. Metrics, tracing and health

- Expose Actuator on a separate management port, never through the ingress:

  ```yaml
  server:
    shutdown: graceful
  spring:
    threads:
      virtual:
        enabled: true
    lifecycle:
      timeout-per-shutdown-phase: 20s
  management:
    server:
      port: 8081
    endpoints:
      web:
        exposure:
          include: health,info,prometheus
    endpoint:
      health:
        probes:
          enabled: true
    metrics:
      distribution:
        percentiles-histogram:
          http.server.requests: true
  ```

- Probes: liveness `/actuator/health/liveness`, readiness `/actuator/health/readiness`, and a startup probe with enough headroom for JVM start. Readiness includes the database; liveness never checks dependencies.
- **Custom metrics:**
  - Micrometer meters named `<domain>.<thing>.<unit>`, exported as `<domain>_<thing>_<unit>`. Counters end up as `_total`, timers as `_seconds`.
  - Tags have bounded values only: no ids, no raw paths, no exception messages. *(corporate enforced practice: bounded labels)*
- **Tracing:**
  - Comes from the OpenTelemetry agent that the platform injects. *(corporate enforced practice)*
  - Don't add the OpenTelemetry SDK unless you need custom spans the agent can't produce; then add spans around business operations, not every method.
- **Alerts:** every new endpoint or job gets at least an error-rate and a latency alert as a `PrometheusRule`, with `severity` and a `runbook_url`. *(corporate enforced practice)*

## 8. Testing

- Test first: write the failing test, make it pass, refactor. *(corporate enforced practice)*

| Layer | Tooling | Scope |
|---|---|---|
| Unit | JUnit Jupiter, AssertJ | Domain logic, no Spring context, milliseconds per test |
| Slice | `@WebMvcTest`, `@DataJpaTest`, `@JsonTest` | One layer with its Spring wiring |
| Integration (`*IT`) | Testcontainers (PostgreSQL, Kafka), WireMock for other services | Real dependencies in containers, full request path *(corporate enforced practice)* |
| Architecture | ArchUnit | Layer and package rules from section 3 |
| Contract | Generated OpenAPI checked into the repo or diffed in CI | No unreviewed API changes |

- Mock only at system boundaries (other services, clock, randomness), never your own classes. *(corporate enforced practice)*
- Tests are deterministic: inject a `Clock`, don't sleep, no shared mutable state, no order dependence.
- Test names describe behavior: `@DisplayName("rejects an invoice without line items")`.
- Every bug fix ships with a regression test that fails without the fix.
- The coverage report (JaCoCo) runs in CI. Coverage must not drop in a PR; it is a signal, not a target.

## 9. Build and dependencies

- **Plugin and build rules:**
  - Pin every plugin and dependency version. *(corporate enforced practice)*
  - Import BOMs instead of repeating versions.
  - No `SNAPSHOT` dependencies on `main`.
  - Maven Enforcer requires the Java and Maven versions, dependency convergence and banned duplicate classes.
  - Reproducible builds: set `project.build.outputTimestamp`.
- Renovate opens dependency PRs: minor and patch updates are grouped, majors come one per PR. *(corporate enforced practice)*
- Before release the image is scanned for CVEs, gets an SBOM, and is signed. *(corporate enforced practice)*

## 10. Container image

```dockerfile
FROM <cache-registry>/maven:<maven-version>-eclipse-temurin-25 AS build
WORKDIR /src
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests \
 && java -Djarmode=tools -jar target/*.jar extract --layers --launcher --destination /app

FROM <cache-registry>/eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/dependencies/ ./
COPY --from=build /app/spring-boot-loader/ ./
COPY --from=build /app/snapshot-dependencies/ ./
COPY --from=build /app/application/ ./
USER 10001
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-XX:+ExitOnOutOfMemoryError", "org.springframework.boot.loader.launch.JarLauncher"]
```

- Base images come from the internal cache registry and are pinned to a tag plus digest. *(corporate enforced practice)*
- The image runs as non-root with a read-only root filesystem; `/tmp` is an `emptyDir`. *(corporate enforced practice)*
- No `-Xmx` in the image: heap follows the container limit through `MaxRAMPercentage`. The memory request equals the memory limit. *(corporate enforced practice: memory request equals limit)*
- Tests run in CI before the image build, which is why the image build skips them.

## 11. Git and pull requests

- Branch from `main` as `feature/<short-description>`. Keep branches short-lived; one task per branch. *(corporate enforced practice)*
- Commits follow Conventional Commits (`feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `chore:`). Add `+major`, `+minor` or `+patch` when the version bump must differ from the default. *(corporate enforced practice)*
- A PR is small and does one thing. The description covers what changed, why, how it was tested, and any migration or config change.
- **Merge requirements:** *(corporate enforced practice)*
  - 1 approval from someone other than the author;
  - all threads resolved;
  - green checks;
  - merge or squash only;
  - company email on every commit.
- Never commit secrets, generated code, `target/` or IDE files. Secret scanning runs on every push. *(corporate enforced practice)*

### PR checklist

- [ ] `mvn -B verify` passes in the container
- [ ] Tests added or updated (unit, plus an integration test for new endpoints or queries)
- [ ] Flyway migration is backward compatible (expand and contract)
- [ ] OpenAPI updated, with examples and every status code
- [ ] Logs are JSON, use the standard fields, and contain no personal data or secrets
- [ ] Metrics have bounded tags; new endpoints have alerts with a `runbook_url`
- [ ] Config added to Helm values for every environment; secrets through External Secrets
- [ ] README updated if run, build or deploy steps changed