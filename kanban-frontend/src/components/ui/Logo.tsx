interface LogoProps {
  /** 아직 로고 파일이 안 정해졌으면 기본값("칸반보드")을 쓰고, 나중에 실제 이름으로 바꾸면 됨. */
  name?: string;
  /** 로고 이미지 박스 크기 (tailwind size 클래스). */
  iconSize?: string;
  textClassName?: string;
}

/**
 * 전역 로고 — 랜딩/메인/사이드바가 전부 이 컴포넌트를 공유한다.
 * 이미지는 /favicon.svg(실제 디자인된 마크)를 고정 크기 박스에 넣어서 쓰고,
 * 나중에 다른 로고 파일로 바꾸더라도 이 박스 크기/배치는 그대로 유지된다.
 */
export default function Logo({
  name = "칸반보드",
  iconSize = "size-8",
  textClassName = "text-[16px] font-bold text-[#111827]",
}: LogoProps) {
  return (
    <div className="flex items-center gap-2">
      <img src="/favicon.svg" alt="" className={`${iconSize} shrink-0`} />
      <p className={`whitespace-nowrap ${textClassName}`}>{name}</p>
    </div>
  );
}
