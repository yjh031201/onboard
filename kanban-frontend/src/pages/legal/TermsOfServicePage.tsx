export default function TermsOfServicePage() {
  return (
    <div className="flex min-h-screen w-full justify-center bg-[#fafafa] px-6 py-16">
      <div className="flex w-full max-w-[680px] flex-col gap-6 rounded-2xl bg-white p-10 shadow-[0px_2px_8px_0px_rgba(0,0,0,0.04)]">
        <div className="flex flex-col gap-1.5">
          <h1 className="text-[22px] font-bold text-[#111827]">서비스 이용약관</h1>
          <p className="text-[12.5px] text-[#9ca3af]">최종 수정일: 2026년 10월</p>
        </div>

        <p className="text-[13.5px] leading-relaxed text-[#374151]">
          본 약관은 대학 과제로 개발된 비상업적 팀 협업 도구 "칸반보드"(이하 "서비스") 이용에 관한 조건을 안내합니다.
        </p>

        <Section title="1. 서비스의 성격">
          <p>본 서비스는 상업적 목적이 아닌 학습·시연 목적으로 운영되며, 사전 고지 없이 기능이 변경되거나 서비스가 종료될 수 있습니다.</p>
        </Section>

        <Section title="2. 이용자의 의무">
          <ul className="list-disc pl-5">
            <li>타인의 계정을 무단으로 사용하지 않습니다.</li>
            <li>서비스를 악용하거나 다른 이용자에게 피해를 주는 행위를 하지 않습니다.</li>
          </ul>
        </Section>

        <Section title="3. 외부 서비스 연동">
          <p>GitHub/Slack/Google Drive 연동은 이용자가 직접 동의한 경우에만 활성화되며, 설정 페이지에서 언제든지 연동을 해제할 수 있습니다.</p>
        </Section>

        <Section title="4. 책임의 제한">
          <p>본 서비스는 무료 과제용 프로젝트로, 서비스 이용 중 발생하는 데이터 손실이나 장애에 대해 법적 책임을 지지 않습니다.</p>
        </Section>

        <Section title="5. 약관의 변경">
          <p>약관 내용은 서비스 개선 과정에서 변경될 수 있으며, 변경 시 이 페이지에 반영합니다.</p>
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
