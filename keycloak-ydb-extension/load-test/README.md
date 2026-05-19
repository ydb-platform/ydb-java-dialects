# Load Testing

Load tests for Keycloak using [keycloak-benchmark](https://github.com/keycloak/keycloak-benchmark) (Gatling).

Supports two infrastructure configurations:

- **Keycloak + Local YDB** — YDB in Docker, retry-proxy in front of Keycloak
- **Keycloak + Remote YDB** — external YDB instance, retry-proxy in front of Keycloak

## Prerequisites

- Java 21+
- Python 3
- Docker + Docker Compose

## Quick Start

## 1. Download keycloak-benchmark

```bash
./prepare.sh
```

Downloads the Gatling benchmark JARs from GitHub releases into `lib/`.
To use a specific version:

```bash
./prepare.sh 26.4.0-SNAPSHOT
```

## 2. Start infrastructure

All commands below are run from the `keycloak-ydb-extension/` root.

### Option A: Keycloak + Local YDB

```bash
../run-keycloak-with-ydb.sh
```

This builds core + retry-proxy, copies the JAR, and starts Docker Compose (YDB + Keycloak + retry-proxy).

Wait for Keycloak to start (~30-60s). Check logs:

```bash
docker compose -f docker/docker-compose.yml logs -f ydb-keycloak
```

| Service                    | URL                   |
|----------------------------|-----------------------|
| Keycloak (via retry-proxy) | http://localhost:9090 |
| YDB Monitoring             | http://localhost:8765 |

### Option B: Keycloak + Remote YDB

Start YDB separately, e.g.:

```bash
docker run -d --rm --name ydb-local -h localhost \
  --platform linux/amd64 \
  -p 2135:2135 -p 2136:2136 -p 8765:8765 \
  -v $(pwd)/ydb_certs:/ydb_certs -v $(pwd)/ydb_data:/ydb_data \
  -e GRPC_TLS_PORT=2135 -e GRPC_PORT=2136 -e MON_PORT=8765 \
  ydbplatform/local-ydb:latest
```

Then start Keycloak + retry-proxy:

```bash
YDB_JDBC_URL="jdbc:ydb:grpc://host.docker.internal:2136/local" \
  docker compose -f docker/docker-compose-remote-ydb.yml up -d --build
```

For a cloud YDB instance:

```bash
YDB_JDBC_URL="jdbc:ydb:grpcs://ydb.serverless.yandexcloud.net:2135/ru-central1/..." \
  docker compose -f docker/docker-compose-remote-ydb.yml up -d --build
```

| Service                    | URL                   |
|----------------------------|-----------------------|
| Keycloak (via retry-proxy) | http://localhost:9090 |

Admin credentials for all options: `admin` / `admin`

### Comparison with other databases

For benchmarking Keycloak with other databases (PostgreSQL, MySQL, etc.), use the setups from the keycloak-benchmark repository:
[keycloak-benchmark](https://github.com/keycloak/keycloak-benchmark/tree/main/provision)

## 3. Setup test realm

```bash
python3 setup-test-realm.py
```

Creates `test-realm` with clients (`gatling`, `client-0`, `test-client`), roles, groups, and test users.

## 4. Run load test

All commands are run from the `load-test/` directory.

First, build the classpath:

```bash
CLASSPATH=$(find lib -name '*.jar' | tr '\n' ':')
```

---

### Admin scenarios using service account (CreateUsers, CreateDeleteUsers, CreateClients, CreateDeleteClients)

These scenarios authenticate via the `gatling` service account created by `setup-test-realm.py`.

```bash
java -server -Xmx1G \
    -Dserver-url=http://localhost:9090 \
    -Drealm-name=test-realm \
    -Dclient-id=gatling \
    -Dclient-secret=setup-for-benchmark \
    -Dusers-per-sec=10 \
    -Dmeasurement=60 \
    -cp "$CLASSPATH" \
    io.gatling.app.Gatling \
    -rf results \
    -s keycloak.scenario.admin.CreateUsers
```

Swap `-s` for any of:
- `keycloak.scenario.admin.CreateUsers`
- `keycloak.scenario.admin.CreateDeleteUsers`
- `keycloak.scenario.admin.CreateClients`
- `keycloak.scenario.admin.CreateDeleteClients`

---

### Admin scenarios using admin account (CreateRealms, CreateDeleteRealms, ListSessions)

These scenarios authenticate as the Keycloak admin user.

```bash
java -server -Xmx1G \
    -Dserver-url=http://localhost:9090 \
    -Drealm-name=test-realm \
    -Dclient-id=gatling \
    -Dclient-secret=setup-for-benchmark \
    -Dadmin-username=admin \
    -Dadmin-password=admin \
    -Dusers-per-sec=10 \
    -Dmeasurement=60 \
    -cp "$CLASSPATH" \
    io.gatling.app.Gatling \
    -rf results \
    -s keycloak.scenario.admin.CreateRealms
```

Swap `-s` for any of:
- `keycloak.scenario.admin.CreateRealms`
- `keycloak.scenario.admin.CreateDeleteRealms`
- `keycloak.scenario.admin.ListSessions`

---

### Authentication scenario: Client Credentials (ClientSecret)

Authenticates via `client_credentials` grant — no user login required.

```bash
java -server -Xmx1G \
    -Dserver-url=http://localhost:9090 \
    -Drealm-name=test-realm \
    -Dclient-id=gatling \
    -Dclient-secret=setup-for-benchmark \
    -Dusers-per-sec=10 \
    -Dmeasurement=60 \
    -cp "$CLASSPATH" \
    io.gatling.app.Gatling \
    -rf results \
    -s keycloak.scenario.authentication.ClientSecret
```

---

### Authentication scenarios: User Login (AuthorizationCode, LoginUserPassword)

These scenarios simulate real user logins. They require `http://0.0.0.0:9090` instead of `localhost` —
Gatling refuses to send secure cookies to localhost with Keycloak 26
(see [keycloak-benchmark#945](https://github.com/keycloak/keycloak-benchmark/issues/945)).
Also make sure `hostname` in `docker/conf/keycloak.conf` matches this address (see retry-proxy README).

By default, users `user-0`, `user-1`, ... with passwords `user-0-password`, `user-1-password`, ... are used (created by `setup-test-realm.py`).

```bash
java -server -Xmx1G \
    -Dserver-url=http://0.0.0.0:9090 \
    -Drealm-name=test-realm \
    -Dclient-id=gatling \
    -Dclient-secret=setup-for-benchmark \
    -Dusers-per-sec=10 \
    -Dmeasurement=60 \
    -cp "$CLASSPATH" \
    io.gatling.app.Gatling \
    -rf results \
    -s keycloak.scenario.authentication.AuthorizationCode
```

Swap `-s` for any of:
- `keycloak.scenario.authentication.AuthorizationCode`
- `keycloak.scenario.authentication.LoginUserPassword`

Results are saved to `results/` with Gatling HTML reports.

## 5. Cleanup between runs

Delete all users from test-realm:

```bash
python3 delete-all-users.py
```

## Available Scenarios

| Scenario | Auth method | localhost ok? |
|---|---|:---:|
| `keycloak.scenario.admin.CreateUsers` | service account | yes |
| `keycloak.scenario.admin.CreateDeleteUsers` | service account | yes |
| `keycloak.scenario.admin.CreateClients` | service account | yes |
| `keycloak.scenario.admin.CreateDeleteClients` | service account | yes |
| `keycloak.scenario.admin.CreateRealms` | admin account | yes |
| `keycloak.scenario.admin.CreateDeleteRealms` | admin account | yes |
| `keycloak.scenario.admin.ListSessions` | admin account | yes |
| `keycloak.scenario.authentication.ClientSecret` | client credentials | yes |
| `keycloak.scenario.authentication.AuthorizationCode` | user login | **no** — use `0.0.0.0` |
| `keycloak.scenario.authentication.LoginUserPassword` | user login | **no** — use `0.0.0.0` |

Full list of scenarios:
[keycloak-benchmark/scenario](https://github.com/keycloak/keycloak-benchmark/tree/main/benchmark/src/main/scala/keycloak/scenario)

## Directory Structure

```
load-test/
  prepare.sh            # Downloads keycloak-benchmark from GitHub
  setup-test-realm.py   # Creates test realm, clients, users
  delete-all-users.py   # Deletes all users from realm
  lib/                  # Benchmark JARs (gitignored)
  results/              # Gatling reports (gitignored)
```
