#!/usr/bin/env bash
set -euo pipefail

HEALTH_URL="${1:-}"
REQUIRE_COMPONENTS="${REQUIRE_COMPONENTS:-db,redis}"
MAX_ATTEMPTS="${MAX_ATTEMPTS:-24}"
SLEEP_SECONDS="${SLEEP_SECONDS:-5}"

if [[ -z "${HEALTH_URL}" ]]; then
  echo "ERROR: health URL is required. Example: bash scripts/check_app_health.sh http://localhost:8080/actuator/health" >&2
  exit 1
fi

if ! command -v curl >/dev/null 2>&1; then
  echo "ERROR: curl is required for health checks." >&2
  exit 1
fi

if ! command -v python3 >/dev/null 2>&1; then
  echo "ERROR: python3 is required for health response validation." >&2
  exit 1
fi

validate_health_json() {
  local response_file="$1"

  python3 - "${response_file}" "${REQUIRE_COMPONENTS}" <<'PY'
import json
import sys

response_path = sys.argv[1]
required_components = [item.strip() for item in sys.argv[2].split(",") if item.strip()]

with open(response_path, "r", encoding="utf-8") as fh:
    payload = json.load(fh)

overall_status = payload.get("status")
if overall_status != "UP":
    print(f"Health status is {overall_status!r}, expected 'UP'.")
    sys.exit(1)

components = payload.get("components") or {}
missing = []
unhealthy = []

for component in required_components:
    component_payload = components.get(component)
    if not component_payload:
        missing.append(component)
        continue

    component_status = component_payload.get("status")
    if component_status != "UP":
        unhealthy.append(f"{component}={component_status!r}")

if missing:
    print("Missing health components: " + ", ".join(missing))
    sys.exit(1)

if unhealthy:
    print("Unhealthy health components: " + ", ".join(unhealthy))
    sys.exit(1)

print("Health check passed: app=UP, " + ", ".join(f"{component}=UP" for component in required_components))
PY
}

attempt=1
response_file="$(mktemp)"
trap 'rm -f "${response_file}"' EXIT

while [[ "${attempt}" -le "${MAX_ATTEMPTS}" ]]; do
  http_status="$(curl --silent --show-error --location --max-time 10 --output "${response_file}" --write-out "%{http_code}" "${HEALTH_URL}" || true)"

  if [[ "${http_status}" == "200" ]]; then
    if validate_health_json "${response_file}"; then
      exit 0
    fi
  else
    echo "Health check attempt ${attempt}/${MAX_ATTEMPTS} returned HTTP ${http_status} for ${HEALTH_URL}."
  fi

  if [[ "${attempt}" -lt "${MAX_ATTEMPTS}" ]]; then
    sleep "${SLEEP_SECONDS}"
  fi

  attempt=$((attempt + 1))
done

echo "ERROR: health endpoint did not become healthy: ${HEALTH_URL}" >&2
exit 1
