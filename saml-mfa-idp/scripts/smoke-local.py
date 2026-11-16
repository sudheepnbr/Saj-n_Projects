#!/usr/bin/env python3
"""Exercise setup → login → admin SP → IdP-initiated SSO against a running IdP."""

from __future__ import annotations

import html
import re
import sys
import urllib.error
import urllib.parse
import urllib.request
from http.cookiejar import CookieJar

BASE = sys.argv[1] if len(sys.argv) > 1 else "http://127.0.0.1:8080"
TOKEN = sys.argv[2] if len(sys.argv) > 2 else "oih-local-setup"
PASSWORD = "CorrectHorseBattery1!"
USERNAME = "bootstrap"

jar = CookieJar()
opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(jar))


def fetch(path: str, data: dict | None = None, method: str | None = None) -> tuple[int, str, str]:
    body = None
    headers = {}
    if data is not None:
        body = urllib.parse.urlencode(data).encode()
        headers["Content-Type"] = "application/x-www-form-urlencoded"
    req = urllib.request.Request(BASE + path, data=body, headers=headers, method=method)
    try:
        with opener.open(req) as resp:
            return resp.status, resp.geturl(), resp.read().decode("utf-8", "replace")
    except urllib.error.HTTPError as err:
        return err.code, BASE + path, err.read().decode("utf-8", "replace")


def csrf(page: str) -> str:
    match = re.search(r'name="_csrf"\s+value="([^"]+)"', page)
    if not match:
        match = re.search(r'name="_csrf"\s+value=\'([^\']+)\'', page)
    if not match:
        raise SystemExit("No CSRF token in page:\n" + page[:800])
    return html.unescape(match.group(1))


def expect(cond: bool, message: str) -> None:
    if not cond:
        raise SystemExit(message)


status, url, page = fetch("/actuator/health")
expect(status == 200 and "UP" in page, f"health failed: {status} {page[:200]}")

status, url, page = fetch("/setup")
expect(status == 200 and "Create the first administrator" in page, f"setup page missing: {status} {url}")
token = csrf(page)
status, url, page = fetch(
    "/setup",
    {
        "_csrf": token,
        "token": TOKEN,
        "username": USERNAME,
        "email": "admin@example.test",
        "firstName": "Boot",
        "lastName": "Strap",
        "password": PASSWORD,
    },
)
expect(status == 200 and ("/login" in url or "Sign in" in page), f"setup post failed: {status} {url}\n{page[:500]}")

status, url, page = fetch("/login")
expect(status == 200 and "Sign in" in page, f"login page missing: {status} {url}")
token = csrf(page)
status, url, page = fetch("/login", {"_csrf": token, "username": USERNAME, "password": PASSWORD})
expect("/account" in url or "Your account" in page, f"login failed: {status} {url}\n{page[:500]}")

status, url, page = fetch("/account")
expect(status == 200 and USERNAME in page and "Admin" in page, f"account failed: {status}\n{page[:500]}")

status, url, page = fetch("/admin")
expect(status == 200 and "Users" in page, f"admin failed: {status}")

status, url, page = fetch("/admin/sp")
expect(status == 200, f"sp admin failed: {status}")
token = csrf(page)
status, url, page = fetch(
    "/admin/sp",
    {
        "_csrf": token,
        "entityId": "https://app.example.test/metadata",
        "displayName": "Demo App",
        "acs": "https://app.example.test/acs",
        "slo": "https://app.example.test/slo",
    },
)
expect(status == 200, f"register SP failed: {status} {page[:400]}")

status, url, page = fetch("/account")
expect("Demo App" in page, f"account missing SP: {page[:400]}")

status, url, page = fetch("/saml2/idp/initiate?sp=" + urllib.parse.quote("https://app.example.test/metadata", safe=""))
expect(status == 200 and "SAMLResponse" in page, f"IdP-initiated SSO failed: {status} {page[:400]}")

status, url, page = fetch("/saml2/idp/metadata")
expect(status == 200 and "IDPSSODescriptor" in page, f"metadata failed: {status}")

print("SMOKE OK: setup, login, admin, SP registry, metadata, IdP-initiated SSO")
