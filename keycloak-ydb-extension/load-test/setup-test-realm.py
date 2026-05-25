#!/usr/bin/env python3
"""
Sets up a test realm in Keycloak for load testing.

Creates:
  - test-realm with registration enabled
  - Clients: gatling (service account), client-0 (OIDC), test-client (public)
  - Roles: user, admin, manager
  - Groups: developers, testers, devops
  - 10 test users (testuser1..10) with passwords, roles, and groups
  - 1 benchmark user (user-0) for keycloak-benchmark scenarios

Usage:
  python3 setup-test-realm.py [KC_URL]
  KC_URL defaults to http://localhost:9090
"""

import sys
import urllib.request
import urllib.parse
import urllib.error
import json

KC_URL = sys.argv[1] if len(sys.argv) > 1 else "http://localhost:9090"
ADMIN_USER = "admin"
ADMIN_PASS = "admin"


def api(method, path, data=None, token=None):
    url = KC_URL + path
    headers = {}
    if token:
        headers["Authorization"] = f"Bearer {token}"

    if data is not None and method != "GET":
        headers["Content-Type"] = "application/json"
        body = json.dumps(data).encode()
    else:
        body = None

    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    try:
        resp = urllib.request.urlopen(req)
        content = resp.read().decode()
        return resp.status, json.loads(content) if content else None
    except urllib.error.HTTPError as e:
        content = e.read().decode()
        try:
            return e.code, json.loads(content)
        except Exception:
            return e.code, content


def get_token():
    data = urllib.parse.urlencode({
        "username": ADMIN_USER, "password": ADMIN_PASS,
        "grant_type": "password", "client_id": "admin-cli"
    }).encode()
    req = urllib.request.Request(
        KC_URL + "/realms/master/protocol/openid-connect/token",
        data=data, headers={"Content-Type": "application/x-www-form-urlencoded"})
    resp = urllib.request.urlopen(req)
    return json.loads(resp.read().decode())["access_token"]


def ok(status):
    return 200 <= status < 300


