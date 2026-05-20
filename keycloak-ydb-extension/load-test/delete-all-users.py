#!/usr/bin/env python3
"""
Deletes all users from test-realm in Keycloak (batched).

Usage:
  python3 delete-all-users.py [KC_URL] [REALM]
  KC_URL defaults to http://localhost:9090
  REALM  defaults to test-realm
"""

import sys
import urllib.request
import urllib.parse
import urllib.error
import json

KC_URL = sys.argv[1] if len(sys.argv) > 1 else "http://localhost:9090"
REALM = sys.argv[2] if len(sys.argv) > 2 else "test-realm"
ADMIN_USER = "admin"
ADMIN_PASS = "admin"
BATCH_SIZE = 100


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
        KC_URL + f"/realms/master/protocol/openid-connect/token",
        data=data, headers={"Content-Type": "application/x-www-form-urlencoded"})
    resp = urllib.request.urlopen(req)
    return json.loads(resp.read().decode())["access_token"]


def main():
    print(f"Keycloak: {KC_URL}")
    print(f"Realm: {REALM}")
    print()

    token = get_token()
    total_deleted = 0

    while True:
        code, users = api("GET", f"/admin/realms/{REALM}/users?max={BATCH_SIZE}", token=token)
        if code != 200 or not users:
            break

        print(f"Fetched {len(users)} users...")
        for u in users:
            username = u["username"]
            uid = u["id"]
            code, _ = api("DELETE", f"/admin/realms/{REALM}/users/{uid}", token=token)
            if code == 204:
                total_deleted += 1
            else:
                print(f"  Failed to delete {username}: {code}")

        print(f"  Deleted batch. Total so far: {total_deleted}")

        # Refresh token periodically
        if total_deleted % 500 == 0:
            token = get_token()

    print(f"\nDone. Deleted {total_deleted} users from {REALM}.")


if __name__ == "__main__":
    main()