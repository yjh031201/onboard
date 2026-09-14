import { useState } from "react";
import PageShell from "../../components/layout/PageShell";
import Section, { Divider } from "../../components/ui/Section";
import { Field, TextareaField } from "../../components/ui/Field";
import Button from "../../components/ui/Button";
import Toggle from "../../components/ui/Toggle";

interface BoardColumn {
  id: string;
  name: string;
}

const INITIAL_COLUMNS: BoardColumn[] = [
  { id: "todo", name: "할 일" },
  { id: "in-progress", name: "진행 중" },
  { id: "done", name: "완료" },
];

interface LabelDef {
  id: string;
  name: string;
  color: string;
}

const INITIAL_LABELS: LabelDef[] = [
  { id: "bug", name: "버그", color: "#ef4444" },
  { id: "feature", name: "기능", color: "#6366f1" },
  { id: "design", name: "디자인", color: "#a855f7" },
  { id: "urgent", name: "긴급", color: "#f59e0b" },
];

export default function SettingsPage() {
  const [projectName, setProjectName] = useState("칸반보드 프로젝트");
  const [description, setDescription] = useState(
    "팀 프로젝트 진행 상황을 관리하는 칸반보드 서비스입니다.",
  );
  const [columns] = useState<BoardColumn[]>(INITIAL_COLUMNS);
  const [labels] = useState<LabelDef[]>(INITIAL_LABELS);
  const [cardMoveNotif, setCardMoveNotif] = useState(true);
  const [commentNotif, setCommentNotif] = useState(true);
  const [dueDateNotif, setDueDateNotif] = useState(false);

  return (
    <PageShell title="설정" subtitle="프로젝트와 보드 설정을 관리하세요">
      <Section title="일반" description="프로젝트의 기본 정보를 관리하세요">
        <Field
          label="프로젝트 이름"
          value={projectName}
          onChange={(e) => setProjectName(e.target.value)}
        />
        <TextareaField
          label="설명"
          rows={2}
          value={description}
          onChange={(e) => setDescription(e.target.value)}
        />
        <div className="flex w-full justify-end">
          <Button variant="primary">변경사항 저장</Button>
        </div>
      </Section>

      <Section title="보드 컬럼" description="칸반보드에 표시할 컬럼을 관리하세요">
        {columns.map((column, i) => (
          <div key={column.id} className="flex w-full flex-col gap-[18px]">
            {i > 0 && <Divider />}
            <div className="flex w-full items-center justify-between">
              <div className="flex items-center gap-2.5">
                <span className="text-[13px] font-bold text-[#9ca3af]">⠿</span>
                <p className="text-[13.5px] font-medium text-[#111827]">{column.name}</p>
              </div>
              <div className="flex items-center gap-3.5 text-[12.5px]">
                <button type="button" className="text-[#6366f1]">
                  이름 변경
                </button>
                <button type="button" className="text-[#9ca3af]">
                  삭제
                </button>
              </div>
            </div>
          </div>
        ))}
        <Button variant="secondary">+ 컬럼 추가</Button>
      </Section>

      <Section title="라벨" description="카드에 붙일 라벨 색상과 이름을 관리하세요">
        {labels.map((label, i) => (
          <div key={label.id} className="flex w-full flex-col gap-[18px]">
            {i > 0 && <Divider />}
            <div className="flex w-full items-center justify-between">
              <div className="flex items-center gap-2.5">
                <span
                  className="size-3.5 rounded-[4px]"
                  style={{ backgroundColor: label.color }}
                />
                <p className="text-[13.5px] font-medium text-[#111827]">{label.name}</p>
              </div>
              <div className="flex items-center gap-3.5 text-[12.5px]">
                <button type="button" className="text-[#6366f1]">
                  편집
                </button>
                <button type="button" className="text-[#9ca3af]">
                  삭제
                </button>
              </div>
            </div>
          </div>
        ))}
        <Button variant="secondary">+ 라벨 추가</Button>
      </Section>

      <Section title="알림" description="이 프로젝트에서 받을 알림을 설정하세요">
        <div className="flex w-full items-center justify-between">
          <div className="flex flex-col gap-[3px]">
            <p className="text-[13.5px] font-medium text-[#111827]">카드 이동 알림</p>
            <p className="text-[12px] text-[#6b7280]">카드가 다른 컬럼으로 이동하면 알려드려요</p>
          </div>
          <Toggle checked={cardMoveNotif} onChange={setCardMoveNotif} label="카드 이동 알림" />
        </div>
        <Divider />
        <div className="flex w-full items-center justify-between">
          <div className="flex flex-col gap-[3px]">
            <p className="text-[13.5px] font-medium text-[#111827]">댓글 알림</p>
            <p className="text-[12px] text-[#6b7280]">카드에 새 댓글이 달리면 알려드려요</p>
          </div>
          <Toggle checked={commentNotif} onChange={setCommentNotif} label="댓글 알림" />
        </div>
        <Divider />
        <div className="flex w-full items-center justify-between">
          <div className="flex flex-col gap-[3px]">
            <p className="text-[13.5px] font-medium text-[#111827]">마감일 알림</p>
            <p className="text-[12px] text-[#6b7280]">마감일이 임박한 카드를 알려드려요</p>
          </div>
          <Toggle checked={dueDateNotif} onChange={setDueDateNotif} label="마감일 알림" />
        </div>
      </Section>

      <Section
        title="프로젝트 관리"
        description="프로젝트를 보관하거나 영구적으로 삭제할 수 있어요"
        danger
      >
        <div className="flex w-full items-center justify-between">
          <div className="flex flex-col gap-[3px]">
            <p className="text-[13.5px] font-medium text-[#111827]">프로젝트 보관</p>
            <p className="text-[12px] text-[#6b7280]">읽기 전용으로 전환되며 언제든 복구할 수 있어요</p>
          </div>
          <Button variant="secondary">보관하기</Button>
        </div>
        <Divider />
        <div className="flex w-full items-center justify-between">
          <div className="flex flex-col gap-[3px]">
            <p className="text-[13.5px] font-medium text-[#111827]">프로젝트 삭제</p>
            <p className="text-[12px] text-[#6b7280]">
              모든 카드와 데이터가 영구적으로 삭제되며 되돌릴 수 없어요
            </p>
          </div>
          <Button variant="danger">프로젝트 삭제</Button>
        </div>
      </Section>
    </PageShell>
  );
}
