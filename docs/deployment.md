# Deployment: EC2, Nginx, and HTTPS

This project can be deployed to an Ubuntu-based EC2 instance with the Spring Boot app listening locally on `localhost:8080` and Nginx serving public HTTPS traffic.

## How It Works

The deployment has three moving parts:

- GitHub Actions builds the Spring Boot jar, copies it to the EC2 instance, creates or updates a systemd service, and restarts the app.
- `scripts/setup_nginx.sh` installs and configures Nginx, obtains or renews a Let's Encrypt certificate with Certbot, and reverse-proxies public traffic to `http://localhost:8080`.
- `scripts/setup_postgres.sh` creates a local PostgreSQL Docker container only when external database secrets are not provided.
- `scripts/setup_redis.sh` creates a local Redis Docker container only when external Redis secrets are not provided.

The public request flow is:

```text
Browser -> HTTPS Nginx -> http://localhost:8080 -> Spring Boot app
```

HTTP requests are redirected to HTTPS after the certificate is available.

Requests sent to the raw server IP address or an unexpected host name are rejected by the managed Nginx default server. Only requests for `DOMAIN_NAME` are redirected or proxied.

## Required GitHub Secrets

Configure these secrets in the GitHub repository before using the deployment workflow:

| Secret | Purpose |
| --- | --- |
| `EC2_HOST` | Public DNS name or IP address of the EC2 instance. |
| `EC2_USER` | SSH username on the Ubuntu instance, for example `ubuntu`. |
| `EC2_SSH_PRIVATE_KEY` | Private SSH key that can log in as `EC2_USER`. Do not commit this key. |
| `DOMAIN_NAME` | Public domain that points to the EC2 instance, for example `api.example.com`. Use the hostname only; do not include `http://` or `https://`. |
| `CERTBOT_EMAIL` | Email used by Certbot for Let's Encrypt registration and renewal notices. |
| `APP_PORT` | Optional local app port. Use `8080`; if omitted in the workflow, it defaults to `8080`. |

Optional database secrets:

| Secret | Purpose |
| --- | --- |
| `DB_HOST` | External PostgreSQL host, such as an RDS endpoint. |
| `DB_PORT` | External PostgreSQL port, usually `5432`. |
| `DB_NAME` | External PostgreSQL database name. |
| `DB_USERNAME` | External PostgreSQL username. |
| `DB_PASSWORD` | External PostgreSQL password. |

If all five database secrets are provided, deployment uses the external database and does not create a local PostgreSQL container. If any of them are missing, deployment creates or reuses a local PostgreSQL Docker container.

Optional Redis secrets:

| Secret | Purpose |
| --- | --- |
| `REDIS_HOST` | External Redis host. |
| `REDIS_PORT` | External Redis port, usually `6379`. |
| `REDIS_PASSWORD` | External Redis password. |

If all three Redis secrets are provided, deployment uses the external Redis instance and does not create a local Redis container. If any of them are missing, deployment creates or reuses a local Redis Docker container.

The application itself also needs its normal runtime configuration on the EC2 host. The systemd service loads optional application environment variables from:

```bash
/home/<EC2_USER>/chatapp/.env
```

Add required non-database application secrets there:

```bash
JWT_SECRET=...
GOOGLE_CLIENT_ID=...
GOOGLE_SECRET_ID=...
GOOGLE_REDIRECT_URI=...
```

Deployment validates this file before restarting the app. If any required key is missing or empty, the workflow fails before starting the service and prints only the missing key names, not secret values.

Do not commit private keys, database passwords, JWT secrets, OAuth secrets, or Redis passwords.

Database connection values are written by deployment to:

```bash
/opt/myapp/.env
```

The file is created with `600` permissions and contains both generic database variables and Spring Boot datasource variables:

```bash
DB_HOST=127.0.0.1
DB_PORT=5432
DB_NAME=myapp
DB_USERNAME=myapp_user
DB_PASSWORD=<generated-secure-password>
SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:5432/myapp
SPRING_DATASOURCE_USERNAME=myapp_user
SPRING_DATASOURCE_PASSWORD=<generated-secure-password>
```

Do not print this file in CI logs. To confirm the database file exists without showing values, list only the keys:

```bash
sudo awk -F= '/^[A-Z_]+=/{print $1}' /opt/myapp/.env
```

Redis connection values are written by deployment to:

```bash
/opt/myapp/redis.env
```

For local Redis, the file contains:

```bash
REDIS_HOST=127.0.0.1
REDIS_PORT=6379
REDIS_PASSWORD=<generated-secure-password>
```

