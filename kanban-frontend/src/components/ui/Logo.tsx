import { Link } from "react-router-dom";

interface LogoProps {
  /** 기본값은 실제 브랜드명("ONBOARD")이고, 필요하면 호출부에서 다른 텍스트로 바꿔 쓸 수 있다. */
  name?: string;
  /** 로고 이미지 박스 크기 (tailwind size 클래스). */
  iconSize?: string;
  textClassName?: string;
}

/**
 * 전역 로고 — 랜딩/메인/사이드바가 전부 이 컴포넌트를 공유한다.
 * 어디서 누르든 메인화면("/")으로 이동한다 — 로그인 상태면 프로젝트 선택 화면,
 * 아니면 랜딩페이지가 보인다 (HomePage가 분기).
 * 이미지는 /logo-mark.png(실제 브랜드 마크, favicon 세트의 apple-icon 기반)를
 * 고정 크기 박스에 넣어서 쓴다.
 */
export default function Logo({
  name = "ONBOARD",
  iconSize = "size-8",
  textClassName = "text-[16px] font-bold text-[#111827]",
}: LogoProps) {
  return (
    <Link to="/" className="flex items-center gap-2">
      <img src="/logo-mark.png" alt="" className={`${iconSize} shrink-0 rounded-md`} />
      <p className={`whitespace-nowrap ${textClassName}`}>{name}</p>
    </Link>
  );
}
