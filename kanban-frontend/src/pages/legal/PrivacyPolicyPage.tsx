export default function PrivacyPolicyPage() {
  return (
    <div className="flex min-h-screen w-full justify-center bg-[#fafafa] px-6 py-16">
      <div className="flex w-full max-w-[680px] flex-col gap-6 rounded-2xl bg-white p-10 shadow-[0px_2px_8px_0px_rgba(0,0,0,0.04)]">
        <div className="flex flex-col gap-1.5">
          <h1 className="text-[22px] font-bold text-[#111827]">개인정보처리방침</h1>
          <p className="text-[12.5px] text-[#9ca3af]">최종 수정일: 2026년 10월</p>
        </div>

        <p className="text-[13.5px] leading-relaxed text-[#374151]">
          본 서비스(이하 "칸반보드")는 대학 과제로 개발된 비상업적 팀 협업 도구입니다. 아래는 서비스 이용 과정에서
          수집하는 정보와 이용 방법을 설명합니다.
        </p>

        <Section title="1. 수집하는 정보">
          <ul className="list-disc pl-5">
            <li>회원가입 시: 이름, 이메일, 휴대폰번호, 비밀번호(암호화 저장)</li>
            <li>소셜 로그인(구글/네이버) 이용 시: 해당 서비스가 제공하는 이메일, 이름, 프로필 식별자</li>
            <li>외부 서비스 연동(GitHub/Slack/Google Drive)을 사용자가 직접 연결한 경우: 연동에 필요한 접근 토큰, 계정 식별 정보</li>
          </ul>
        </Section>

        <Section title="2. 이용 목적">
          <ul className="list-disc pl-5">
            <li>회원 인증 및 팀 협업 기능(칸반보드, 타임라인, 실시간 알림) 제공</li>
            <li>사용자가 직접 연결한 경우에 한해 GitHub 이슈 조회, Slack 알림 전송, Google Drive 파일 목록 조회</li>
          </ul>
        </Section>

        <Section title="3. 제3자 제공">
          <p>수집한 정보를 외부에 판매하거나 제공하지 않습니다. 연동 서비스(GitHub/Slack/Google)의 데이터는 사용자 본인의 요청을 처리하는 목적으로만 사용되며, 다른 목적으로 전송되지 않습니다.</p>
        </Section>

        <Section title="4. 보관 기간">
          <p>회원 탈퇴 또는 연동 해제 시 관련 정보를 지체 없이 삭제합니다.</p>
        </Section>

        <Section title="5. 문의">
          <p>개인정보 관련 문의는 서비스 운영자 이메일로 연락해 주세요.</p>
        </Section>
      </div>
    </div>
  );
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div className="flex flex-col gap-2">
      <h2 className="text-[14.5px] font-bold text-[#111827]">{title}</h2>
      <div className="text-[13.5px] leading-relaxed text-[#374151]">{children}</div>
    </div>
  );
}