Check only the keys without printing values:

```bash
sudo awk -F= '/^[A-Z_]+=/{print $1}' /opt/myapp/redis.env
```

## Manual Nginx Setup

SSH into the EC2 instance, make sure this repository or the deployment bundle is present, and run:

```bash
DOMAIN_NAME=example.com CERTBOT_EMAIL=admin@example.com APP_PORT=8080 bash scripts/setup_nginx.sh
```

`DOMAIN_NAME` and `CERTBOT_EMAIL` are required. `APP_PORT` defaults to `8080`.

Before running the script, confirm:

- The EC2 security group allows inbound TCP `80` and `443`.
- The domain has an `A` or `CNAME` record pointing to the EC2 instance.
- The app is running locally on the EC2 instance.

Use `DOMAIN_NAME=chat.fredmaina.com`, not `DOMAIN_NAME=https://chat.fredmaina.com`.

Check the local app:

```bash
curl http://localhost:8080
```

Check local app, PostgreSQL, and Redis health:

```bash
bash scripts/check_app_health.sh http://localhost:8080/actuator/health
```

Test the public HTTPS endpoint:

```bash
curl https://your-domain.com
```

Check public HTTPS app, PostgreSQL, and Redis health:

```bash
bash scripts/check_app_health.sh https://your-domain.com/actuator/health
```

## Database Modes

The deployment supports two database modes.

External database mode is used when these GitHub Secrets are all present:

```text
DB_HOST
DB_PORT
DB_NAME
DB_USERNAME
DB_PASSWORD
```

In this mode, GitHub Actions writes those values to `/opt/myapp/.env`, derives the Spring Boot datasource settings, and skips local PostgreSQL container creation.

Local PostgreSQL mode is used when any required database secret is missing. In this mode, `scripts/setup_postgres.sh`:

- Installs Docker if it is missing.
- Creates the Docker volume `myapp-postgres-data` if it does not exist.
- Creates the Docker container `myapp-postgres` if it does not exist.
- Binds PostgreSQL to `127.0.0.1:5432`.
- Generates a secure password with `openssl rand -hex 32` when no existing local password is present.
- Stores local DB config in `/opt/myapp/.env`.
- Starts the existing container if it is stopped.

The local PostgreSQL volume preserves database data across deployments and container restarts.

Do not delete the PostgreSQL Docker volume unless you intentionally want to delete the database data.

Run the local PostgreSQL setup manually:

```bash
bash scripts/setup_postgres.sh
```

## Redis Modes

The deployment supports two Redis modes.

External Redis mode is used when these GitHub Secrets are all present:

```text
REDIS_HOST
REDIS_PORT
REDIS_PASSWORD
```

In this mode, GitHub Actions writes those values to `/opt/myapp/redis.env` and skips local Redis container creation.

Local Redis mode is used when any required Redis secret is missing. In this mode, `scripts/setup_redis.sh`:

- Installs Docker if it is missing.
- Creates the Docker volume `myapp-redis-data` if it does not exist.
- Creates the Docker container `myapp-redis` if it does not exist.
- Binds Redis to `127.0.0.1:6379`.
- Generates a secure password with `openssl rand -hex 32` when no existing local password is present.
- Stores local Redis config in `/opt/myapp/redis.env`.
- Starts the existing container if it is stopped.

Run the local Redis setup manually:

```bash
bash scripts/setup_redis.sh
```

## Idempotency

The Nginx setup script is safe to run during every deployment:

- It checks whether `nginx`, `certbot`, and `python3-certbot-nginx` are already installed before installing packages.
- It writes one managed site config at `/etc/nginx/sites-available/chatapp.conf`.
- It enables the site with a single symlink at `/etc/nginx/sites-enabled/chatapp.conf`.
- It compares the desired Nginx config with the existing file and only rewrites it when content changed.
- It checks whether the certificate already exists before requesting one.
- It uses `certbot renew --quiet`, which only renews certificates that are due.
- It always runs `sudo nginx -t` before reloading or restarting Nginx.
- If the app's Nginx config is missing, outdated, or manually changed, the script replaces it with the expected config.
- It disables the stock Ubuntu Nginx default site symlink so the managed default server handles raw-IP requests.
- It rejects raw-IP and unexpected-host requests with Nginx `444` responses.

If another unrelated Nginx config on the server is broken, `sudo nginx -t` will fail and the script will not reload Nginx. Fix the broken Nginx config and rerun the script.

