# Zindigi Corporate — Server Deploy

Repo: https://github.com/rizwan-devv/ZindigiCorporate  
Branch: **`dev`**

Ports (do not collide with DFS Corporate `8050` / `8060`):

| Service | Host port | Container |
|---------|-----------|-----------|
| Backend API | **8150** | 8090 |
| Frontend portal | **8160** | 80 |

## DFS Corporate env (reuse same MySQL)

Copy these from the live DFS stack (`DFS-Corporate/docker-compose.yml`):

```env
MYSQL_HOST=172.17.0.1
MYSQL_PORT=3306
MYSQL_DATABASE=dfs_corporate
MYSQL_USER=dfs
MYSQL_PASSWORD=change-me
```

Zindigi is configured to use **the same database** (`dfs_corporate`).  
Branding / JWT / frontend URLs stay Zindigi-specific.

Other DFS env values you usually copy as-is (APIs):

```env
DFS_ACCOUNT_API_ENABLED=true
DFS_ACCOUNT_API_BASE_URL=http://46.225.160.93:18001
DFS_TXN_API_BASE_URL=http://46.225.160.93:18009/transactions
DFS_APP_API_BASE_URL=http://46.225.160.93:18002/app
DFS_PORTAL_API_CHANNEL=COP
DFS_PORTAL_API_ENABLED=false
CORPORATE_PORTAL_API_KEY=
DFS_CMS_API_ENABLED=false
DFS_CMS_PORTAL_BASE_URL=http://46.224.146.158:7070
DFS_CMS_APP_BASE_URL=http://46.224.146.158:8016
```

Mail (same SMTP as DFS if desired):

```env
MAIL_ENABLED=true
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USER=<same-as-dfs>
MAIL_PASS=<same-as-dfs>
MAIL_FROM=<same-as-dfs>
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS=true
```

## First-time server setup

```bash
# 1) Clone
sudo mkdir -p /opt/zindigi-corporate
sudo chown "$USER":"$USER" /opt/zindigi-corporate
git clone -b dev https://github.com/rizwan-devv/ZindigiCorporate.git /opt/zindigi-corporate
cd /opt/zindigi-corporate

# 2) Edit docker-compose.yml if server IP / secrets differ
#    - FRONTEND_BASE_URL, VITE_API_BASE, APP_CORS_ORIGINS
#    - MYSQL_* (must match DFS)
#    - JWT_SECRET (use a NEW secret for Zindigi — do not copy DFS JWT)

# 3) Ensure MySQL allows Docker bridge host (same as DFS)
#    Database dfs_corporate must already exist (created by DFS).

# 4) Build & start
docker compose up -d --build

# 5) Check
docker compose ps
curl -sS http://127.0.0.1:8150/actuator/health || true
# Portal: http://<server-ip>:8160
```

Flyway will apply only **new** migrations (e.g. `V27__zindigi_brand.sql`) on the shared DB.  
Older migrations (`V1`–`V26`) must match DFS checksums — do not edit them.

## Redeploy / update

```bash
cd /opt/zindigi-corporate
export DEPLOY_BRANCH=dev
bash scripts/deploy-server.sh
```

Or manually:

```bash
cd /opt/zindigi-corporate
git fetch origin
git checkout dev
git pull origin dev
docker compose build --no-cache
docker compose up -d --force-recreate
docker compose ps
```

## Local run (shared DFS DB)

```powershell
$env:MYSQL_HOST="localhost"
$env:MYSQL_PORT="3306"
$env:MYSQL_DATABASE="dfs_corporate"
$env:MYSQL_USER="dfs"
$env:MYSQL_PASSWORD="change-me"

cd D:\Karsaaz\Zindigi-Corporate\backend
mvn spring-boot:run

cd D:\Karsaaz\Zindigi-Corporate\frontend
npm install
npm run dev
```

Admin (shared DB seed from DFS): `admin@dfscorporate.local` / `Admin@123`  
(Zindigi seed only creates `admin@zindigicorp.local` if that email is missing.)

## Side-by-side with DFS

| | DFS Corporate | Zindigi Corporate |
|--|---------------|-------------------|
| Repo path (example) | `/opt/dfs-corporate` | `/opt/zindigi-corporate` |
| Backend | `:8050` | `:8150` |
| Frontend | `:8060` | `:8160` |
| MySQL DB | `dfs_corporate` | **same** `dfs_corporate` |
| Deep link | `dfscorporate://kyc` | `zindigicorp://kyc` |
