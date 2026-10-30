# Engineering Standards: Developer Practice, Naming, Platform and Monitoring

## 1. Developer best practices

### 1.1 Git

- Short-lived branches from `main`; one task per branch; rebase or merge `main` often. *(corporate enforced practice)*
- Atomic commits with Conventional Commits 1.0.0 messages (`feat:`, `fix:`, `docs:`, `chore:`, ...). *(corporate enforced practice)*
- SemVer is calculated from Git history. Use `+major`, `+minor`, `+patch` or `+none` in a commit message to steer the increment. *(corporate enforced practice)*
- Never rewrite shared history. Never commit generated artifacts, build output or local environment files.

### 1.2 Pull requests and review

- Small PRs with one concern each. The description says what changed, why, and how it was tested.
- Reviewers read the tests first, then the code. *(corporate enforced practice)*
- Self-review the diff before requesting review. Leave the code cleaner than you found it, but keep refactors in their own PR.
- Merge requirements are in section 6.3. *(corporate enforced practice)*

### 1.3 Testing

- Test first: red, green, refactor, in thin vertical slices. *(corporate enforced practice)*
- Mock only at system boundaries (network, clock, randomness); use real dependencies in containers for integration tests. *(corporate enforced practice)*
- Tests are deterministic: no sleeps, no shared state between tests, no reliance on test order.
- Every bug fix comes with a regression test that fails without the fix.

### 1.4 Dependencies

- Lockfiles (`pnpm-lock.yaml`, `uv.lock`) are committed and installs are frozen. *(corporate enforced practice)*
- Every repo has a Renovate config. Grouped minor and patch updates come weekly, majors are reviewed on their own, and security updates skip the schedule. *(corporate enforced practice)*
- Prefer the standard library and well-maintained packages. Every new dependency needs a reason in the PR description.

### 1.5 Security

- Secret scanning (gitleaks) runs in pre-commit and in CI on every push. *(corporate enforced practice)*
- Images are scanned for CVEs and misconfiguration before release, get an SBOM, and are signed keylessly. Deploy by digest. *(corporate enforced practice)*
- Containers run as non-root with a read-only root filesystem and all capabilities dropped. *(corporate enforced practice)*
- CI tokens get least privilege: read-only by default, write only per job. Third-party actions are pinned to a commit SHA. *(corporate enforced practice)*
- Validate all input at the boundary and encode all output. No secrets, tokens or personal data in logs. *(corporate enforced practice: logs)*

### 1.6 Configuration and errors

