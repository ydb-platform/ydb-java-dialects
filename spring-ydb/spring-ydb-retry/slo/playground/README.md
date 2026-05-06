# Playground

Docker Compose environments for running SLO tests with chaos injection. Each scenario deploys a full YDB cluster, two workload applications (with and without retry), Prometheus, Grafana, and a chaos container.

## Shared Infrastructure

All scenarios use the same architecture:

| Component | Count | Description |
|---|---|---|
| YDB static node | 1 | Storage node + discovery (`static-0`) |
| YDB database nodes | 5 | Tenant nodes (`database-1` .. `database-5`) |
| SLO app with retry | 1 | Port 8081, retry enabled |
| SLO app without retry | 1 | Port 8082, retry disabled |
| Prometheus | 1 | Scrapes metrics every 5s |
| Grafana | 1 | Visualization on port 3000 |
| Chaos container | 1 | Docker container with docker.sock access |

All services run on a single Docker network `slo-network`. The YDB cluster uses erasure `none` (no storage-level replication), which amplifies the impact of failures.

---

## Scenario 1: `chaos/` — Baseline Chaos

A mild scenario modeling typical operational failures: graceful shutdown, restart, and crash of a single node at a time.

### Start

```bash
cd slo/playground/chaos
docker compose up --build -d
```

### Chaos Phases (`chaos.sh`)

The chaos script starts 60 seconds after launch (once YDB and apps are ready).

| Phase | Iterations | Action | Pause | Generated Errors |
|---|---|---|---|---|
| Stop/Start | 5 | `docker stop` → `docker start` a random node | 60s | `UNAVAILABLE`, `TRANSPORT_UNAVAILABLE` |
| Restart | 3 | `docker restart -t 0` a random node (instant) | 60s | `TRANSPORT_UNAVAILABLE` |
| Final Kill | 1 | `docker kill -s SIGKILL` a random node | — | `UNAVAILABLE`, `BAD_SESSION` |

**Total chaos duration:** ~8 minutes after the 60s delay.

---

## Scenario 2: `chaos-aggressive/` — Aggressive Chaos

An intensive scenario with multi-node failures, pause/unpause, and rapid kill/start cycles. YDB nodes run with constrained resources (768 MB RAM, 1 CPU), amplifying the effect.

### Start

```bash
cd slo/playground/chaos-aggressive
docker compose up --build -d
```

### Chaos Phases (`chaos.sh`)

| Phase | Iterations | Action | Pause |
|---|---|---|---|
| 1. Pause/Unpause | 4 | `docker pause` 20s → `docker unpause` one node | 15s |
| 2. Multi-node Kill | 3 | `SIGKILL` **two** nodes simultaneously → `docker start` both | 25s |
| 3. Instant Restart | 3 | `docker restart -t 0` one node | 20s |
| 4. Dual Pause | 1 | `docker pause` **two** nodes for 30s → unpause | 15s |
| 5. Rapid Kill/Start | 5 | `SIGKILL` → `docker start` with no gap | 8s |
| 6. Final Triple Kill | 1 | `SIGKILL` **three** nodes simultaneously | — |

**Total chaos duration:** ~7 minutes after the 60s delay.

---

## Configuration Files

### `configs/ydb.yaml`

YDB cluster configuration with erasure `none`, a single storage pool (SSD), and 5 database nodes connected to the tenant `/Root/testdb`.

### `configs/prometheus/prometheus.yaml`

Scrape configuration: both apps are scraped every 5 seconds at `:9464/metrics`.

### `configs/grafana/provisioning/`

- **datasource.yaml** — Prometheus datasource
- **dashboard.yaml** — Auto-loads JSON dashboards from the directory
- **slo.json** — Pre-built dashboard

## Cleanup

```bash
docker compose down -v
```

Removes containers, networks, and volumes (Prometheus data, Grafana DB).
