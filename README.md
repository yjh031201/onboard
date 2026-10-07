# 칸반보드 프로젝트

실시간 협업 칸반보드 — 4인 팀 포트폴리오 프로젝트

## 폴더 구조

```
onboard/
├── kanban-frontend/   # React + TypeScript + Tailwind CSS
└── kanban-backend/    # Spring Boot + MySQL (Docker)
```

## 사전 설치 필요한 것

| 항목 | 버전 | 확인 명령어 |
|---|---|---|
| Node.js | 20.19+ 또는 22.12+ | `node -v` |
| JDK | 21 | `java -version` |
| Docker Desktop | 최신 | 트레이 아이콘 확인 |

> Maven은 따로 설치 안 해도 됩니다. `kanban-backend`에 있는 `mvnw`(맥/리눅스) / `mvnw.cmd`(윈도우) 래퍼가 최초 실행 시 알아서 받아옵니다.

## CI

`master`로의 PR/push마다 GitHub Actions가 자동으로 돈다 (`.github/workflows/`):

- **backend-ci.yml**: `kanban-backend/**` 변경 시 `mvnw test` (H2라 Docker 불필요)
- **frontend-ci.yml**: `kanban-frontend/**` 변경 시 `npm ci && npm run lint && npm run build`

## Git 브랜치 규칙

**`master`(또는 `main`)에 직접 push 금지.** 각자 브랜치 만들어서 작업하고, PR(Pull Request)로 리뷰 후 머지합니다.

```
git checkout -b feature/브랜치이름     # 본인 작업 브랜치 생성
git add .
git commit -m "커밋 메시지"
git push origin feature/브랜치이름     # 본인 브랜치로 push
```

다 됐으면 GitHub에서 PR 열어서 팀원 리뷰 받고 머지. 브랜치 이름은 `feature/기능명`, `fix/버그명` 식으로 통일하면 편해요.

## 실행 순서

### 1. 저장소 받기

```
git clone <레포주소>
cd onboard
```

이미 받아둔 사람은 최신 상태로:

```
git pull
```

### 2. 백엔드 — DB(MySQL) 먼저 켜기

```
cd kanban-backend
docker compose up -d
```

정상 기동 확인:

```
docker ps
```

`kanban-mysql`, `kanban-redis` 둘 다 `healthy` 상태로 떠 있어야 합니다. (`starting` 상태에서 바로 다음 단계로 넘어가면 DB/Redis 연결 실패로 실행이 죽습니다 — 몇 초 기다렸다가 다시 확인)

### 3. 백엔드 실행

터미널에서:

```
./mvnw spring-boot:run        # 맥/리눅스
.\mvnw.cmd spring-boot:run    # 윈도우
```

또는 인텔리제이에서 `KanbanBackendApplication.java` 우클릭 → Run.

- 최초 실행 시에만 Maven 배포판(3.9.16)을 `~/.m2/wrapper/dists`에 내려받습니다 (PC당 1회, 이후 캐시 재사용).
- `Started KanbanBackendApplication` 로그가 뜨면 성공. `http://localhost:8080`에서 대기 중.

### 4. 프론트엔드 실행

새 터미널에서:

```
cd kanban-frontend
npm install
npm run dev
```

→ `http://localhost:5173`

`.env` 파일은 따로 안 만들어도 됩니다 (기본값이 `http://localhost:8080`으로 백엔드를 바라보게 되어 있음). 백엔드 주소를 바꿔야 하면 `.env.example`을 참고해서 `.env` 파일을 만드세요.

## 현재 구현된 기능

- **회원가입 / 로그인**: 프론트 ↔ 백엔드(`/api/auth/signup`, `/api/auth/login`) 완전히 연동됨. JWT 발급, BCrypt 비밀번호 해싱.
- **내 정보 조회**: `GET /api/auth/me` (토큰 필요)
- 로그인 상태면 화면 우측 상단에 이름/아바타 표시(클릭 시 로그아웃), 비로그인 상태면 로그인 버튼 표시
- **Refresh Token**: `/api/auth/refresh`, `/api/auth/logout` — Redis에 저장, 로그아웃 시 즉시 무효화 (백엔드 API만 있음, 프론트 연동 전)
- **프로젝트 설정**: `GET/PUT /api/settings` — 설정 페이지 "일반"(프로젝트 이름/설명) 연동됨. 수정은 OWNER/ADMIN만
- **일정(캘린더)**: `GET/POST/PUT/DELETE /api/schedules` — 대시보드 캘린더 연동됨 (카테고리/시간/색상 포함). 수정/삭제는 작성자 본인 또는 OWNER/ADMIN만
- **파일**: `GET/POST/DELETE /api/files`, `GET /api/files/{id}/download` — 파일 페이지 연동됨 (드래그 앤 드롭, 파일당 최대 50MB, 로컬 디스크 저장)
- **구글 / 네이버 로그인**: `/oauth2/authorization/google`, `/oauth2/authorization/naver` — 프론트 로그인 페이지 버튼까지 연동됨 (설정 방법은 아래 참고)

