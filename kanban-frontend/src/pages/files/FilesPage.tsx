import { useEffect, useRef, useState, type DragEvent } from "react";
import PageShell from "../../components/layout/PageShell";
import { Divider } from "../../components/ui/Section";
import Button from "../../components/ui/Button";
import { ApiError } from "../../lib/api";
import { getStoredUser } from "../../lib/auth";
import {
  MAX_FILE_SIZE_BYTES,
  deleteFile,
  downloadFile,
  listFiles,
  uploadFile,
  type ProjectFile,
} from "../../lib/files";

const EXTENSION_ICONS: Record<string, string> = {
  pdf: "📄",
  doc: "📄",
  docx: "📄",
  txt: "📄",
  png: "🖼️",
  jpg: "🖼️",
  jpeg: "🖼️",
  gif: "🖼️",
  svg: "🖼️",
  ppt: "📊",
  pptx: "📊",
  xls: "📊",
  xlsx: "📊",
  zip: "🗂️",
};

function fileIcon(fileName: string): string {
  const ext = fileName.split(".").pop()?.toLowerCase() ?? "";
  return EXTENSION_ICONS[ext] ?? "📎";
}

function formatSize(bytes: number): string {
  if (bytes < 1024 * 1024) return `${Math.max(1, Math.round(bytes / 1024))}KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)}MB`;
}

function formatDate(iso: string): string {
  const d = new Date(iso);
  return `${d.getMonth() + 1}월 ${d.getDate()}일`;
}

function errorMessage(err: unknown, fallback: string): string {
  return err instanceof ApiError ? err.message : fallback;
}

