import PageShell from "../../components/layout/PageShell";
import { Divider } from "../../components/ui/Section";
import Button from "../../components/ui/Button";

interface FileRow {
  id: string;
  icon: string;
  name: string;
  uploader: string;
  date: string;
  size: string;
}

const FILES: FileRow[] = [
  { id: "1", icon: "📄", name: "요구사항정의서.pdf", uploader: "양종호", date: "9월 2일", size: "1.2MB" },
  { id: "2", icon: "🖼️", name: "와이어프레임_v2.png", uploader: "박지훈", date: "9월 5일", size: "3.4MB" },
  { id: "3", icon: "📊", name: "발표자료.pptx", uploader: "이서연", date: "9월 8일", size: "5.1MB" },
  { id: "4", icon: "🗂️", name: "소스코드.zip", uploader: "김민수", date: "9월 10일", size: "12.8MB" },
  { id: "5", icon: "📄", name: "API_명세서.pdf", uploader: "양종호", date: "9월 12일", size: "0.8MB" },
];

export default function FilesPage() {
  return (
    <PageShell title="파일" subtitle="팀 프로젝트 관련 파일을 업로드하고 관리하세요">
      <div className="flex w-full flex-col items-center justify-center gap-2.5 rounded-[14px] border-[1.5px] border-dashed border-[#c7c8fa] bg-[#f9f9fe] p-10">
        <div className="flex size-12 items-center justify-center rounded-full bg-[#eeeefe]">
          <span className="text-[18px] font-bold text-[#6366f1]">⬆</span>
        </div>
        <p className="text-[14px] font-medium text-[#111827]">
          파일을 이곳에 드래그하거나 클릭해서 업로드하세요
        </p>
        <p className="text-[12.5px] text-[#6b7280]">PDF, 이미지, ZIP 등 파일당 최대 50MB</p>
        <Button variant="primary" className="px-[18px] py-2.5 text-[13px]">
          파일 선택
        </Button>
      </div>

      <div className="flex w-full flex-1 flex-col gap-4 rounded-[14px] border border-[#f0f0f2] bg-white px-6 pt-6 pb-2 shadow-[0px_2px_8px_0px_rgba(0,0,0,0.03)]">
        <div className="flex w-full items-center justify-between">
          <p className="text-[16px] font-bold text-[#111827]">프로젝트 파일</p>
          <p className="text-[12.5px] font-medium text-[#6b7280]">총 {FILES.length}개</p>
        </div>
        {FILES.map((file, i) => (
          <div key={file.id} className="flex w-full flex-col gap-4">
            {i > 0 && <Divider />}
            <div className="flex w-full items-center justify-between py-2.5">
              <div className="flex items-center gap-3">
                <div className="flex size-9 items-center justify-center rounded-lg bg-[#f3f4f6]">
                  <span className="text-[15px]">{file.icon}</span>
                </div>
                <div className="flex flex-col gap-[3px]">
                  <p className="text-[13.5px] font-medium text-[#111827]">{file.name}</p>
                  <p className="text-[12px] text-[#6b7280]">
                    {file.uploader} · {file.date} · {file.size}
                  </p>
                </div>
              </div>
              <div className="flex items-center gap-4">
                <button type="button" className="text-[12.5px] font-medium text-[#6366f1]">
                  다운로드
                </button>
                <button type="button" className="text-[14px] font-bold text-[#9ca3af]">
                  ⋯
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>
    </PageShell>
  );
}
