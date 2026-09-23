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

`kanban-mysql`이 `healthy` 상태로 떠 있어야 합니다. (`starting` 상태에서 바로 다음 단계로 넘어가면 DB 연결 실패로 실행이 죽습니다 — 몇 초 기다렸다가 다시 확인)

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

**아직 미구현:**
- 아이디 찾기 / 비밀번호 찾기 — 화면(UI)만 있고 백엔드 API 없음
- 칸반보드 카드 CRUD, 실시간 동기화 등 — 프론트 화면만 있고 백엔드 연동 전

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

## 자주 발생하는 문제

**`Public Key Retrieval is not allowed`**
→ 이미 해결되어 있음 (`allowPublicKeyRetrieval=true` JDBC 옵션 적용됨). 혹시 다시 나오면 `application.yml`의 datasource url 확인.

**`ports are not available: ... 3306 ...`**
→ 로컬에 이미 다른 MySQL이 3306 포트를 쓰고 있는 경우. 이 프로젝트는 호스트 포트를 3307로 분리해뒀으니 재발하면 `docker-compose.yml`의 포트 매핑 확인.

**`release version 21 not supported`**
→ 컴파일에 쓰이는 Java가 21 미만. `java -version` 확인 후 JDK 21로 `JAVA_HOME` 설정.

**Spring Boot 앱은 뜨는데 프론트에서 API 호출이 실패함**
→ 백엔드가 먼저 켜져 있는지, `docker ps`로 MySQL이 healthy인지 확인. CORS는 `http://localhost:5173`만 허용되어 있으니 다른 포트로 프론트를 띄웠다면 백엔드 `SecurityConfig`의 CORS 설정도 맞춰야 함.

## 테스트 실행 (백엔드)

```
./mvnw test        # 맥/리눅스
.\mvnw.cmd test     # 윈도우
```

H2 인메모리 DB를 쓰기 때문에 Docker/MySQL이 안 켜져 있어도 실행됩니다.