- **칸반보드**: 카드 생성/이동(드래그)/수정/삭제, 보드 컬럼·라벨 설정, STOMP + Redis pub/sub 실시간 동기화, 타임라인·알림

**아직 미구현:**
- 아이디 찾기 / 비밀번호 찾기 — 화면(UI)만 있고 백엔드 API 없음
- 대시보드 칸반 위젯에서 카드 조작 (보기·삭제만 가능, 드래그·추가·수정은 칸반 페이지에서)
- 마감일 알림 (설정 토글만 있음)
- 파일 업로드 S3 저장 (`FileStorageService` 인터페이스만 있고 구현체는 로컬 전용)

## 최근 추가된 기능 (`feature/infra`)

### 대시보드
- **진행 현황 카드 ↔ 칸반 동기화**: 고정 숫자 대신 실제 카드 수·비율을 보드 컬럼별로 계산 (컬럼을 추가·삭제하면 같이 바뀜)
- **달력·팀원 현황 접기/펼치기**: 접으면 칸반 영역이 넓어짐 (상태는 브라우저에 저장)
- 칸반 위젯이 화면 밖으로 잘리던 문제 수정(넘치면 가로 스크롤), 사이드바 스크롤 고정

### 칸반 카드
- **카드 수정**: ✎ 버튼으로 제목·마감 일시 수정. 권한은 삭제와 같음(작성자 또는 OWNER/ADMIN, 서버에서도 검증)
- **마감 일시**: 카드에 `⏰ 10/2 18:00` 표시, 지나면 빨간색. 대시보드 캘린더에도 마감일이 점으로 표시됨
- **라벨 최대 2개** + 처음부터 있는 **"기타"** 라벨
- 긴 제목(URL 등)이 카드·알림·타임라인 밖으로 넘치지 않고 줄바꿈, 제목 입력칸 자동 높이
- 라벨 없는 카드에 빈 줄이 생기던 문제 수정 (`+ 라벨`, ✎는 마우스를 올렸을 때만 표시)

### 알림
- 설정의 "댓글 알림" → **새 카드 알림**으로 변경, **라벨 변경 알림** 추가
- 타임라인에 카드 수정·라벨 변경 기록 추가

### 백엔드 / API 변경 (팀 공유용)

| API | 변경 |
|---|---|
| `PATCH /api/cards/{id}` | **신규** — `{ title, dueAt }` 수정 |
| `PATCH /api/cards/{id}/label` | `{ labelId }` → **`{ labelIds: [] }`** (최대 2개) |
| `POST /api/cards` | `labelId` → **`labelIds`**, `dueAt`(선택) 추가 |
| 카드 응답 | `labelId` → **`labelIds`**, `dueAt` 추가 |
| 타임라인 타입 | `CARD_UPDATED`, `CARD_LABEL_CHANGED` 추가. 카드 생성·라벨 변경도 알림 대상 |

**DB 마이그레이션 `V12__card_due_and_multi_labels.sql`**
- `cards.due_at` 추가
- `cards.label_id` → `card_labels`(card_id, position, label_id) 연결 테이블로 이전 (기존 라벨 자동 이전)
- `labels`에 `etc`("기타") 추가

> 새 마이그레이션은 **V13부터** 만들어 주세요.

## 구글 / 네이버 로그인 설정

기본값(`dummy-...`)만 있으면 앱은 정상 기동하지만, 버튼을 눌러도 구글/네이버가 "잘못된 클라이언트"라고
막습니다. 실제로 로그인이 되게 하려면 아래처럼 직접 앱을 등록해야 합니다 (계정당 한 번만 하면 됨).