def main():
    print(f"Keycloak URL: {KC_URL}")
    print(f"Admin credentials: {ADMIN_USER}/{ADMIN_PASS}")
    print()

    # 1. Token
    print("=== Getting admin token ===")
    token = get_token()
    print(f"  Token: {token[:30]}...")
    print()

    # 2. Realm
    print("=== Creating realm: test-realm ===")
    code, resp = api("POST", "/admin/realms", data={
        "realm": "test-realm",
        "enabled": True,
        "displayName": "Test Realm for Load Testing",
        "registrationAllowed": True,
        "loginWithEmailAllowed": True
    }, token=token)
    if code == 201:
        print("  CREATED (201)")
    elif code == 409:
        print("  Already exists (409), skipping")
    else:
        print(f"  FAILED ({code}): {resp}")
        sys.exit(1)
    print()

    # 3. Clients
    print("=== Creating clients ===")
    clients_spec = [
        {
            "clientId": "gatling", "enabled": True,
            "clientAuthenticatorType": "client-secret",
            "secret": "setup-for-benchmark",
            "redirectUris": ["*"], "serviceAccountsEnabled": True,
            "publicClient": False, "protocol": "openid-connect",
            "attributes": {"post.logout.redirect.uris": "+"}
        },
        {
            "clientId": "client-0", "enabled": True,
            "clientAuthenticatorType": "client-secret",
            "secret": "client-0-secret",
            "redirectUris": ["*"], "serviceAccountsEnabled": True,
            "publicClient": False, "protocol": "openid-connect",
            "attributes": {"post.logout.redirect.uris": "+"}
        },
        {
            "clientId": "test-client", "enabled": True,
            "directAccessGrantsEnabled": True,
            "publicClient": True,
            "redirectUris": ["*"], "webOrigins": ["*"]
        },
    ]
    for c in clients_spec:
        code, _ = api("POST", "/admin/realms/test-realm/clients", data=c, token=token)
        if code == 409:
            # Client exists — update its secret and settings
            _, existing = api("GET", f"/admin/realms/test-realm/clients?clientId={c['clientId']}", token=token)
            if existing:
                client_uuid = existing[0]["id"]
                update_code, _ = api("PUT", f"/admin/realms/test-realm/clients/{client_uuid}", data={**existing[0], **c}, token=token)
                status_msg = "UPDATED" if update_code == 204 else f"update FAILED ({update_code})"
            else:
                status_msg = "already exists (could not update)"
        elif code == 201:
            status_msg = "CREATED"
        else:
            status_msg = f"FAILED ({code})"
        print(f"  {c['clientId']}: {status_msg}")

    # Assign realm-management roles to gatling service account
    print("  Assigning realm-management roles to gatling service account...")
    code, clients = api("GET", "/admin/realms/test-realm/clients?clientId=gatling", token=token)
    gatling_id = clients[0]["id"]
    code, sa_user = api("GET", f"/admin/realms/test-realm/clients/{gatling_id}/service-account-user", token=token)
    sa_user_id = sa_user["id"]
    code, rm_clients = api("GET", "/admin/realms/test-realm/clients?clientId=realm-management", token=token)
    rm_id = rm_clients[0]["id"]

    roles_to_assign = []
    for role_name in ["manage-clients", "view-users", "manage-realm", "manage-users", "query-users", "query-groups"]:
        code, role = api("GET", f"/admin/realms/test-realm/clients/{rm_id}/roles/{role_name}", token=token)
        roles_to_assign.append(role)

    code, _ = api("POST",
        f"/admin/realms/test-realm/users/{sa_user_id}/role-mappings/clients/{rm_id}",
        data=roles_to_assign, token=token)
    print(f"  Roles assigned: {code}")
    print()

    # 4. Realm roles
    print("=== Creating realm roles ===")
    for role_name in ["user", "admin", "manager"]:
        code, _ = api("POST", "/admin/realms/test-realm/roles",
            data={"name": role_name, "description": f"{role_name} role"}, token=token)
        status_msg = "CREATED" if code == 201 else "already exists" if code == 409 else f"FAILED ({code})"
        print(f"  {role_name}: {status_msg}")
    print()

    # 5. Groups
    print("=== Creating groups ===")
    for group_name in ["developers", "testers", "devops"]:
        code, _ = api("POST", "/admin/realms/test-realm/groups",
            data={"name": group_name}, token=token)
        status_msg = "CREATED" if code == 201 else "already exists" if code == 409 else f"FAILED ({code})"
        print(f"  {group_name}: {status_msg}")
    print()

    # Fetch roles and groups for assignment
    roles = {}
    for rname in ["user", "admin", "manager"]:
        code, data = api("GET", f"/admin/realms/test-realm/roles/{rname}", token=token)
        roles[rname] = data

    code, groups_data = api("GET", "/admin/realms/test-realm/groups", token=token)
    groups = {g["name"]: g["id"] for g in groups_data}

    role_list = ["user", "admin", "manager"]
    group_list = ["developers", "testers", "devops"]

    # 6. Test users
    print("=== Creating test users ===")
    for i in range(1, 11):
        uname = f"testuser{i}"
        role_name = role_list[(i - 1) % 3]
        group_name = group_list[(i - 1) % 3]

        code, _ = api("POST", "/admin/realms/test-realm/users", data={
            "username": uname,
            "email": f"{uname}@example.com",
            "firstName": "Test",
            "lastName": f"User{i}",
            "enabled": True,
            "credentials": [{"type": "password", "value": "password", "temporary": False}]
        }, token=token)

        if code == 201:
            code2, users = api("GET",
                f"/admin/realms/test-realm/users?username={uname}&exact=true", token=token)
            user_id = users[0]["id"]

            api("POST", f"/admin/realms/test-realm/users/{user_id}/role-mappings/realm",
                data=[roles[role_name]], token=token)
            api("PUT", f"/admin/realms/test-realm/users/{user_id}/groups/{groups[group_name]}",
                data={}, token=token)

            print(f"  {uname}: CREATED | role={role_name} | group={group_name}")
        elif code == 409:
            print(f"  {uname}: already exists, skipping")
        else:
            print(f"  {uname}: FAILED ({code})")

    # 7. Benchmark user (user-0)
    print()
    print("=== Creating benchmark user (user-0) ===")
    code, _ = api("POST", "/admin/realms/test-realm/users", data={
        "username": "user-0",
        "enabled": True,
        "firstName": "Firstname",
        "lastName": "Lastname",
        "email": "user-0@keycloak.org",
        "credentials": [{"type": "password", "value": "user-0-password", "temporary": False}]
    }, token=token)
    status_msg = "CREATED" if code == 201 else "already exists" if code == 409 else f"FAILED ({code})"
    print(f"  user-0: {status_msg}")
    print()

    # 8. Verification
    print("=== Verification ===")

    # Users
    code, users = api("GET", "/admin/realms/test-realm/users?max=50", token=token)
    print(f"  Total users in test-realm: {len(users)}")
    for u in users:
        code, rm = api("GET",
            f"/admin/realms/test-realm/users/{u['id']}/role-mappings/realm", token=token)
        user_roles = [r["name"] for r in (rm or []) if not r["name"].startswith("default-roles")]
        code, ug = api("GET",
            f"/admin/realms/test-realm/users/{u['id']}/groups", token=token)
        user_groups = [g["name"] for g in (ug or [])]
        print(f"    {u['username']:15} | {u.get('email',''):25} | roles={user_roles} | groups={user_groups}")

    # Clients
    code, all_clients = api("GET", "/admin/realms/test-realm/clients", token=token)
    custom_clients = [c for c in all_clients if c["clientId"] in ("gatling", "client-0", "test-client")]
    print(f"\n  Custom clients: {[c['clientId'] for c in custom_clients]}")

    # Test login
    print("\n=== Testing login ===")
    try:
        login_data = urllib.parse.urlencode({
            "username": "testuser1", "password": "password",
            "grant_type": "password", "client_id": "test-client"
        }).encode()
        req = urllib.request.Request(
            KC_URL + "/realms/test-realm/protocol/openid-connect/token",
            data=login_data, headers={"Content-Type": "application/x-www-form-urlencoded"})
        resp = urllib.request.urlopen(req)
        data = json.loads(resp.read().decode())
        print(f"  testuser1 login: SUCCESS (token expires in {data['expires_in']}s)")
    except urllib.error.HTTPError as e:
        print(f"  testuser1 login: FAILED ({e.code})")

    print()
    print("Setup complete!")


if __name__ == "__main__":
    main()