#!/usr/bin/env bash
# Run on server: cd /opt/zindigi-corporate && bash scripts/deploy-server.sh
set -euo pipefail

cd "$(dirname "$0")/.."

BRANCH="${DEPLOY_BRANCH:-dev}"

echo "==> Stashing local docker-compose.yml overrides (if any)..."
git stash push -m "server-compose-$(date +%Y%m%d)" -- docker-compose.yml 2>/dev/null || true

echo "==> Pulling latest ${BRANCH}..."
git fetch origin "${BRANCH}"
git checkout "${BRANCH}"
git pull origin "${BRANCH}"

echo "==> HEAD:"
git log -1 --oneline

echo "==> Re-applying compose stash (resolve conflicts manually if needed)..."
git stash pop 2>/dev/null || true

echo "==> Building & restarting portal + backend..."
docker compose build --no-cache zindigi-corporate-frontend zindigi-corporate-backend
docker compose up -d --force-recreate zindigi-corporate-frontend zindigi-corporate-backend

echo "==> Building consumer financing app (:8170)..."
CONSUMER_API_BASE="${VITE_API_BASE:-http://46.224.146.158:8150}"
CONSUMER_PORT="${CONSUMER_PORT:-8170}"
(
  cd consumer
  npm ci
  VITE_API_BASE="${CONSUMER_API_BASE}" npm run build
)
if command -v pm2 >/dev/null 2>&1 && pm2 describe zindigi-consumer >/dev/null 2>&1; then
  echo "==> Restarting pm2 zindigi-consumer..."
  pm2 restart zindigi-consumer
elif command -v npx >/dev/null 2>&1; then
  echo "==> Starting/restarting static consumer on :${CONSUMER_PORT}..."
  # Kill any prior serve on this port, then host dist/
  fuser -k "${CONSUMER_PORT}/tcp" 2>/dev/null || true
  nohup npx --yes serve -s consumer/dist -l "${CONSUMER_PORT}" >/var/log/zindigi-consumer.log 2>&1 &
  echo "    (log: /var/log/zindigi-consumer.log)"
else
  echo "==> WARN: consumer built at consumer/dist — restart your :${CONSUMER_PORT} process manually"
fi

echo "==> Done. Verify:"
docker compose ps
echo "Portal UI:   http://<server>:8160"
echo "Backend:     http://<server>:8150/actuator/health"
echo "Consumer UI: http://<server>:${CONSUMER_PORT}"
echo "CMS status: curl -H \"Authorization: Bearer \$TOKEN\" http://127.0.0.1:8150/api/cms/cards/status"
