#!/usr/bin/env bash
# GitHub의 배포 브랜치에 새 커밋이 있으면 받아서 다시 빌드·실행한다.
# systemd 타이머(onboard-deploy.timer)가 1분마다 실행 — 저장소 쪽 권한(웹훅/Secrets) 없이 동작한다.
set -euo pipefail

APP_DIR=/opt/onboard/app          # 저장소 clone 위치
CONFIG_DIR=/opt/onboard/config    # .env, Caddyfile, override compose (저장소 밖에 둬서 브랜치와 무관하게 유지)
BRANCH=${DEPLOY_BRANCH:-master}
LOCK=/tmp/onboard-deploy.lock

exec 9>"$LOCK"
flock -n 9 || exit 0   # 이전 빌드가 아직 돌고 있으면 건너뛴다

cd "$APP_DIR"
git fetch --quiet origin "$BRANCH"
LOCAL=$(git rev-parse HEAD)
REMOTE=$(git rev-parse "origin/$BRANCH")

if [ "$LOCAL" = "$REMOTE" ] && [ "${1:-}" != "--force" ]; then
  exit 0
fi

echo "$(date '+%F %T') deploy $LOCAL -> $REMOTE"
git reset --quiet --hard "origin/$BRANCH"

# 서버 전용 설정을 매번 덮어써서 저장소 내용과 상관없이 같은 구성으로 띄운다.
mkdir -p deploy
cp "$CONFIG_DIR/Caddyfile" deploy/Caddyfile
cp "$CONFIG_DIR/docker-compose.server.yml" deploy/docker-compose.server.yml
cp "$CONFIG_DIR/.env" .env

docker compose -f docker-compose.prod.yml -f deploy/docker-compose.server.yml up -d --build --remove-orphans
docker image prune -f >/dev/null
echo "$(date '+%F %T') deploy done $(git rev-parse --short HEAD)"