The PostgreSQL setup script is also safe to run during every deployment:

- It installs Docker only when Docker is missing.
- It creates the named Docker volume only when missing.
- It creates the named PostgreSQL container only when missing.
- It starts the existing PostgreSQL container if it is stopped.
- It reuses existing credentials from `/opt/myapp/.env`.
- It does not regenerate the local DB password when one already exists.
- It does not delete or recreate the Docker volume.

The Redis setup script is also safe to run during every deployment:

- It installs Docker only when Docker is missing.
- It creates the named Docker volume only when missing.
- It creates the named Redis container only when missing.
- It starts the existing Redis container if it is stopped.
- It reuses existing credentials from `/opt/myapp/redis.env`.
- It does not regenerate the local Redis password when one already exists.
- It binds Redis to `127.0.0.1`, not a public interface.

Flyway migrations run automatically when the Spring Boot app starts. Hibernate schema validation is enabled with `spring.jpa.hibernate.ddl-auto=validate`, so the app fails startup if the schema created by migrations no longer matches the entity models.

## HTTPS and Certbot

On a fresh host, the script first writes a temporary HTTP-only Nginx config so Nginx can start before any certificate files exist. It then requests a Let's Encrypt certificate using Certbot's Nginx plugin.

After the certificate exists, the script writes the final config:

- Port `80` redirects to HTTPS.
- Raw-IP or unexpected-host requests are rejected by the default server.
- Port `443` uses the Let's Encrypt certificate.
- Requests are proxied to `http://localhost:8080`.
- Standard reverse proxy headers are sent to the app:

```nginx
proxy_set_header Host $host;
proxy_set_header X-Real-IP $remote_addr;
proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
proxy_set_header X-Forwarded-Proto $scheme;
```

## GitHub Actions Deployment

The workflow at `.github/workflows/deploy.yml` runs on pushes to `main` and can also be started manually with `workflow_dispatch`.

It performs these steps:

1. Checks out the repository.
2. Sets up Java 21.
3. Builds the Spring Boot jar with Maven.
4. Copies `app.jar` and `setup_nginx.sh` to `/home/<EC2_USER>/chatapp` on the EC2 instance.
5. Copies `setup_postgres.sh` to the EC2 instance.
6. Installs the Java 21 runtime on the EC2 instance if it is missing.
7. Uses external DB secrets when all five database secrets exist.
8. Otherwise creates or reuses a local PostgreSQL Docker container.
9. Uses external Redis secrets when all three Redis secrets exist.
10. Otherwise creates or reuses a local Redis Docker container.
11. Validates the required server-side app environment keys without printing values.
12. Validates the required datasource and Redis environment keys without printing values.
13. Creates or updates a systemd service named `chatapp`.
14. Restarts the app with `SERVER_PORT=8080`.
15. Checks that the service is active without printing service logs.
16. Runs the local actuator health check and requires app, PostgreSQL, and Redis health to be `UP`.
17. Runs the idempotent Nginx and HTTPS setup script.
18. Runs the public HTTPS actuator health check and requires app, PostgreSQL, and Redis health to be `UP`.

The workflow assumes the EC2 user can run `sudo` for service and Nginx management.

The workflow intentionally does not print systemd logs or environment file contents. If startup fails, it reports the failure and tells you which server-side command to run manually over SSH.

## Troubleshooting

Validate Nginx configuration:

```bash
sudo nginx -t
```

Check Nginx status:

```bash
sudo systemctl status nginx
```

Reload Nginx after a valid config change:

```bash
sudo systemctl reload nginx
```

List Certbot certificates:

```bash
sudo certbot certificates
```

Inspect Nginx logs:

```bash
sudo journalctl -u nginx
```

Check the app service:

```bash
sudo systemctl status chatapp
sudo journalctl -u chatapp -f
```

Check the app locally:

```bash
curl http://localhost:8080
```

Check HTTPS publicly:

```bash
curl https://your-domain.com
```

Check actuator health publicly:

```bash
curl https://your-domain.com/actuator/health
```

Check that raw-IP HTTP requests are rejected:

```bash
curl -I http://<ec2-public-ip>
```

Check PostgreSQL containers and logs:

```bash
docker ps
docker ps -a
docker logs myapp-postgres
docker volume ls
docker inspect myapp-postgres
sudo awk -F= '/^[A-Z_]+=/{print $1}' /opt/myapp/.env
```

Connect to the local PostgreSQL container:

```bash
docker exec -it myapp-postgres psql -U myapp_user -d myapp
```

