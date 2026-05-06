# SLO Testing for YDB Spring Retry

SLO (Service Level Objectives) testing validates that the **spring-ydb-retry** library reduces visible application errors during YDB cluster node failures — restarts, shutdowns, network issues, and kill signals.

## How It Works

Two identical Spring Boot applications run the same workload (read/write) against the same YDB cluster:

| Instance | Port | Retry | Description |
|---|---|---|---|
| `app-with-retry` | 8081 | **Enabled** (max 10 retries, idempotent=true) | Uses the same workload with global retry enabled |
| `app-no-retry` | 8082 | **Disabled** | Uses the same workload with `YDB_TRANSACTION_RETRY_ENABLED=false` |

A chaos script periodically stops, restarts, and kills random YDB nodes. The Grafana dashboard shows an error rate comparison, clearly demonstrating that retry significantly reduces visible application errors.

## Test Scenarios

Two chaos levels are available:

| Scenario | Directory | Description |
|---|---|---|
| **chaos** | `playground/chaos/` | Baseline: stop/start, restart, SIGKILL of individual nodes |
| **chaos-aggressive** | `playground/chaos-aggressive/` | Aggressive: pause/unpause, multi-node kill, rapid kill/start, triple kill + resource constraints |

See [`playground/README.md`](playground/README.md) for details.

## Quick Start

### 1. Start (baseline chaos)

```bash
cd slo/playground/chaos
docker compose up --build -d
```

Wait ~60 seconds for YDB to initialize and apps to seed data.

### 2. Start (aggressive chaos)

```bash
cd slo/playground/chaos-aggressive
docker compose up --build -d
```

### 3. Open Grafana

Navigate to **http://localhost:3000** (login: `admin` / `admin`).

The **"YDB Spring Retry SLO - Retry vs No-Retry Comparison"** dashboard is pre-loaded and auto-refreshes every 5 seconds.

### 4. Stop

```bash
docker compose down
```

To also remove data volumes:

```bash
docker compose down -v
```

## Services

| Service | URL | Description |
|---|---|---|
| Grafana | http://localhost:3000 | Metrics dashboard (admin/admin) |
| Prometheus | http://localhost:9090 | Metrics storage |
| YDB Monitoring | http://localhost:8765 | YDB cluster UI |
| YDB gRPC | grpc://localhost:2136 | YDB endpoint |
| App with retry metrics | internal `http://app-with-retry:9464/metrics` | Prometheus scrape target |
| App without retry metrics | internal `http://app-no-retry:9464/metrics` | Prometheus scrape target |

The app containers do not publish their internal Spring Boot or metrics ports to the host. Prometheus scrapes them over the Docker network at `:9464/metrics`.

## Metrics

The SLO application exports Prometheus metrics via OpenTelemetry SDK:

| Metric | Type | Labels | Description |
|---|---|---|---|
| `slo_operations_total` | Counter | ref, operation_type, status, error_type | Total operations |
| `slo_operation_duration_seconds` | Histogram | ref, operation_type, status, error_type | Operation latency |

### Labels

| Label | Values | Description |
|---|---|---|
| `ref` | `with-retry`, `no-retry` | Instance identifier |
| `operation_type` | `read`, `write` | Operation type |
| `status` | `success`, `failure` | Operation result |
| `error_type` | `none`, `UNAVAILABLE`, `TRANSPORT_UNAVAILABLE`, `OVERLOADED`, `BAD_SESSION`, ... | YDB status code or exception class name |

## Configuration

Environment variables for the app containers:

| Variable | Default | Description |
|---|---|---|
| `SERVER_PORT` | 8080 | HTTP port |
| `SPRING_DATASOURCE_URL` | - | YDB JDBC URL |
| `YDB_TRANSACTION_RETRY_ENABLED` | true | Enable/disable retry |
| `YDB_TRANSACTION_RETRY_MAX_RETRIES` | 10 | Max retry attempts |
| `YDB_TRANSACTION_RETRY_IDEMPOTENT` | true | Treat operations as idempotent |
| `SLO_RUN_ID` | auto | Shared run identifier used for result folder name |
| `SLO_RESULTS_DIR` | `/app/results` in Docker | Root directory for saved run results |
| `REF` | unknown | Label for metrics (with-retry / no-retry) |
| `SLO_READ_RPS` | 100 | Target read RPS |
| `SLO_WRITE_RPS` | 100 | Target write RPS |
| `SLO_INITIAL_DATA` | 1000 | Initial rows to seed |
| `SLO_TIME` | 600 | Workload duration in seconds |

## Saved Results

```text
results/
  <runId>/
    retry
    no-retry
```

The `retry` file contains the final summary for `app-with-retry`, and `no-retry` contains the final summary for `app-no-retry`.
