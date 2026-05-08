#!/usr/bin/env bash
set -euo pipefail

APP_PORT="${APP_PORT:-8080}"
SITE_NAME="${SITE_NAME:-chatapp}"
NGINX_AVAILABLE="/etc/nginx/sites-available/${SITE_NAME}.conf"
NGINX_ENABLED="/etc/nginx/sites-enabled/${SITE_NAME}.conf"
CERTBOT_WEBROOT="/var/www/certbot"

if [[ -z "${DOMAIN_NAME:-}" ]]; then
  echo "ERROR: DOMAIN_NAME is required. Example: DOMAIN_NAME=example.com CERTBOT_EMAIL=admin@example.com bash scripts/setup_nginx.sh" >&2
  exit 1
fi

if [[ "${DOMAIN_NAME}" == http://* || "${DOMAIN_NAME}" == https://* || "${DOMAIN_NAME}" == */* ]]; then
  echo "ERROR: DOMAIN_NAME must be a hostname only, for example chat.fredmaina.com. Do not include http:// or https://." >&2
  exit 1
fi

if [[ -z "${CERTBOT_EMAIL:-}" ]]; then
  echo "ERROR: CERTBOT_EMAIL is required. Example: DOMAIN_NAME=example.com CERTBOT_EMAIL=admin@example.com bash scripts/setup_nginx.sh" >&2
  exit 1
fi

if ! [[ "${APP_PORT}" =~ ^[0-9]+$ ]]; then
  echo "ERROR: APP_PORT must be a number. Current value: ${APP_PORT}" >&2
  exit 1
fi

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

install_packages() {
  local packages_to_install=()

  for package in nginx certbot python3-certbot-nginx; do
    if ! package_installed "${package}"; then
      packages_to_install+=("${package}")
    fi
  done

  if [[ "${#packages_to_install[@]}" -eq 0 ]]; then
    echo "Nginx and Certbot packages are already installed."
    return
  fi

  echo "Installing missing packages: ${packages_to_install[*]}"
  run_sudo apt-get update
  run_sudo env DEBIAN_FRONTEND=noninteractive apt-get install -y "${packages_to_install[@]}"
}

ensure_site_enabled() {
  run_sudo mkdir -p /etc/nginx/sites-available /etc/nginx/sites-enabled "${CERTBOT_WEBROOT}"

  if [[ -L "${NGINX_ENABLED}" ]]; then
    local current_target
    current_target="$(readlink "${NGINX_ENABLED}")"
    if [[ "${current_target}" != "${NGINX_AVAILABLE}" ]]; then
      echo "Updating enabled site symlink: ${NGINX_ENABLED}"
      run_sudo ln -sfn "${NGINX_AVAILABLE}" "${NGINX_ENABLED}"
    fi
  elif [[ -e "${NGINX_ENABLED}" ]]; then
    echo "ERROR: ${NGINX_ENABLED} exists and is not a symlink. Move it aside before rerunning this script." >&2
    exit 1
  else
    echo "Enabling Nginx site: ${SITE_NAME}"
    run_sudo ln -s "${NGINX_AVAILABLE}" "${NGINX_ENABLED}"
  fi
}

disable_stock_default_site() {
  local default_site="/etc/nginx/sites-enabled/default"

  if [[ -L "${default_site}" ]]; then
    echo "Disabling stock Nginx default site so raw-IP requests use the managed reject server."
    run_sudo rm -f "${default_site}"
  fi
}

write_if_changed() {
  local target_file="$1"
  local temp_file
  temp_file="$(mktemp)"
  cat > "${temp_file}"

  if run_sudo test -f "${target_file}" && run_sudo cmp -s "${temp_file}" "${target_file}"; then
    rm -f "${temp_file}"
    echo "Nginx config is already up to date: ${target_file}"
    return 1
  fi

  echo "Writing Nginx config: ${target_file}"
  run_sudo install -m 0644 "${temp_file}" "${target_file}"
  rm -f "${temp_file}"
  return 0
}

render_http_bootstrap_config() {
  cat <<EOF
server {
    listen 80 default_server;
    listen [::]:80 default_server;
    server_name _;
    return 444;
}

server {
    listen 80;
    listen [::]:80;
    server_name ${DOMAIN_NAME};

    location /.well-known/acme-challenge/ {
        root ${CERTBOT_WEBROOT};
    }

    location / {
        proxy_pass http://localhost:${APP_PORT};
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
EOF
}

render_https_config() {
  cat <<EOF
server {
    listen 80 default_server;
    listen [::]:80 default_server;
    server_name _;
    return 444;
}

server {
    listen 80;
    listen [::]:80;
    server_name ${DOMAIN_NAME};

    location /.well-known/acme-challenge/ {
        root ${CERTBOT_WEBROOT};
    }

    location / {
        return 301 https://\$host\$request_uri;
    }
}

server {
    listen 443 ssl http2 default_server;
    listen [::]:443 ssl http2 default_server;
    server_name _;

    ssl_certificate /etc/letsencrypt/live/${DOMAIN_NAME}/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/${DOMAIN_NAME}/privkey.pem;
    include /etc/letsencrypt/options-ssl-nginx.conf;
    ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem;

    return 444;
}

server {
    listen 443 ssl http2;
    listen [::]:443 ssl http2;
    server_name ${DOMAIN_NAME};

    ssl_certificate /etc/letsencrypt/live/${DOMAIN_NAME}/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/${DOMAIN_NAME}/privkey.pem;
    include /etc/letsencrypt/options-ssl-nginx.conf;
    ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem;

    location / {
        proxy_pass http://localhost:${APP_PORT};
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
EOF
}

test_and_reload_nginx() {
  echo "Validating Nginx configuration..."
  run_sudo nginx -t

  echo "Reloading Nginx..."
  if run_sudo systemctl is-active --quiet nginx; then
    run_sudo systemctl reload nginx
  else
    run_sudo systemctl restart nginx
  fi
}

certificate_exists() {
  run_sudo test -f "/etc/letsencrypt/live/${DOMAIN_NAME}/fullchain.pem" \
    && run_sudo test -f "/etc/letsencrypt/live/${DOMAIN_NAME}/privkey.pem"
}

install_packages
run_sudo systemctl enable nginx
disable_stock_default_site

if certificate_exists; then
  echo "SSL certificate already exists for ${DOMAIN_NAME}."
else
  echo "SSL certificate not found for ${DOMAIN_NAME}; installing temporary HTTP config for certificate issuance."
  render_http_bootstrap_config | write_if_changed "${NGINX_AVAILABLE}" || true
  ensure_site_enabled
  test_and_reload_nginx

  echo "Requesting SSL certificate for ${DOMAIN_NAME}."
  run_sudo certbot certonly \
    --nginx \
    --non-interactive \
    --agree-tos \
    --email "${CERTBOT_EMAIL}" \
    --domains "${DOMAIN_NAME}" \
    --keep-until-expiring \
    --expand
fi

render_https_config | write_if_changed "${NGINX_AVAILABLE}" || true
ensure_site_enabled
test_and_reload_nginx

echo "Renewing certificates when due."
run_sudo certbot renew --quiet

test_and_reload_nginx

echo "Nginx HTTPS setup complete for ${DOMAIN_NAME}; proxying to localhost:${APP_PORT}."