export default function FilesPage() {
  const currentUser = getStoredUser();
  const inputRef = useRef<HTMLInputElement>(null);

  const [files, setFiles] = useState<ProjectFile[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [uploading, setUploading] = useState(false);
  const [dragOver, setDragOver] = useState(false);

  useEffect(() => {
    listFiles()
      .then(setFiles)
      .catch((err) => setError(errorMessage(err, "파일 목록을 불러오지 못했어요.")))
      .finally(() => setLoading(false));
  }, []);

  // 업로더 본인이거나 OWNER/ADMIN이면 삭제 가능 (서버에서도 동일하게 검증됨).
  const canDelete = (file: ProjectFile) =>
    file.uploadedBy === currentUser?.id || currentUser?.role === "OWNER" || currentUser?.role === "ADMIN";

  const handleUpload = async (selected: FileList | null) => {
    if (!selected || selected.length === 0 || uploading) return;

    const tooLarge = Array.from(selected).filter((f) => f.size > MAX_FILE_SIZE_BYTES);
    if (tooLarge.length > 0) {
      setError(`파일당 최대 50MB까지 올릴 수 있어요: ${tooLarge.map((f) => f.name).join(", ")}`);
      return;
    }

    setUploading(true);
    setError(null);
    try {
      for (const file of Array.from(selected)) {
        const uploaded = await uploadFile(file);
        setFiles((prev) => [uploaded, ...prev]);
      }
    } catch (err) {
      setError(errorMessage(err, "파일을 업로드하지 못했어요."));
    } finally {
      setUploading(false);
      if (inputRef.current) inputRef.current.value = "";
    }
  };

  const handleDrop = (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    setDragOver(false);
    handleUpload(e.dataTransfer.files);
  };

  const handleDownload = (file: ProjectFile) => {
    setError(null);
    downloadFile(file).catch((err) => setError(errorMessage(err, "파일을 다운로드하지 못했어요.")));
  };

  const handleDelete = async (file: ProjectFile) => {
    if (!window.confirm(`'${file.fileName}' 파일을 삭제할까요?`)) return;
    setError(null);
    try {
      await deleteFile(file.id);
      setFiles((prev) => prev.filter((f) => f.id !== file.id));
    } catch (err) {
      setError(errorMessage(err, "파일을 삭제하지 못했어요."));
    }
  };

  return (
    <PageShell title="파일" subtitle="팀 프로젝트 관련 파일을 업로드하고 관리하세요">
      <div
        onClick={() => inputRef.current?.click()}
        onDragOver={(e) => {
          e.preventDefault();
          setDragOver(true);
        }}
        onDragLeave={() => setDragOver(false)}
        onDrop={handleDrop}
        className={`flex w-full cursor-pointer flex-col items-center justify-center gap-2.5 rounded-[14px] border-[1.5px] border-dashed p-10 ${
          dragOver ? "border-[#6366f1] bg-[#eeeefe]" : "border-[#c7c8fa] bg-[#f9f9fe]"
        }`}
      >
        <input
          ref={inputRef}
          type="file"
          multiple
          className="hidden"
          onChange={(e) => handleUpload(e.target.files)}
        />
        <div className="flex size-12 items-center justify-center rounded-full bg-[#eeeefe]">
          <span className="text-[18px] font-bold text-[#6366f1]">⬆</span>
        </div>
        <p className="text-[14px] font-medium text-[#111827]">
          {uploading ? "업로드 중..." : "파일을 이곳에 드래그하거나 클릭해서 업로드하세요"}
        </p>
        <p className="text-[12.5px] text-[#6b7280]">PDF, 이미지, ZIP 등 파일당 최대 50MB</p>
        <Button
          variant="primary"
          disabled={uploading}
          className="px-[18px] py-2.5 text-[13px] disabled:opacity-60"
          onClick={(e) => {
            e.stopPropagation();
            inputRef.current?.click();
          }}
        >
          파일 선택
        </Button>
      </div>

      {error && <p className="w-full text-[13px] text-[#ef4444]">{error}</p>}

      <div className="flex w-full flex-1 flex-col gap-4 rounded-[14px] border border-[#f0f0f2] bg-white px-6 pt-6 pb-2 shadow-[0px_2px_8px_0px_rgba(0,0,0,0.03)]">
        <div className="flex w-full items-center justify-between">
          <p className="text-[16px] font-bold text-[#111827]">프로젝트 파일</p>
          <p className="text-[12.5px] font-medium text-[#6b7280]">총 {files.length}개</p>
        </div>
        {loading && <p className="pb-4 text-[13px] text-[#9ca3af]">불러오는 중...</p>}
        {!loading && files.length === 0 && (
          <p className="pb-4 text-[13px] text-[#9ca3af]">아직 업로드된 파일이 없어요.</p>
        )}
        {files.map((file, i) => (
          <div key={file.id} className="flex w-full flex-col gap-4">
            {i > 0 && <Divider />}
            <div className="flex w-full items-center justify-between py-2.5">
              <div className="flex items-center gap-3">
                <div className="flex size-9 items-center justify-center rounded-lg bg-[#f3f4f6]">
                  <span className="text-[15px]">{fileIcon(file.fileName)}</span>
                </div>
                <div className="flex flex-col gap-[3px]">
                  <p className="text-[13.5px] font-medium text-[#111827]">{file.fileName}</p>
                  <p className="text-[12px] text-[#6b7280]">
                    {file.uploaderName ?? "알 수 없음"} · {formatDate(file.createdAt)} ·{" "}
                    {formatSize(file.fileSize)}
                  </p>
                </div>
              </div>
              <div className="flex items-center gap-4">
                <button
                  type="button"
                  onClick={() => handleDownload(file)}
                  className="text-[12.5px] font-medium text-[#6366f1]"
                >
                  다운로드
                </button>
                {canDelete(file) && (
                  <button
                    type="button"
                    onClick={() => handleDelete(file)}
                    className="text-[12.5px] font-medium text-[#9ca3af] hover:text-[#ef4444]"
                  >
                    삭제
                  </button>
                )}
              </div>
            </div>
          </div>
        ))}
      </div>
    </PageShell>
  );
}
