# CardBoard

간단한 칸반(Kanban) 보드 애플리케이션. Express + MongoDB 백엔드와 React(Vite) 클라이언트로 구성됩니다.

## 스택

- **Server**: Node.js, Express, Mongoose(MongoDB), JWT 인증, bcryptjs
- **Client**: React 18, Vite, React Router, Zustand, Axios
- **DB**: MongoDB (docker-compose로 로컬 구동)

## 사전 준비

- Node.js 18+
- Docker (로컬 MongoDB 실행용, 이미 MongoDB가 있다면 생략 가능)

## 시작하기

### 1. MongoDB 실행

```bash
docker-compose up -d
```

`mongodb://localhost:27017/kanban`로 접속 가능한 MongoDB 컨테이너가 뜹니다.

### 2. 서버

```bash
cd server
npm install
cp .env.example .env   # 값 확인 후 필요시 수정 (특히 JWT_SECRET)
npm run dev
```

기본적으로 `http://localhost:4000`에서 API가 열립니다.

### 3. 클라이언트

```bash
cd client
npm install
cp .env.example .env
npm run dev
```

`http://localhost:5173`에서 개발 서버가 열립니다.

## 환경 변수

### server/.env

| 변수 | 설명 | 기본값 |
| --- | --- | --- |
| `PORT` | API 서버 포트 | `4000` |
| `MONGO_URI` | MongoDB 접속 문자열 | `mongodb://localhost:27017/kanban` |
| `JWT_SECRET` | JWT 서명 비밀키 (운영 배포 전 반드시 변경) | - |
| `JWT_EXPIRES_IN` | JWT 만료 시간 | `2h` |
| `CLIENT_ORIGIN` | CORS 허용 origin | `http://localhost:5173` |

### client/.env

| 변수 | 설명 | 기본값 |
| --- | --- | --- |
| `VITE_API_URL` | API 베이스 경로 | `/api` |

## API 개요

모든 엔드포인트는 `/api` 하위에 있으며, 인증이 필요한 엔드포인트는 `Authorization: Bearer <token>` 헤더가 필요합니다.

- `POST /api/auth/register` — 회원가입
- `POST /api/auth/login` — 로그인
- `GET /api/auth/me` — 내 정보 조회 (인증 필요)
- `POST /api/boards` / `GET /api/boards` — 보드 생성 / 내 보드 목록
- `GET /api/boards/:boardId` — 보드 상세
- `PATCH /api/boards/:boardId` / `DELETE /api/boards/:boardId` — 보드 이름 변경 / 삭제
- `POST /api/boards/:boardId/invite` / `DELETE /api/boards/:boardId/members/:userId` — 멤버 초대 / 제거
- `POST /api/boards/:boardId/labels` / `PATCH .../labels/:labelId` / `DELETE .../labels/:labelId` — 라벨 관리
- `POST /api/boards/:boardId/columns` — 컬럼 생성
- `PATCH /api/columns/:columnId` / `DELETE /api/columns/:columnId` — 컬럼 이름 변경 / 삭제
- `POST /api/columns/:columnId/cards` — 카드 생성
- `PATCH /api/cards/:cardId` / `DELETE /api/cards/:cardId` — 카드 수정 / 삭제

## 프로젝트 구조

```
server/
  src/
    config/      # env, db 연결
    controllers/ # 라우트 핸들러
    middleware/  # JWT 인증, 멤버십 체크
    models/      # Mongoose 스키마
    routes/      # Express 라우터
client/
  src/           # React 앱
```
