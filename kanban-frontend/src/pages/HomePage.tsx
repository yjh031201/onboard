import { isLoggedIn } from "../lib/auth";
import LandingPage from "./LandingPage";
import ProjectSelectPage from "./ProjectSelectPage";

/** "/" 라우트 — 로그인 여부에 따라 랜딩페이지 또는 프로젝트 선택 화면을 보여줌. */
export default function HomePage() {
  return isLoggedIn() ? <ProjectSelectPage /> : <LandingPage />;
}