Safely restart the local PostgreSQL container:

```bash
docker restart myapp-postgres
```

Check Redis containers and logs:

```bash
docker ps
docker ps -a
docker logs myapp-redis
docker volume ls
docker inspect myapp-redis
sudo awk -F= '/^[A-Z_]+=/{print $1}' /opt/myapp/redis.env
```

Connect to the local Redis container:

```bash
docker exec -it myapp-redis redis-cli -a "$(sudo awk -F= '$1 == "REDIS_PASSWORD" {print $2}' /opt/myapp/redis.env)" ping
```

Safely restart the local Redis container:

```bash
docker restart myapp-redis
```

Common issues:

- `nginx -t` fails: inspect the error output and fix any invalid Nginx config before reloading.
- `DOMAIN_NAME` validation fails: store only `chat.fredmaina.com`, not `https://chat.fredmaina.com`.
- Certbot cannot issue a certificate: confirm DNS points to the EC2 instance and ports `80` and `443` are open.
- Health check fails for `db`: check database credentials, local Postgres container state, and Flyway migration errors in `sudo journalctl -u chatapp -n 100 --no-pager`.
- Health check fails for `redis`: check Redis credentials and local Redis container state.
- HTTPS works but the app returns errors: check `sudo journalctl -u chatapp -n 100 --no-pager` and confirm the `.env` files contain the required Spring, database, Redis, JWT, and OAuth settings.
- Public requests time out: verify the EC2 security group, instance firewall, Nginx status, and DNS records.
- PostgreSQL container is not running: check `docker ps -a`, `docker logs myapp-postgres`, and `sudo systemctl status docker`.
- Local DB data seems missing: verify the container still uses the `myapp-postgres-data` volume and confirm the volume was not deleted.
- Redis container is not running: check `docker ps -a`, `docker logs myapp-redis`, and `sudo systemctl status docker`.

## Assumptions

- The EC2 instance runs Ubuntu.
- The app listens locally on port `8080`.
- The deployment user has passwordless `sudo`.
- Runtime application secrets are stored on the EC2 instance, not in the repository.

## Secrets Checklist

Add these GitHub repository secrets:

```text
EC2_HOST
EC2_USER
EC2_SSH_PRIVATE_KEY
DOMAIN_NAME
CERTBOT_EMAIL
```

Optional GitHub repository secrets:

```text
APP_PORT
DB_HOST
DB_PORT
DB_NAME
DB_USERNAME
DB_PASSWORD
REDIS_HOST
REDIS_PORT
REDIS_PASSWORD
```

If you use an external PostgreSQL database, add all five `DB_*` secrets. If you want the deployment to create a local PostgreSQL container, leave all `DB_*` secrets unset.

If you use an external Redis instance, add all three `REDIS_*` secrets. If you want the deployment to create a local Redis container, leave all `REDIS_*` secrets unset.

Put these server-only app secrets in `/home/<EC2_USER>/chatapp/.env` on the EC2 instance:

```bash
JWT_SECRET=...
GOOGLE_CLIENT_ID=...
GOOGLE_SECRET_ID=...
GOOGLE_REDIRECT_URI=...
```

Create and secure the file like this, replacing `ubuntu` with your EC2 user:

```bash
sudo mkdir -p /home/ubuntu/chatapp
sudo nano /home/ubuntu/chatapp/.env
sudo chown ubuntu:ubuntu /home/ubuntu/chatapp/.env
chmod 600 /home/ubuntu/chatapp/.env
```

For your domain secret, use:

```text
DOMAIN_NAME=chat.fredmaina.com
```

Do not use:

```text
DOMAIN_NAME=https://chat.fredmaina.com
```

## Updated Expected Outcome

After this work is complete:

- The EC2 instance should serve the app through Nginx.
- Public traffic should use HTTPS.
- Nginx should reverse-proxy traffic to `localhost:8080`.
- Raw-IP requests should be rejected instead of proxied.
- Certbot should manage SSL certificates.
- GitHub Actions should deploy the app easily.
- Running the deployment multiple times should be safe.
- If external DB credentials are provided, the app should use the external database.
- If external DB credentials are missing, the deployment should create a local PostgreSQL Docker container.
- The local PostgreSQL container should persist data using a Docker volume.
- If external Redis credentials are missing, the deployment should create a local Redis Docker container.
- Redis should bind only to `127.0.0.1` and use a generated password.
- The app should automatically receive the correct database environment variables.
- The app should automatically receive the correct Redis environment variables.
