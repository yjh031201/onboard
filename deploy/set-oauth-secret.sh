#!/usr/bin/env bash
# 사용법: sudo set-oauth-secret.sh GOOGLE   (또는 NAVER, GITHUB, SLACK, GOOGLE_DRIVE)
# Client ID(생략 가능)와 Client Secret(화면에 안 보임)을 입력받아 /opt/onboard/config/.env에 저장하고 백엔드를 재시작한다.
set -euo pipefail
PROVIDER=${1:?GOOGLE, NAVER, GITHUB, SLACK, GOOGLE_DRIVE 중 하나를 지정하세요}
ENV_FILE=/opt/onboard/config/.env

set_env() {
  if grep -q "^$1=" "$ENV_FILE"; then
    sed -i "s|^$1=.*|$1=$2|" "$ENV_FILE"
  else
    echo "$1=$2" >> "$ENV_FILE"
  fi
}

read -rp "${PROVIDER} Client ID (이미 넣었으면 그냥 Enter): " CLIENT_ID
[ -n "$CLIENT_ID" ] && set_env "${PROVIDER}_CLIENT_ID" "$CLIENT_ID"

read -rsp "${PROVIDER} Client Secret 붙여넣기 (화면에 안 보임) 후 Enter: " SECRET; echo
[ -n "$SECRET" ] || { echo "Secret이 비어 있어서 취소했어요."; exit 1; }
set_env "${PROVIDER}_CLIENT_SECRET" "$SECRET"
unset SECRET

cp "$ENV_FILE" /opt/onboard/app/.env
cd /opt/onboard/app
docker compose -f docker-compose.prod.yml -f deploy/docker-compose.server.yml up -d backend
echo "저장하고 백엔드를 재시작했어요. 1분쯤 뒤에 다시 시도해 보세요."