- Configuration comes from environment variables and mounted config, never from values hard-coded per environment.
- Fail fast on missing configuration at startup, with a clear message.
- Errors carry context (wrap, don't swallow). APIs return the documented status codes (section 8).
- Structured JSON logging with trace correlation (section 7.3). *(corporate enforced practice)*

### 1.7 Documentation

- Every repo has a README covering purpose, how to run and test it in the sandbox, how it deploys, and its owners. *(corporate enforced practice)*
- Architecture decisions are written down (an ADR, or a technical improvement proposal for platform changes). *(corporate enforced practice)*

---

## 2. Environments *(corporate enforced practice)*

| Context | Values |
|---|---|
| Cloud resource names and tags | `dev`, `tst`, `uat`, `sta`, `prd`, `sbx` |
| GitOps folders and deployment names | `dev`, `tst`, `uat`, `prod` |

Use `prd` in cloud resource names and tags. Use `prod` in GitOps paths and deployment names.

| Cluster role | Purpose |
|---|---|
| `apps-nonprod` | Shared application runtime for dev, tst and uat |
| `apps-prod` | Shared application runtime for production |
| `platform` | GitOps controller, promotion engine, CI runners, observability stack |

The cluster role names are illustrative.

---

## 3. Naming *(corporate enforced practice)*

### 3.1 Source control

| Object | Pattern | Example |
|---|---|---|
| Organization | `<prefix>-<application>` (max 39 characters) | `acme-billing` |
| Backend service repo | `<feature>-service` | `auth-service` |
| Frontend app repo | `web-<feature>` | `web-portal` |
| Shared library | `<feature>-lib` | `common-lib` |
| Automation role | `ansible-<feature>` | `ansible-database` |
| Script or tool | `tool-<purpose>` | `tool-benchmark` |
| Reserved, one per org | `gitops`, `values`, `infrastructure`, `docs`, `terraform-modules` | |
| Branches | `main`, `feature/<description>`, `hotfix/<description>`, `release/<version>` | `feature/add-search` |

### 3.2 Cloud resources

Pattern, joined with `-`; empty parts are skipped:

```text
<prefix>-<type>-<name>-<env>-<region>[-<suffix>]
prefix = company prefix (e.g. acme)
type   = resource abbreviation (table below)
region = short region code (e.g. weu)
suffix = optional 3 random characters [a-z0-9] for uniqueness
```

Examples:
- `acme-k8s-apps-prd-weu-x7k`
- `acme-kv-billing-tst-weu`
- `acme-buc-reports-dev-weu`

For providers with tight length limits (storage accounts, registries, key vaults), use the short form with no separators:

```text
<prefix><type><shortname><region><env><instance>   e.g. acmestbillingweuprd01
```

Private endpoints: `pep-<subresource>-<target-resource-name>`.

| Resource | Abbrev | Resource | Abbrev |
|---|---|---|---|
| Resource group / compartment | `rg` / `comp` | Kubernetes cluster | `k8s` |
| Kubernetes node pool | `np` | Virtual network | `vnet` |
| Subnet | `snet` | Network security group | `nsg` |
| Route table | `rt` | NAT gateway | `ngw` |
| Public IP | `pip` | Load balancer | `lb` |
| WAF policy | `wafpol` | Private endpoint | `pep` |
| Key vault | `kv` | Object storage bucket | `buc` |
| Storage account | `st` | IAM policy | `idpol` |
| Identity / managed identity | `id` | Group | `grp` |
| Virtual machine | `vm` | Block volume | `vol` |
| PostgreSQL | `psql` | MySQL | `mysql` |
| Redis | `redis` | Log group / workspace | `log` |
| Event stream | `stream` | Container registry | `cr` |

**MUST** generate names with the shared naming module or function, never by hand.

### 3.3 Kubernetes and GitOps

| Object | Pattern | Example |
|---|---|---|
| Deployment (GitOps application) | `<env>-<service>` | `prod-billing-service` |
| GitOps project | `<env>-<team>-project` | `prod-billing-project` |
| Deployment manifest | `apps/<env>/<service>.yaml` in the org `gitops` repo | `apps/tst/billing-service.yaml` |
| Helm chart | `helm/<service>/` in the service repo | |
| Environment values | `env/<env>/<service>/values-custom.yaml` in the org `values` repo | |
| Namespace | `<team>-<env>` | `billing-prod` |
| Policy file | `<resource>-<action>-<constraint>.yaml` | `pod-require-probes.yaml` |

### 3.4 Access groups

| Purpose | Pattern |
|---|---|
| Dashboards | `<org>-grafana-<app>-viewer` / `-editor` |
| Identity provider realm admins | `<org>-idp-<app>-<env>-admins` |
| GitOps access | `<org>-gitops-<app>-<env>-<role>` (`viewers`, `contributors`) |
| Artifact repository | `<org>-artifacts-admins` / `-developers` |

Access is requested through a ticket, never self-granted.

### 3.5 CI secrets in the vault

| Scope | Pattern |
|---|---|
| Per organization | `<org>--<name>` (e.g. `billing--teams-webhook-url`) |
| Shared | `<purpose>-*` (e.g. `release-app-id`, `release-app-private-key`, `artifacts-*`) |

---

## 4. Tagging *(corporate enforced practice)*

Every cloud resource carries these tags under one tag namespace (e.g. `Org-Tags`):

| Tag | Required | Type | Values |
|---|---|---|---|
| `ApplicationID` | yes | string (100) | CMDB application id |
| `UnitCode` | yes | string (3) | business unit code |
| `Environment` | yes | enum | `dev`, `tst`, `uat`, `sta`, `prd` |
| `StartDate` | yes | date | lifecycle start |
| `EndDate` | yes | date | lifecycle end (used for reservations) |
| `Component` | no | string (100) | finer-grained component |
| `PolicyCorrelationCode` | no | string (100) | links a resource to a group for tag-based IAM |
| `Automation` | no | JSON | `{enabled, tz, start_crons[], stop_crons[]}` start/stop schedule |

---

## 5. Kubernetes workloads

### 5.1 Labels *(corporate enforced practice)*

**MUST** on every workload:

| Key | Example |
|---|---|
| `app.kubernetes.io/name` | `billing-service` |
| `app.kubernetes.io/instance` | `billing-service-prod` |
| `app.kubernetes.io/version` | `1.4.2` |
| `app.kubernetes.io/component` | `backend` / `frontend` / `db` |
| `app.kubernetes.io/part-of` | `billing` |
| `app.kubernetes.io/managed-by` | `helm` |
| `app.example.io/environment` | `dev` / `tst` / `uat` / `prod` |
| `app.example.io/team` | owning team |

**Conditional:**

| Key | When |
|---|---|
| `logging.example.io/parser: json` | **MUST** on any pod that logs JSON |
| `governance.example.io/compliance: "true"` | opts a workload into strict admission rules early |
| `app.example.io/tier`, `app.example.io/release`, `app.example.io/purpose` | **SHOULD** where meaningful |

### 5.2 Admission rules *(corporate enforced practice)*

| Rule | Production | Non-production |
|---|---|---|
| CPU and memory requests plus a memory limit | Enforce | Audit (Enforce with the compliance label) |
| No BestEffort QoS | Enforce | Audit |
| `replicas >= 2` | Enforce | - |
| PodDisruptionBudget required, `minAvailable` below replicas | Enforce | Enforce (`minAvailable`) |
| Images only from approved registries | Enforce | Enforce |
| Databases expose metrics (`monitoring.enabled: true`) | Enforce | Enforce |
| Memory request equal to memory limit | - | Enforce |
| emptyDir `sizeLimit` | - | Enforce |
| Ingress with explicit host, HTTPS, no default backend | Enforce | Enforce |
| Liveness, readiness or startup probe | Audit | Audit |
| `readOnlyRootFilesystem: true` | Audit | Audit |
| No `:latest` tag | Audit | Audit |

New policies start in Audit in non-production, then move to Enforce, and only then reach production. *(corporate enforced practice)*

**Demo rule:** meet every Audit rule as well. That means probes, a read-only root filesystem, a pinned image, the labels above, and a memory request equal to the memory limit.

### 5.3 Images *(corporate enforced practice)*

- **MUST** pull public base images through the internal cache registry, never straight from public hubs.
- **MUST** publish to `<registry>/<org>/<image>:<semver>`.
- **SHOULD** deploy by digest, because version tags can be overwritten.
- **MUST NOT** use `:latest`.

### 5.4 Secrets *(corporate enforced practice)*

- **MUST** use External Secrets Operator syncing from the cloud vault through a cluster or project `SecretStore` authenticated with workload identity.
- **MUST NOT** commit `Secret` manifests that contain data.

---

## 6. Delivery

### 6.1 Flow *(corporate enforced practice)*

```text
pull request -> merge to main -> CI (version, image, Helm chart, release)
  -> CD writes the release version into gitops apps/<env>/<service>.yaml
  -> GitOps controller syncs (automated, prune, self-heal)
  -> higher environments gated by environment approvals or manual promotion
```

- Deployments use two sources: the Helm chart from the service repo at the release tag, and the environment values from the `values` repo main branch.
- Platform components (identity provider, GitOps controller, policy engine, secrets operator) are promoted by a promotion engine. Dev promotes automatically; production is promoted by hand.

### 6.2 CI

- **Shared workflows:** **MUST** be pinned to a release tag, never a moving branch (`main`, `stable`). *(corporate enforced practice)*
- **Third-party actions:** **SHOULD** be pinned to a full commit SHA. *(corporate enforced practice)*
- **Runners:** self-hosted autoscaled runners for anything that needs internal network or cloud access; hosted runners only for jobs that need neither. *(corporate enforced practice)*
- **Registry and vault access:** through workload identity on the runner. No long-lived cloud keys in CI secrets. *(corporate enforced practice)*
- **Permissions:** read-only at workflow level; write only on the jobs that need it. *(corporate enforced practice)*
- **Toolchain steps** run the same containerized commands as local development. *(corporate enforced practice)*
- **Dependencies:** every repo has a Renovate config. Shared platform workflows are excluded from automatic updates. *(corporate enforced practice)*
- **Notifications:** chat notifications go through workflow-based webhooks; legacy connector webhooks are retired. *(corporate enforced practice)*

### 6.3 Pull requests *(corporate enforced practice)*

- 1 approval from someone other than the author.
- All review threads resolved.
- A new push dismisses earlier approvals.
- Merge or squash only.
- Commit author and committer must use a company email domain or the platform's noreply address.
- **MUST NOT** merge with failing checks.
- If the repo keeps a changelog file, every pull request adds one unreleased entry; CI stamps the version.

---

## 7. Monitoring and observability

### 7.1 Platform *(corporate enforced practice)*

| Signal | Path |
|---|---|
| Metrics | Prometheus-compatible scrape or remote write -> long-term metrics store -> Grafana |
| Logs | Node agent (tail + Kubernetes metadata) -> aggregator -> Loki |
| Traces | OpenTelemetry -> trace store |
| Off-cluster sources | OTLP over HTTP or gRPC (metrics, logs, traces) |

### 7.2 Instrumentation

- Services **MUST** emit OpenTelemetry-compatible metrics, traces and logs. *(corporate enforced practice)*
- **SHOULD** use operator auto-instrumentation; add the SDK only where auto-instrumentation can't work. *(corporate enforced practice)* Example for a Java service:

  ```yaml
  spec:
    template:
      metadata:
        annotations:
          instrumentation.opentelemetry.io/inject-java: observability/java-default
  ```

- Every service exposes health endpoints and wires them to probes.

### 7.3 Logs *(corporate enforced practice)*

- **MUST** log JSON to stdout, one object per line, and label the pod `logging.example.io/parser: json`.
- **MUST** include `trace_id` and `span_id` when instrumented.
- **MUST** use these field names:

  ```json
  {"time":"2026-10-30T12:00:00.000Z","level":"INFO","logger_name":"com.example.billing.InvoiceService","thread_name":"http-exec-5","message":"Invoice created","trace_id":"4bf92f3577b34da6a3ce929d0e0e4736","span_id":"00f067aa0ba902b7","correlation_id":"c-1029"}
  ```

- Loki labels:

  | Origin | Labels |
  |---|---|
  | Kubernetes metadata | `namespace`, `pod`, `container`, `host` |
  | The log record | `level`, `logger_name`, `thread_name`, `trace_id`, `correlation_id` |
  | Kubernetes events | `event_*` |

- **MUST NOT** log secrets, tokens or personal data.

### 7.4 Metrics

- Name metrics `<namespace>_<name>_<unit>`. Counters end in `_total` and durations in `_seconds`.
- **MUST** keep label values bounded: no user ids, request ids, raw URLs or error messages as labels. Estimate the new series count before adding a label. *(corporate enforced practice)*
- **SHOULD** use RED for services (rate, errors, duration histogram) and USE for resources (utilization, saturation, errors).
- **MUST** enable metrics on managed data services (databases, search, caches). *(corporate enforced practice)*
- **SHOULD** report CI pipeline metrics and annotate releases on dashboards. *(corporate enforced practice)*

### 7.5 Alerting *(corporate enforced practice)*

- **MUST** define alerts as `PrometheusRule` resources. Source-side rules in the metrics and logs backends are preferred over dashboard-managed rules.
- **MUST** give every rule `labels.severity` (`warning` or `critical`) and the annotations `summary`, `description` and `runbook_url`.

  ```yaml
  apiVersion: monitoring.coreos.com/v1
  kind: PrometheusRule
  metadata:
    name: billing-service-alerts
    labels:
      app.kubernetes.io/part-of: billing
  spec:
    groups:
      - name: billing-service
        rules:
          - alert: BillingServiceHighErrorRate
            expr: |
              sum(rate(http_server_requests_seconds_count{namespace="billing-prod",service="billing-service",status=~"5.."}[5m]))
              / sum(rate(http_server_requests_seconds_count{namespace="billing-prod",service="billing-service"}[5m])) > 0.05
            for: 10m
            labels:
              severity: critical
            annotations:
              summary: "billing-service 5xx ratio above 5%"
              description: "{{ $value | humanizePercentage }} of requests failed over the last 10 minutes."
              runbook_url: "https://docs.example.com/runbooks/billing-service-high-error-rate"
  ```

- **Severity:**
  - `warning`: the value is outside its normal range (e.g. CPU above 70% when normal is 50%).
  - `critical`: the value has reached a failure threshold (e.g. CPU above 90%).
  - `critical` suppresses `warning` for the same rule.
- **Routing:** each application has three receiver groups, each with both severities, delivered to dedicated chat channels:
  - infrastructure support: CPU, memory, disk;
  - application support: errors, health checks, app metrics;
  - database support: sessions, execution counts, storage.
- **Review:** the platform team reviews recording and alerting rules for performance impact before merge.
- **SLOs:** define SLIs from user-facing signals and set SLO targets with an error budget. Alert on multiwindow burn rate rather than raw thresholds.

### 7.6 Dashboards *(corporate enforced practice)*

- **MUST** be code: a `GrafanaDashboard` custom resource in the monitoring config, one folder per application, with this `instanceSelector`:

  ```yaml
  apiVersion: grafana.integreatly.org/v1beta1
  kind: GrafanaDashboard
  metadata:
    name: billing-service
    namespace: dashboards
  spec:
    folder: billing
    instanceSelector:
      matchLabels:
        app.kubernetes.io/name: grafana
    json: |
      { "title": "billing-service", "panels": [] }
  ```

- Access uses the viewer and editor groups from section 3.4.

---

## 8. APIs *(corporate enforced practice)*

- REST or gRPC only. SOAP is deprecated and must not be used in new work.
- REST resources are **plural nouns**:
  - `GET /patients`
  - `POST /patients`
  - `PUT|PATCH|DELETE /patients/{id}`
  - actions as `POST /patients/{id}/activate`
  - complex search as `POST /patients/search`
- Status codes:
  - success: `200`, `201`, `202`;
  - client errors: `400`, `401`, `403`, `404`, `405`, `409`;
  - server errors: `500`, `501`.
  - Rate limiting (`429`) is applied at the gateway only.
- **MUST** document REST APIs in OpenAPI 3.1, with request and response examples and every status code.
- **MUST** use proto3 for gRPC and the standard gRPC-to-HTTP status mapping.
- **Versioning:** support at most n-3 versions, stay backward compatible, and remove deprecated fields at end of support.
- **Auth:** tokens are validated at the API gateway. Services use claims only for data filtering. Standard authorization (OAuth 2.0 / UMA 2.0); no custom schemes.
- mTLS, retries and circuit breaking belong to the gateway or service mesh, not to service code.

---

## 9. Documentation *(corporate enforced practice)*

- High-level design goes in the team wiki; configuration and usage docs go in repo Markdown (`README.md`).
- Diagrams use C4 or draw.io. API contracts use OpenAPI 3.1.
- A significant platform change needs a written technical improvement proposal approved before work starts.
- Every alert's `runbook_url` points to an existing runbook.
- Every repo README states its purpose, how to run and test it in the sandbox, how it deploys (environments and GitOps path), and its owners.

---

## 10. Definition of done

| Area | Must be true |
|---|---|
| Intake | Grill record completed; sandbox used, or the opt-out and its reason recorded. *(corporate enforced practice)* |
| Toolchain | Node with pnpm and a frozen lockfile, or Python with uv and a frozen lockfile; every tool runs in a container with a memory cap. *(corporate enforced practice)* |
| Repo | Name follows 3.1; README; Renovate config; pre-commit with secret scan; CI on pinned shared workflows. *(corporate enforced practice)* |
| Tests | Test-first, boundary-only mocks, integration tests with real dependencies in containers, green in CI. *(corporate enforced practice)* |
| Image | Base from the cache registry; pushed under the org path; semver tag plus digest; scanned, SBOM attached, signed; no `:latest`. *(corporate enforced practice)* |
| Kubernetes | Labels from 5.1; requests and limits with memory request equal to limit; probes; read-only root FS; `replicas >= 2` and a PDB in production; JSON logs with the parser label. *(corporate enforced practice)* |
| Secrets | ExternalSecret from the vault; nothing secret in Git. *(corporate enforced practice)* |
| GitOps | `apps/<env>/<service>.yaml`, deployment name `<env>-<service>`, two-source Helm, values in `env/<env>/<service>/values-custom.yaml`. *(corporate enforced practice)* |
| Cloud resources | Created through platform APIs or the naming module with all required tags. *(corporate enforced practice)* |
| Observability | OpenTelemetry auto-instrumentation; RED metrics with bounded labels; `PrometheusRule` with `severity` and `runbook_url`; `GrafanaDashboard` resource. *(corporate enforced practice)* |
| Identity | Workload identity, no static keys; access through groups from 3.4. *(corporate enforced practice)* |
| Pull request | English, Conventional Commit title, no secrets, reviewer approval, green checks, changelog entry where the repo keeps one. *(corporate enforced practice)* |