**구글**: [Google Cloud Console → API 및 서비스 → 사용자 인증 정보](https://console.cloud.google.com/apis/credentials)에서
OAuth 클라이언트 ID 생성 → 승인된 리디렉션 URI에 `http://localhost:8080/login/oauth2/code/google`과
"게스트로 이용" 전용인 `http://localhost:8080/login/oauth2/code/google-guest` 둘 다 추가.
("승인된 JavaScript 원본"은 안 씀 — 서버 리다이렉트 방식이라 비워둬도 됨.)

**네이버**: [네이버 개발자센터 → 애플리케이션 등록](https://developers.naver.com/apps)에서 앱 생성,
사용 API에 "네이버 로그인" 추가(이름/이메일 제공 동의 필수) → 콜백 URL에
`http://localhost:8080/login/oauth2/code/naver` 등록.

발급받은 값은 `kanban-backend/.env` 파일로 넣는 게 제일 간단합니다(`kanban-backend/.env.example` 복사해서
채우기 — git에는 안 올라감, `mvnw`/IntelliJ로 실행하면 자동으로 읽힘):

```
cd kanban-backend
cp .env.example .env   # 값 채우기
```

(터미널/IntelliJ Run Configuration에 환경변수로 직접 넣어도 동일하게 동작합니다 — 둘 중 편한 쪽으로.)

로그인 성공/실패 후에는 백엔드가 `FRONTEND_BASE_URL`(기본값 `http://localhost:5173`)로 리다이렉트합니다.
프론트를 다른 포트로 띄웠다면 이 값도 같이 맞춰주고, `CORS_ALLOWED_ORIGINS` 환경변수(CORS 허용 목록)에도 추가해야 합니다.

## DB 접속 정보 (로컬 개발용)

`docker-compose.yml`에 정의됨:

| 항목 | 값 |
|---|---|
| Host | localhost |
| Port | 3307 (호스트 포트 충돌 방지로 3306 대신 사용) |
| DB명 | kanban |
| User | kanban |
| Password | kanban1234 |

DB 직접 접속해서 확인하고 싶을 때:

```
docker exec -it kanban-mysql mysql -ukanban -pkanban1234 kanban -e "SELECT id, email, name, role, created_at FROM users;"
```

## DB 스키마 관리 (Flyway)

테이블은 더 이상 Hibernate가 자동으로 만들지 않습니다(`ddl-auto: validate`). 스키마는 전부
`kanban-backend/src/main/resources/db/migration/V{번호}__설명.sql` 마이그레이션 파일로 관리하고,
앱이 뜰 때 Flyway가 자동으로 적용합니다.

- 엔티티에 컬럼/테이블을 추가·변경했으면 **기존 파일을 고치지 말고** 새 버전 파일을 추가하세요.
  예: `V2__add_board_tables.sql`
- 테스트(`mvnw test`)는 지금처럼 H2 + Hibernate `create-drop`을 쓰고 Flyway는 꺼져 있습니다
  (`application-test.yml`) — 마이그레이션 파일과 별개로 동작하니 신경 안 써도 됩니다.

## 배포 (Docker)

로컬 개발은 지금처럼 `docker compose up -d`(DB/Redis만) + `mvnw` + `npm run dev` 조합을 그대로 씁니다.
아래는 백엔드/프론트까지 컨테이너로 띄우는 **배포용** 별도 구성입니다.

```
cp .env.example .env          # 값 채우기 (비밀번호, JWT 시크릿 등)
docker compose -f docker-compose.prod.yml up -d --build
```

- 프론트: `http://localhost` (nginx, 80포트)
- 백엔드: `http://localhost:8080`
- `kanban-backend/Dockerfile`, `kanban-frontend/Dockerfile` 각각 멀티스테이지 빌드
- 프론트 Dockerfile은 빌드 시점에 `VITE_API_BASE_URL`을 박아 넣으므로, 배포 도메인이 바뀌면
  `.env`의 값을 바꾸고 다시 빌드해야 함
- CORS 허용 origin은 `CORS_ALLOWED_ORIGINS` 환경변수로 관리 (콤마로 여러 개 가능)

## 운영 배포 (AWS EC2 + 도메인)

**접속 주소: https://onboard-kanban.duckdns.org**

**`master`에 머지되면 자동으로 배포됩니다.** 서버가 1분마다 GitHub를 확인해서 새 커밋이 있으면
받아서 다시 빌드·실행합니다. 서버에서 직접 빌드하므로 반영까지 **5~10분** 걸립니다.
master가 깨진 상태로 머지되면 사이트도 같이 깨지니 PR에서 CI 통과를 확인하고 머지하세요.

### 구성

```
브라우저 ──HTTPS──▶ Caddy(80/443, 인증서 자동 발급·갱신)
                     ├─ /api/*, /ws/*, /oauth2/*, /login/oauth2/* ──▶ backend(Spring Boot :8080)
                     └─ 그 외 ────────────────────────────────────▶ frontend(nginx :80)
                     backend ──▶ MySQL, Redis (같은 서버의 컨테이너, 외부 비공개)
```

| 항목 | 내용 |
|---|---|
| 서버 | AWS EC2 서울(ap-northeast-2), t3.micro(1GB) + 스왑 4GB, Ubuntu 26.04, 저장공간 20GB |
| 도메인 | DuckDNS 무료 서브도메인 `onboard-kanban.duckdns.org` → EC2 퍼블릭 IP |
| HTTPS | Caddy가 Let's Encrypt 인증서 자동 발급 |
| 자동 배포 | systemd 타이머(`onboard-deploy.timer`) → `/opt/onboard/bin/auto-deploy.sh` |
| 비용 | AWS 무료 플랜 크레딧에서 차감 (서버·저장공간·IP 합쳐 약 $15/월) |

프론트와 백엔드를 **같은 도메인**으로 묶은 이유: refresh token 쿠키가 `SameSite=Lax`라서 도메인이 다르면
쿠키가 전달되지 않아 로그인 유지가 깨집니다.

### 관련 파일 (`deploy/`)

| 파일 | 역할 |
|---|---|
| `Caddyfile` | 경로별로 backend / frontend에 연결하는 리버스 프록시 설정 |
| `docker-compose.server.yml` | `docker-compose.prod.yml` 위에 겹치는 서버용 설정 (Caddy 추가, 80/443만 외부 공개) |
| `auto-deploy.sh` | 새 커밋 확인 → `git reset` → `docker compose up -d --build` |
| `setup-server.sh` | 새 서버 최초 1회 설정 (Docker, 스왑, 방화벽, 저장소 clone, `.env` 생성, 자동 배포 타이머) |

서버의 비밀값(DB 비밀번호, JWT 시크릿 등)은 **서버의 `/opt/onboard/config/.env`에만** 있고 저장소에는 올라가지 않습니다.
`deploy/`의 설정 파일도 서버에는 `/opt/onboard/config/`에 복사해 두고 쓰기 때문에, 이 폴더를 고쳤으면 서버에도 다시 복사해야 반영됩니다.

### 운영 시 주의사항

- EC2 콘솔에서 **"종료(Terminate)"하면 서버와 DB 데이터가 영구 삭제**됩니다. 잠시 끄려면 **"중지(Stop)"**.
- 중지 후 다시 켜면 **퍼블릭 IP가 바뀝니다** → [DuckDNS](https://www.duckdns.org)에서 IP를 다시 넣어야 접속됩니다.
- 무료 플랜 크레딧이 떨어지거나 기간(2027-03-30)이 끝나면 서버가 멈춥니다. 계속 쓰려면 유료 전환 필요.
- 운영 DB는 로컬과 별개인 빈 DB입니다 (회원가입부터 새로).
- **구글·네이버 로그인은 운영에서 아직 안 됩니다.** 각 개발자 콘솔에 리디렉션 URI
  `https://onboard-kanban.duckdns.org/login/oauth2/code/google`(naver)을 등록하고, 서버 `.env`의
  `GOOGLE_*`, `NAVER_*` 값을 바꾼 뒤 `sudo /opt/onboard/bin/auto-deploy.sh --force`로 재시작하세요.

### 서버 관리 명령어

```
ssh -i ~/.ssh/<키파일> ubuntu@<EC2 퍼블릭 IP>

sudo journalctl -u onboard-deploy -n 50                  # 자동 배포 기록
sudo /opt/onboard/bin/auto-deploy.sh --force             # 지금 바로 다시 배포
cd /opt/onboard/app && sudo docker compose -f docker-compose.prod.yml -f deploy/docker-compose.server.yml ps
sudo docker logs -f kanban-backend-prod                  # 백엔드 로그
```

## 자주 발생하는 문제

**`Public Key Retrieval is not allowed`**
→ 이미 해결되어 있음 (`allowPublicKeyRetrieval=true` JDBC 옵션 적용됨). 혹시 다시 나오면 `application.yml`의 datasource url 확인.

**`ports are not available: ... 3306 ...`**
→ 로컬에 이미 다른 MySQL이 3306 포트를 쓰고 있는 경우. 이 프로젝트는 호스트 포트를 3307로 분리해뒀으니 재발하면 `docker-compose.yml`의 포트 매핑 확인.

**`ports are not available: ... 6379 ...`**
→ 로컬에 이미 다른 Redis가 6379 포트를 쓰고 있는 경우. 기존 Redis를 끄거나, `docker-compose.yml`의 매핑을 `"6380:6379"`처럼 바꾸고 백엔드 실행 시 `REDIS_PORT=6380` 환경변수를 지정.

**`release version 21 not supported`**
→ 컴파일에 쓰이는 Java가 21 미만. `java -version` 확인 후 JDK 21로 `JAVA_HOME` 설정.

**Spring Boot 앱은 뜨는데 프론트에서 API 호출이 실패함**
→ 백엔드가 먼저 켜져 있는지, `docker ps`로 MySQL/Redis가 healthy인지 확인. CORS는 기본값이 `http://localhost:5173`만 허용이니 다른 포트로 프론트를 띄웠다면 `CORS_ALLOWED_ORIGINS` 환경변수(또는 `application.yml`의 `app.cors.allowed-origins`)에 추가해야 함.

## 테스트 실행 (백엔드)

```
./mvnw test        # 맥/리눅스
.\mvnw.cmd test     # 윈도우
```

H2 인메모리 DB를 쓰기 때문에 Docker/MySQL이 안 켜져 있어도 실행됩니다.
