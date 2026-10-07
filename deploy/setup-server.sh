#!/usr/bin/env bash
# Ubuntu 서버(AWS EC2 등) 최초 1회 설정. 사용법:
#   SITE_DOMAIN=xxx.duckdns.org sudo -E bash setup-server.sh
# 이 스크립트와 Caddyfile, docker-compose.server.yml, auto-deploy.sh가 같은 폴더에 있어야 한다.
set -euo pipefail

: "${SITE_DOMAIN:?SITE_DOMAIN을 지정하세요 (예: onboard.duckdns.org)}"
REPO_URL=${REPO_URL:-https://github.com/yjh031201/onboard.git}
BRANCH=${DEPLOY_BRANCH:-master}
SRC_DIR=$(cd "$(dirname "$0")" && pwd)

# 1) Docker
if ! command -v docker >/dev/null; then
  # 공식 스크립트가 아직 지원하지 않는 최신 Ubuntu면 Ubuntu 기본 패키지로 설치
  curl -fsSL https://get.docker.com | sh || {
    apt-get update -q
    DEBIAN_FRONTEND=noninteractive apt-get install -y docker.io docker-compose-v2 docker-buildx
    systemctl enable --now docker
  }
fi
usermod -aG docker ubuntu || true

# 2) 메모리가 작은 인스턴스를 위한 스왑 (빌드 중 OOM 방지)
if ! swapon --show | grep -q /swapfile; then
  fallocate -l 4G /swapfile && chmod 600 /swapfile && mkswap /swapfile && swapon /swapfile
  echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi

# 3) 서버 방화벽에서 80/443 허용 (AWS는 보통 열려 있고 보안 그룹이 막는다. Oracle 이미지는 여기서 막혀 있음)
for port in 80 443; do
  iptables -C INPUT -p tcp --dport "$port" -j ACCEPT 2>/dev/null ||
    iptables -I INPUT -p tcp --dport "$port" -j ACCEPT
done
DEBIAN_FRONTEND=noninteractive apt-get install -y iptables-persistent >/dev/null
netfilter-persistent save

# 4) 저장소와 서버 전용 설정
mkdir -p /opt/onboard/config /opt/onboard/bin
[ -d /opt/onboard/app/.git ] || git clone --branch "$BRANCH" "$REPO_URL" /opt/onboard/app
cp "$SRC_DIR/Caddyfile" "$SRC_DIR/docker-compose.server.yml" /opt/onboard/config/
install -m 755 "$SRC_DIR/auto-deploy.sh" /opt/onboard/bin/auto-deploy.sh

if [ ! -f /opt/onboard/config/.env ]; then
  rand() { openssl rand -base64 48 | tr -dc 'A-Za-z0-9' | head -c "$1"; }
  cat > /opt/onboard/config/.env <<EOF
SITE_DOMAIN=$SITE_DOMAIN
MYSQL_ROOT_PASSWORD=$(rand 32)
DB_NAME=kanban
DB_USERNAME=kanban
DB_PASSWORD=$(rand 32)
JWT_SECRET=$(rand 64)
JWT_EXPIRATION_MS=86400000
JWT_REFRESH_EXPIRATION_MS=1209600000
VITE_API_BASE_URL=https://$SITE_DOMAIN
CORS_ALLOWED_ORIGINS=https://$SITE_DOMAIN
FRONTEND_BASE_URL=https://$SITE_DOMAIN
COOKIE_SECURE=true
GOOGLE_CLIENT_ID=dummy-google-client-id
GOOGLE_CLIENT_SECRET=dummy-google-client-secret
NAVER_CLIENT_ID=dummy-naver-client-id
NAVER_CLIENT_SECRET=dummy-naver-client-secret
EOF
  chmod 600 /opt/onboard/config/.env
fi

# 5) 1분마다 새 커밋 확인 → 자동 배포
cat > /etc/systemd/system/onboard-deploy.service <<EOF
[Unit]
Description=onboard auto deploy
After=docker.service network-online.target

[Service]
Type=oneshot
Environment=DEPLOY_BRANCH=$BRANCH
ExecStart=/opt/onboard/bin/auto-deploy.sh
EOF

cat > /etc/systemd/system/onboard-deploy.timer <<'EOF'
[Unit]
Description=check GitHub for new commits every minute

[Timer]
OnBootSec=1min
OnUnitActiveSec=1min

[Install]
WantedBy=timers.target
EOF

systemctl daemon-reload
systemctl enable --now onboard-deploy.timer

# 첫 배포는 바로 실행
/opt/onboard/bin/auto-deploy.sh --force
