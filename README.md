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
- **프로젝트 설정 API**: `GET/PUT /api/settings` (수정은 OWNER/ADMIN만, 백엔드만)
- **일정(캘린더) API**: `GET/POST/PUT/DELETE /api/schedules` (수정/삭제는 작성자 본인 또는 OWNER/ADMIN만, 백엔드만)
- **파일 업로드 API**: `GET/POST/DELETE /api/files`, `GET /api/files/{id}/download` — 로컬 디스크 저장 (백엔드만)
- **구글 / 네이버 로그인**: `/oauth2/authorization/google`, `/oauth2/authorization/naver` — 프론트 로그인 페이지 버튼까지 연동됨 (설정 방법은 아래 참고)

**아직 미구현:**
- 아이디 찾기 / 비밀번호 찾기 — 화면(UI)만 있고 백엔드 API 없음
- 칸반보드 카드 CRUD, 실시간 동기화 등 — 프론트 화면만 있고 백엔드 연동 전
- 위 설정/일정/파일 API들의 프론트엔드 화면 연동
- 파일 업로드 S3 저장 (`FileStorageService` 인터페이스만 있고 구현체는 로컬 전용)

## 구글 / 네이버 로그인 설정

기본값(`dummy-...`)만 있으면 앱은 정상 기동하지만, 버튼을 눌러도 구글/네이버가 "잘못된 클라이언트"라고
막습니다. 실제로 로그인이 되게 하려면 아래처럼 직접 앱을 등록해야 합니다 (계정당 한 번만 하면 됨).

**구글**: [Google Cloud Console → API 및 서비스 → 사용자 인증 정보](https://console.cloud.google.com/apis/credentials)에서
OAuth 클라이언트 ID 생성 → 승인된 리디렉션 URI에 `http://localhost:8080/login/oauth2/code/google` 추가.

**네이버**: [네이버 개발자센터 → 애플리케이션 등록](https://developers.naver.com/apps)에서 앱 생성,
사용 API에 "네이버 로그인" 추가(이름/이메일 제공 동의 필수) → 콜백 URL에
`http://localhost:8080/login/oauth2/code/naver` 등록.

발급받은 값은 `kanban-backend`를 실행하는 터미널/IntelliJ Run Configuration에 환경변수로 넣어주세요:

```
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...
NAVER_CLIENT_ID=...
NAVER_CLIENT_SECRET=...
```

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
