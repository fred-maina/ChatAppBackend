#!/usr/bin/env bash
set -euo pipefail

APP_CONFIG_DIR="${APP_CONFIG_DIR:-/opt/myapp}"
APP_ENV_FILE="${APP_ENV_FILE:-${APP_CONFIG_DIR}/.env}"
APP_POSTGRES_CONTAINER="${APP_POSTGRES_CONTAINER:-myapp-postgres}"
APP_POSTGRES_VOLUME="${APP_POSTGRES_VOLUME:-myapp-postgres-data}"
POSTGRES_IMAGE="${POSTGRES_IMAGE:-postgres:16-alpine}"
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-myapp}"
DB_USERNAME="${DB_USERNAME:-myapp_user}"

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

  if run_sudo test -f "${APP_ENV_FILE}"; then
    run_sudo awk -F= -v key="${key}" '$1 == key { sub(/^[^=]*=/, ""); print; exit }' "${APP_ENV_FILE}"
  fi
}

ensure_db_password() {
  local existing_password
  existing_password="$(read_existing_env_value DB_PASSWORD || true)"

  if [[ -n "${DB_PASSWORD:-}" ]]; then
    echo "${DB_PASSWORD}"
  elif [[ -n "${existing_password}" ]]; then
    echo "${existing_password}"
  else
    openssl rand -hex 32
  fi
}

write_env_file() {
  local db_password="$1"
  local temp_file
  temp_file="$(mktemp)"

  cat > "${temp_file}" <<EOF
DB_HOST=${DB_HOST}
DB_PORT=${DB_PORT}
DB_NAME=${DB_NAME}
DB_USERNAME=${DB_USERNAME}
DB_PASSWORD=${db_password}
SPRING_DATASOURCE_URL=jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
SPRING_DATASOURCE_USERNAME=${DB_USERNAME}
SPRING_DATASOURCE_PASSWORD=${db_password}
EOF

  run_sudo mkdir -p "${APP_CONFIG_DIR}"

  if run_sudo test -f "${APP_ENV_FILE}" && run_sudo cmp -s "${temp_file}" "${APP_ENV_FILE}"; then
    rm -f "${temp_file}"
    echo "Database environment file is already up to date: ${APP_ENV_FILE}"
    run_sudo chmod 600 "${APP_ENV_FILE}"
    return
  fi

  echo "Writing database environment file: ${APP_ENV_FILE}"
  run_sudo install -m 0600 "${temp_file}" "${APP_ENV_FILE}"
  rm -f "${temp_file}"
}

ensure_volume() {
  if run_sudo docker volume inspect "${APP_POSTGRES_VOLUME}" >/dev/null 2>&1; then
    echo "PostgreSQL volume already exists: ${APP_POSTGRES_VOLUME}"
  else
    echo "Creating PostgreSQL volume: ${APP_POSTGRES_VOLUME}"
    run_sudo docker volume create "${APP_POSTGRES_VOLUME}" >/dev/null
  fi
}

container_exists() {
  run_sudo docker inspect "${APP_POSTGRES_CONTAINER}" >/dev/null 2>&1
}

container_running() {
  [[ "$(run_sudo docker inspect -f '{{.State.Running}}' "${APP_POSTGRES_CONTAINER}" 2>/dev/null || true)" == "true" ]]
}

ensure_container() {
  local db_password="$1"

  if container_exists; then
    echo "PostgreSQL container already exists: ${APP_POSTGRES_CONTAINER}"
    if container_running; then
      echo "PostgreSQL container is already running."
    else
      echo "Starting existing PostgreSQL container."
      run_sudo docker start "${APP_POSTGRES_CONTAINER}" >/dev/null
    fi
    return
  fi

  echo "Creating PostgreSQL container: ${APP_POSTGRES_CONTAINER}"
  run_sudo docker run -d \
    --name "${APP_POSTGRES_CONTAINER}" \
    --restart unless-stopped \
    -e POSTGRES_DB="${DB_NAME}" \
    -e POSTGRES_USER="${DB_USERNAME}" \
    -e POSTGRES_PASSWORD="${db_password}" \
    -p "127.0.0.1:${DB_PORT}:5432" \
    -v "${APP_POSTGRES_VOLUME}:/var/lib/postgresql/data" \
    "${POSTGRES_IMAGE}" >/dev/null
}

wait_for_postgres() {
  local max_attempts="${POSTGRES_READY_MAX_ATTEMPTS:-30}"
  local sleep_seconds="${POSTGRES_READY_SLEEP_SECONDS:-2}"
  local attempt=1

  echo "Waiting for PostgreSQL to accept connections..."
  while [[ "${attempt}" -le "${max_attempts}" ]]; do
    if run_sudo docker exec "${APP_POSTGRES_CONTAINER}" pg_isready -U "${DB_USERNAME}" -d "${DB_NAME}" >/dev/null 2>&1; then
      echo "PostgreSQL is ready."
      return
    fi

    sleep "${sleep_seconds}"
    attempt=$((attempt + 1))
  done

  echo "ERROR: PostgreSQL did not become ready in time." >&2
  exit 1
}

if ! package_installed openssl; then
  run_sudo apt-get update
  run_sudo env DEBIAN_FRONTEND=noninteractive apt-get install -y openssl
fi

DB_PASSWORD="$(ensure_db_password)"

install_docker_if_missing
ensure_docker_running
ensure_volume
write_env_file "${DB_PASSWORD}"
ensure_container "${DB_PASSWORD}"
wait_for_postgres

echo "Local PostgreSQL setup complete. Credentials are stored in ${APP_ENV_FILE}."
