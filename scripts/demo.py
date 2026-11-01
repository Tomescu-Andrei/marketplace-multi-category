"""Black-box API/Swagger demonstration, using only the Python standard library."""
import copy
import http.cookiejar
import json
import os
import time
import urllib.error
import urllib.request

base_url = os.environ.get("DEMO_BASE_URL", "http://backend:8080")
cookies = http.cookiejar.CookieJar()
client = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(cookies))
csrf_headers = {}


def request(path, method="GET", body=None, expected=200):
    headers = dict(csrf_headers)
    data = None
    if body is not None:
        headers["Content-Type"] = "application/json"
        data = json.dumps(body).encode()
    req = urllib.request.Request(base_url + path, data=data, headers=headers, method=method)
    try:
        response = client.open(req, timeout=30)
    except urllib.error.HTTPError as error:
        response = error
    with response:
        content = response.read()
        assert response.status == expected, (path, response.status, content.decode(errors="replace"))
        if "json" in response.headers.get("Content-Type", ""):
            result = json.loads(content)
        else:
            result = content.decode()
        print(f"{method} {path} -> {response.status}")
        return result


def renew_csrf():
    token = request("/api/v1/auth/csrf")
    csrf_headers.clear()
    csrf_headers[token["headerName"]] = token["token"]


api = request("/v3/api-docs")
request("/swagger-ui.html")
request("/swagger-ui/swagger-ui-bundle.js")
assert request("/actuator/health")["status"] == "UP"
categories = request("/api/v1/categories")
assert categories
category = request(f"/api/v1/categories/{categories[0]['id']}")
assert category["attributes"]

account = None
if "/api/v1/auth/register" in api["paths"]:
    renew_csrf()
    credentials = {"email": f"demo-{time.time_ns()}@marketplace.test", "password": "DockerDemo-2026!"}
    account = request("/api/v1/auth/register", "POST", {**credentials, "username": f"demo_{time.time_ns()}", "displayName": "Docker Demo"}, 201)
    old_session = next(cookie.value for cookie in cookies if cookie.name == "JSESSIONID")
    request("/api/v1/auth/login", "POST", {"identifier": account["username"], "password": credentials["password"]})
    session_cookie = next(cookie for cookie in cookies if cookie.name == "JSESSIONID")
    assert session_cookie.value != old_session
    assert session_cookie.has_nonstandard_attr("HttpOnly")
    assert session_cookie.get_nonstandard_attr("SameSite") == "Lax"
    print("Session cookie rotated; HttpOnly and SameSite=Lax verified")
    renew_csrf()
    assert request("/api/v1/auth/me")["id"] == account["id"]

examples = api["paths"]["/api/v1/listings"]["post"]["requestBody"]["content"]["application/json"]["examples"]
body = copy.deepcopy(next(iter(examples.values()))["value"])
if isinstance(body, str):
    body = json.loads(body)
body["title"] = "Docker demo laptop"
listing = request("/api/v1/listings", "POST", body, 201)
assert listing["title"] == body["title"]
if account:
    assert listing["sellerId"] == account["id"]
assert request(f"/api/v1/listings/{listing['id']}")["attributes"] == body["attributes"]
assert any(item["id"] == listing["id"] for item in request("/api/v1/listings"))

invalid = copy.deepcopy(body)
invalid.pop("price")
problem = request("/api/v1/listings", "POST", invalid, 400)
assert problem["code"] == "INVALID_LISTING" and problem["errors"][0]["field"] == "price"
invalid = copy.deepcopy(body)
invalid["attributes"]["year"] = "2022"
problem = request("/api/v1/listings", "POST", invalid, 400)
assert problem["errors"][0]["field"] == "attributes.year"
assert request("/api/v1/listings/9223372036854775807", expected=404)["code"] == "NOT_FOUND"

if account:
    request("/api/v1/auth/logout", "POST", expected=204)
    assert request("/api/v1/auth/me", expected=401)["code"] == "UNAUTHENTICATED"

print(f"DEMO PASSED; persisted listing ID={listing['id']}")
