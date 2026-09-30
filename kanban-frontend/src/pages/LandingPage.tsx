import { Link } from "react-router-dom";
import Button from "../components/ui/Button";

interface FeatureCard {
  emoji: string;
  title: string;
  desc: string;
}

const FEATURES: FeatureCard[] = [
  { emoji: "⚡", title: "실시간 협업", desc: "여러 명이 동시에 보드를 수정해도\n실시간으로 반영돼요." },
  { emoji: "👥", title: "팀원 관리", desc: "이메일로 팀원을 초대하고\n권한을 세밀하게 관리하세요." },
  { emoji: "🗓️", title: "일정·타임라인", desc: "일정과 팀 활동 기록을\n한눈에 확인할 수 있어요." },
];

/** 로그아웃 상태에서 "/"로 접속했을 때 보여주는 랜딩페이지. 로그인 상태면 HomePage가 대신 ProjectSelectPage를 보여줌. */
export default function LandingPage() {
  return (
    <div className="flex min-h-screen w-full flex-col items-center bg-[#fafafa]">
      <nav className="flex w-full items-center justify-between bg-white px-[60px] py-[22px] shadow-[0px_2px_8px_rgba(0,0,0,0.04)]">
        <p className="text-[16px] font-bold text-[#6366f1]">📋 칸반보드</p>
        <div className="flex items-center gap-3.5">
          <Link to="/login" className="text-[14px] font-medium text-[#6b7280] hover:text-[#111827]">
            로그인
          </Link>
          <Link to="/signup">
            <Button variant="primary" className="px-[18px] py-[9px] text-[13.5px]">
              회원가입
            </Button>
          </Link>
        </div>
      </nav>

      <section className="flex w-full flex-col items-center gap-5 bg-white pb-[90px] pt-[120px]">
        <span className="rounded-full bg-[#eeeefe] px-3.5 py-1.5 text-[12px] font-bold text-[#6366f1]">
          🚀 4인 팀 포트폴리오 프로젝트
        </span>
        <h1 className="w-[700px] text-center text-[40px] font-bold leading-normal text-[#111827]">
          팀과 함께, 실시간으로
          <br />
          협업하는 칸반보드
        </h1>
        <p className="w-[560px] text-center text-[15.5px] leading-normal text-[#6b7280]">
          일정, 팀원, 파일까지 — 흩어져 있던 협업을 하나의 보드로 모았어요.
          <br />
          지금 바로 팀과 함께 시작해보세요.
        </p>
        <div className="flex gap-3 pt-3">
          {/* 게스트 로그인은 아직 백엔드에 없어서 일단 로그인 화면으로 연결해뒀어요. */}
          <Link to="/login">
            <Button variant="primary" className="px-7 py-[13px] text-[15px]">
              게스트로 이용
            </Button>
          </Link>
          <Link
            to="/login"
            className="flex items-center justify-center rounded-lg border border-[#e5e7eb] bg-white px-7 py-[13px] text-[15px] font-bold text-[#374151] transition-colors hover:bg-[#f9fafb]"
          >
            로그인
          </Link>
        </div>
      </section>

      <section className="flex w-full justify-center gap-6 bg-white px-[60px] pb-[100px]">
        {FEATURES.map((f) => (
          <div
            key={f.title}
            className="flex w-[360px] flex-col gap-3 rounded-[14px] bg-white p-7 shadow-[0px_2px_8px_rgba(0,0,0,0.04)]"
          >
            <p className="text-[26px]">{f.emoji}</p>
            <p className="text-[15px] font-bold text-[#111827]">{f.title}</p>
            <p className="whitespace-pre-line text-[13px] leading-normal text-[#6b7280]">{f.desc}</p>
          </div>
        ))}
      </section>

      <footer className="flex w-full items-center justify-center bg-white py-6">
        <p className="text-[12px] text-[#9ca3af]">© 2026 칸반보드 팀 프로젝트 · 대림대학교</p>
      </footer>
    </div>
  );
}
