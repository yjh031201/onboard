import { useState } from "react";
import PageShell from "../../components/layout/PageShell";
import Section, { Divider } from "../../components/ui/Section";
import { Field } from "../../components/ui/Field";
import Button from "../../components/ui/Button";
import Avatar from "../../components/ui/Avatar";
import Pill from "../../components/ui/Pill";

type Role = "소유자" | "관리자" | "멤버";

interface TeamMemberRow {
  id: string;
  name: string;
  email: string;
  initial: string;
  role: Role;
}

const MEMBERS: TeamMemberRow[] = [
  { id: "1", name: "양종호", email: "jdbdjhd8q@gmail.com", initial: "양", role: "소유자" },
  { id: "2", name: "김민수", email: "minsu@example.com", initial: "김", role: "관리자" },
  { id: "3", name: "이서연", email: "seoyeon@example.com", initial: "이", role: "멤버" },
  { id: "4", name: "박지훈", email: "jihoon@example.com", initial: "박", role: "멤버" },
];

const ROLE_PILL: Record<Role, { bg: string; text: string }> = {
  소유자: { bg: "#eeeefe", text: "#6366f1" },
  관리자: { bg: "#ecf2fe", text: "#2f60e0" },
  멤버: { bg: "#f3f4f6", text: "#6b7280" },
};

interface IntegrationRow {
  id: string;
  name: string;
  description: string;
  connected: boolean;
}

const INTEGRATIONS: IntegrationRow[] = [
  { id: "github", name: "GitHub", description: "코드 저장소와 이슈를 연결하세요", connected: true },
  { id: "slack", name: "Slack", description: "알림을 슬랙 채널로 받아보세요", connected: false },
  {
    id: "drive",
    name: "Google Drive",
    description: "파일을 동기화하고 첨부하세요",
    connected: false,
  },
];

export default function MembersPage() {
  const [teamName, setTeamName] = useState("칸반보드 프로젝트팀");

  return (
    <PageShell title="팀원" subtitle="팀원을 초대하고 팀 정보를 관리하세요">
      <Section title="일반" description="팀의 기본 정보를 관리하세요">
        <Field label="팀 이름" value={teamName} onChange={(e) => setTeamName(e.target.value)} />
        <div className="flex w-full justify-end">
          <Button variant="primary">변경사항 저장</Button>
        </div>
      </Section>

      <Section title="구성원 및 권한" description="팀원을 초대하고 권한을 관리하세요">
        <div className="flex w-full items-center justify-between">
          <p className="text-[12.5px] font-medium text-[#6b7280]">총 {MEMBERS.length}명</p>
          <Button variant="primary">+ 구성원 초대</Button>
        </div>
        {MEMBERS.map((member, i) => (
          <div key={member.id} className="flex w-full flex-col gap-[18px]">
            {i > 0 && <Divider />}
            <div className="flex w-full items-center justify-between">
              <div className="flex items-center gap-2.5">
                <Avatar initial={member.initial} />
                <div className="flex flex-col gap-0.5">
                  <p className="text-[13.5px] font-medium text-[#111827]">{member.name}</p>
                  <p className="text-[12px] text-[#6b7280]">{member.email}</p>
                </div>
              </div>
              <div className="flex items-center gap-3.5">
                <Pill bg={ROLE_PILL[member.role].bg} text={ROLE_PILL[member.role].text}>
                  {member.role}
                </Pill>
                {member.role !== "소유자" && (
                  <button type="button" className="text-[12.5px] text-[#9ca3af]">
                    제거
                  </button>
                )}
              </div>
            </div>
          </div>
        ))}
      </Section>

      <Section title="연동" description="외부 서비스와 연결해 팀 작업을 더 편리하게 만드세요">
        {INTEGRATIONS.map((integration, i) => (
          <div key={integration.id} className="flex w-full flex-col gap-[18px]">
            {i > 0 && <Divider />}
            <div className="flex w-full items-center justify-between">
              <div className="flex flex-col gap-[3px]">
                <p className="text-[13.5px] font-medium text-[#111827]">{integration.name}</p>
                <p className="text-[12px] text-[#6b7280]">{integration.description}</p>
              </div>
              <div className="flex items-center gap-3">
                {integration.connected ? (
                  <Pill bg="#e9f9f1" text="#109568">
                    연결됨
                  </Pill>
                ) : (
                  <Pill bg="#f3f4f6" text="#6b7280">
                    연결 안 됨
                  </Pill>
                )}
                <Button variant={integration.connected ? "secondary" : "primary"}>
                  {integration.connected ? "관리" : "연결하기"}
                </Button>
              </div>
            </div>
          </div>
        ))}
      </Section>

      <Section title="팀 삭제" description="팀과 관련된 모든 데이터가 영구적으로 삭제되며 이 작업은 되돌릴 수 없습니다." danger>
        <Button variant="danger">팀 삭제</Button>
      </Section>
    </PageShell>
  );
}
