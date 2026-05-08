#!/usr/bin/env bash
set -euo pipefail

APP_CONFIG_DIR="${APP_CONFIG_DIR:-/opt/myapp}"
REDIS_ENV_FILE="${REDIS_ENV_FILE:-${APP_CONFIG_DIR}/redis.env}"
APP_REDIS_CONTAINER="${APP_REDIS_CONTAINER:-myapp-redis}"
APP_REDIS_VOLUME="${APP_REDIS_VOLUME:-myapp-redis-data}"
REDIS_IMAGE="${REDIS_IMAGE:-redis:7-alpine}"
REDIS_HOST="${REDIS_HOST:-127.0.0.1}"
REDIS_PORT="${REDIS_PORT:-6379}"

run_sudo() {
  if [[ "${EUID}" -eq 0 ]]; then
    "$@"
  else
    sudo "$@"
  fi
}

package_installed() {
  dpkg-query -W -f='${Status}' "$1" 2>/dev/null | grep -q "install ok installed"
}

install_docker_if_missing() {
  if command -v docker >/dev/null 2>&1; then
    echo "Docker is already installed."
    return
  fi

  echo "Installing Docker..."
  run_sudo apt-get update
  run_sudo env DEBIAN_FRONTEND=noninteractive apt-get install -y docker.io
}

ensure_docker_running() {
  run_sudo systemctl enable docker
  if ! run_sudo systemctl is-active --quiet docker; then
    run_sudo systemctl start docker
  fi
}

read_existing_env_value() {
  local key="$1"

  if run_sudo test -f "${REDIS_ENV_FILE}"; then
    run_sudo awk -F= -v key="${key}" '$1 == key { sub(/^[^=]*=/, ""); print; exit }' "${REDIS_ENV_FILE}"
  fi
}

ensure_redis_password() {
  local existing_password
  existing_password="$(read_existing_env_value REDIS_PASSWORD || true)"

  if [[ -n "${REDIS_PASSWORD:-}" ]]; then
    echo "${REDIS_PASSWORD}"
  elif [[ -n "${existing_password}" ]]; then
    echo "${existing_password}"
  else
    openssl rand -hex 32
  fi
}

write_env_file() {
  local redis_password="$1"
  local temp_file
  temp_file="$(mktemp)"

  cat > "${temp_file}" <<EOF
REDIS_HOST=${REDIS_HOST}
REDIS_PORT=${REDIS_PORT}
REDIS_PASSWORD=${redis_password}
EOF

  run_sudo mkdir -p "${APP_CONFIG_DIR}"

  if run_sudo test -f "${REDIS_ENV_FILE}" && run_sudo cmp -s "${temp_file}" "${REDIS_ENV_FILE}"; then
    rm -f "${temp_file}"
    echo "Redis environment file is already up to date: ${REDIS_ENV_FILE}"
    run_sudo chmod 600 "${REDIS_ENV_FILE}"
    return
  fi

  echo "Writing Redis environment file: ${REDIS_ENV_FILE}"
  run_sudo install -m 0600 "${temp_file}" "${REDIS_ENV_FILE}"
  rm -f "${temp_file}"
}

ensure_volume() {
  if run_sudo docker volume inspect "${APP_REDIS_VOLUME}" >/dev/null 2>&1; then
    echo "Redis volume already exists: ${APP_REDIS_VOLUME}"
  else
    echo "Creating Redis volume: ${APP_REDIS_VOLUME}"
    run_sudo docker volume create "${APP_REDIS_VOLUME}" >/dev/null
  fi
}

container_exists() {
  run_sudo docker inspect "${APP_REDIS_CONTAINER}" >/dev/null 2>&1
}

container_running() {
  [[ "$(run_sudo docker inspect -f '{{.State.Running}}' "${APP_REDIS_CONTAINER}" 2>/dev/null || true)" == "true" ]]
}

ensure_container() {
  local redis_password="$1"

  if container_exists; then
    echo "Redis container already exists: ${APP_REDIS_CONTAINER}"
    if container_running; then
      echo "Redis container is already running."
    else
      echo "Starting existing Redis container."
      run_sudo docker start "${APP_REDIS_CONTAINER}" >/dev/null
    fi
    return
  fi

  echo "Creating Redis container: ${APP_REDIS_CONTAINER}"
  run_sudo docker run -d \
    --name "${APP_REDIS_CONTAINER}" \
    --restart unless-stopped \
    -p "127.0.0.1:${REDIS_PORT}:6379" \
    -v "${APP_REDIS_VOLUME}:/data" \
    "${REDIS_IMAGE}" \
    redis-server --appendonly yes --requirepass "${redis_password}" >/dev/null
}

if ! package_installed openssl; then
  run_sudo apt-get update
  run_sudo env DEBIAN_FRONTEND=noninteractive apt-get install -y openssl
fi

REDIS_PASSWORD="$(ensure_redis_password)"

install_docker_if_missing
ensure_docker_running
ensure_volume
write_env_file "${REDIS_PASSWORD}"
ensure_container "${REDIS_PASSWORD}"

echo "Local Redis setup complete. Credentials are stored in ${REDIS_ENV_FILE}."
