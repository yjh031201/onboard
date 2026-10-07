import type { ReactNode } from "react";
import Logo from "../components/ui/Logo";

// 구글·네이버 OAuth 앱 게시(검수)에 필요한 개인정보처리방침. 로그인 없이 볼 수 있어야 한다.
const EFFECTIVE_DATE = "2026년 10월 1일";
const CONTACT_URL = "https://github.com/yjh031201/onboard/issues";

function Section({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="flex flex-col gap-2">
      <h2 className="text-[16px] font-bold text-[#111827]">{title}</h2>
      <div className="flex flex-col gap-1.5 text-[14px] leading-relaxed text-[#374151]">{children}</div>
    </section>
  );
}

export default function PrivacyPage() {
  return (
    <div className="min-h-screen w-full bg-[#fafafa] px-4 py-12">
      <article className="mx-auto flex w-[720px] max-w-full flex-col gap-7 rounded-2xl border border-[#f0f0f2] bg-white p-10 shadow-[0px_4px_16px_0px_rgba(0,0,0,0.05)]">
        <header className="flex flex-col gap-2">
          <Logo iconSize="size-6" textClassName="text-[14px] font-bold text-[#6366f1]" />
          <h1 className="text-[23px] font-bold text-[#111827]">개인정보처리방침</h1>
          <p className="text-[13px] text-[#6b7280]">시행일: {EFFECTIVE_DATE}</p>
        </header>

        <p className="text-[14px] leading-relaxed text-[#374151]">
          Onboard 칸반보드(이하 "서비스")는 4인 팀이 포트폴리오 목적으로 만든 비영리 협업 도구입니다. 서비스는
          운영에 꼭 필요한 최소한의 개인정보만 수집하며, 아래와 같이 처리합니다.
        </p>

        <Section title="1. 수집하는 항목">
          <p>· 이메일 회원가입: 이메일, 이름, 비밀번호(암호화하여 저장), 휴대폰 번호(입력한 경우)</p>
          <p>· Google 로그인: Google 계정의 이메일, 이름, 계정 고유 식별자</p>
          <p>· 네이버 로그인: 네이버 계정의 이메일, 이름, 계정 고유 식별자</p>
          <p>· 서비스 이용 중 생성되는 정보: 작성한 카드·일정·업로드한 파일, 활동 기록(타임라인), 접속 상태</p>
        </Section>

        <Section title="2. 이용 목적">
          <p>· 회원 식별과 로그인 유지</p>
          <p>· 칸반보드·일정·파일 등 협업 기능 제공과 팀원에게 작성자 표시</p>
          <p>· 서비스 내 알림 제공</p>
          <p>수집한 정보는 광고, 마케팅, 프로필링 용도로 사용하지 않습니다.</p>
        </Section>

        <Section title="3. 보관 기간과 파기">
          <p>
            개인정보는 회원 탈퇴를 요청하거나 서비스 운영을 종료할 때까지 보관하며, 그 즉시 데이터베이스와 업로드
            파일에서 삭제합니다.
          </p>
        </Section>

        <Section title="4. 제3자 제공과 처리 위탁">
          <p>개인정보를 제3자에게 판매하거나 제공하지 않습니다.</p>
          <p>
            서비스는 Amazon Web Services(AWS) 서울 리전의 서버에서 운영되며, 데이터는 해당 서버에만 저장됩니다.
          </p>
        </Section>

        <Section title="5. Google 사용자 데이터">
          <p>
            Google 로그인으로 받은 정보(이메일, 이름, 식별자)는 계정 생성과 로그인에만 사용합니다. 이 정보를 다른
            곳에 전송하거나 AI 모델 학습에 사용하지 않으며, Google API 서비스 사용자 데이터 정책(제한적 사용 요건
            포함)을 따릅니다.
          </p>
        </Section>

        <Section title="6. 쿠키와 브라우저 저장소">
          <p>
            로그인 유지를 위해 인증 토큰을 쿠키와 브라우저 저장소에 보관하며, 알림 설정 같은 화면 설정도 브라우저에
            저장합니다. 추적·광고용 쿠키는 사용하지 않습니다.
          </p>
        </Section>

        <Section title="7. 이용자의 권리">
          <p>
            언제든지 본인 정보의 열람, 수정, 삭제(회원 탈퇴)를 요청할 수 있습니다. 아래 문의처로 요청하면 지체 없이
            처리합니다.
          </p>
        </Section>

        <Section title="8. 문의처">
          <p>
            개인정보 관련 문의:{" "}
            <a href={CONTACT_URL} target="_blank" rel="noreferrer" className="font-medium text-[#6366f1]">
              GitHub 이슈 ({CONTACT_URL.replace("https://", "")})
            </a>
          </p>
        </Section>

        <Section title="9. 변경 안내">
          <p>이 방침이 바뀌면 시행일을 갱신하고 이 페이지에 공지합니다.</p>
        </Section>
      </article>
    </div>
  );
}
