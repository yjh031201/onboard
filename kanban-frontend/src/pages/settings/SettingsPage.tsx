import { useEffect, useState } from "react";
import PageShell from "../../components/layout/PageShell";
import Section, { Divider } from "../../components/ui/Section";
import { Field, TextareaField } from "../../components/ui/Field";
import Button from "../../components/ui/Button";
import Toggle from "../../components/ui/Toggle";
import { ApiError } from "../../lib/api";
import { getStoredUser } from "../../lib/auth";
import ColumnSettings from "../../components/settings/ColumnSettings";
import LabelSettings from "../../components/settings/LabelSettings";
import {
  archiveProject,
  deleteProject,
  getProjectSettings,
  subscribeProjectSettings,
  unarchiveProject,
  updateProjectSettings,
} from "../../lib/settings";
import {
  getNotificationSettings,
  setNotificationSetting,
  type NotificationSettings,
} from "../../lib/notificationSettings";

export default function SettingsPage() {
  const currentUser = getStoredUser();
  // OWNER/ADMIN만 프로젝트 정보를 바꿀 수 있음 (서버에서도 동일하게 검증됨).
  const canEditProject = currentUser?.role === "OWNER" || currentUser?.role === "ADMIN";
  // 프로젝트 삭제는 OWNER만 (서버에서도 동일하게 검증됨).
  const canDeleteProject = currentUser?.role === "OWNER";

  // 서버에 저장된 이름 — 삭제 확인에 쓴다 (입력칸의 projectName은 저장 전 수정 중인 값일 수 있다).
  const [savedProjectName, setSavedProjectName] = useState("");
  const [archived, setArchived] = useState(false);
  const [managing, setManaging] = useState(false);
  const [manageError, setManageError] = useState<string | null>(null);

  const [projectName, setProjectName] = useState("");
  const [description, setDescription] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<{ type: "error" | "success"; text: string } | null>(null);
  const [notifSettings, setNotifSettings] = useState(getNotificationSettings);

  const updateNotifSetting = (key: keyof NotificationSettings, value: boolean) => {
    setNotifSettings(setNotificationSetting(key, value));
  };

  useEffect(() => {
    getProjectSettings()
      .then((settings) => {
        setProjectName(settings.projectName);
        setSavedProjectName(settings.projectName);
        setDescription(settings.description ?? "");
        setArchived(settings.archived);
      })
      .catch((err) =>
        setMessage({
          type: "error",
          text: err instanceof ApiError ? err.message : "프로젝트 설정을 불러오지 못했어요.",
        }),
      )
      .finally(() => setLoading(false));

    // 다른 관리자가 보관하거나 보관을 풀어도 바로 반영한다.
    return subscribeProjectSettings((settings) => setArchived(settings.archived));
  }, []);

  const handleToggleArchive = async () => {
    const question = archived
      ? "프로젝트 보관을 해제할까요?\n다시 카드와 일정, 파일을 수정할 수 있게 돼요."
      : "프로젝트를 보관할까요?\n보관을 해제할 때까지 모든 팀원에게 읽기 전용이 돼요.";
    if (!window.confirm(question)) return;

    setManaging(true);
    setManageError(null);
    try {
      const saved = await (archived ? unarchiveProject() : archiveProject());
      setArchived(saved.archived);
      setSavedProjectName(saved.projectName);
      // 한 번도 저장한 적 없는 프로젝트를 보관하면 서버가 기본 이름을 채운다.
      if (!projectName.trim()) setProjectName(saved.projectName);
    } catch (err) {
      setManageError(err instanceof ApiError ? err.message : "프로젝트 보관 상태를 바꾸지 못했어요.");
    } finally {
      setManaging(false);
    }
  };

  const handleDeleteProject = async () => {
    // 되돌릴 수 없는 작업이라 실수로 누른 것이 아닌지, 프로젝트 이름을 직접 입력받아 확인한다.
    const confirmText = savedProjectName.trim() || "삭제";
    const typed = window.prompt(
      "프로젝트의 모든 카드, 일정, 파일, 활동 기록이 영구적으로 삭제되며 되돌릴 수 없어요.\n" +
        "(팀원 계정과 컬럼·라벨 구성은 남아요.)\n\n" +
        `계속하려면 "${confirmText}"을(를) 입력하세요.`,
    );
    if (typed === null) return;
    if (typed.trim() !== confirmText) {
      setManageError("입력한 내용이 일치하지 않아 삭제하지 않았어요.");
      return;
    }

    setManaging(true);
    setManageError(null);
    try {
      await deleteProject();
      window.alert("프로젝트를 삭제했어요.");
      // 화면 곳곳에 남아 있는 카드·일정·파일 목록을 한 번에 비우기 위해 새로 불러온다.
      window.location.assign("/dashboard");
    } catch (err) {
      setManageError(err instanceof ApiError ? err.message : "프로젝트를 삭제하지 못했어요.");
      setManaging(false);
    }
  };

  const handleSave = async () => {
    if (!projectName.trim()) {
      setMessage({ type: "error", text: "프로젝트 이름을 입력해주세요." });
      return;
    }
    setSaving(true);
    setMessage(null);
    try {
      const saved = await updateProjectSettings(projectName.trim(), description);
      setProjectName(saved.projectName);
      setSavedProjectName(saved.projectName);
      setDescription(saved.description ?? "");
      setMessage({ type: "success", text: "저장했어요." });
    } catch (err) {
      setMessage({
        type: "error",
        text: err instanceof ApiError ? err.message : "프로젝트 설정을 저장하지 못했어요.",
      });
    } finally {
      setSaving(false);
    }
  };

  return (
    <PageShell title="설정" subtitle="프로젝트와 보드 설정을 관리하세요">
      <Section title="일반" description="프로젝트의 기본 정보를 관리하세요">
        <Field
          label="프로젝트 이름"
          placeholder={loading ? "불러오는 중..." : "프로젝트 이름을 입력하세요"}
          maxLength={100}
          disabled={loading || !canEditProject || archived}
          value={projectName}
          onChange={(e) => setProjectName(e.target.value)}
        />
        <TextareaField
          label="설명"
          rows={2}
          maxLength={2000}
          disabled={loading || !canEditProject || archived}
          value={description}
          onChange={(e) => setDescription(e.target.value)}
        />
        <div className="flex w-full items-center justify-between gap-3">
          <p
            className={`text-[12.5px] ${
              message?.type === "error" ? "text-[#ef4444]" : "text-[#6b7280]"
            }`}
          >
            {message?.text ??
              (!canEditProject
                ? "프로젝트 정보는 소유자/관리자만 변경할 수 있어요."
                : archived
                  ? "보관된 프로젝트는 읽기 전용이에요."
                  : "")}
          </p>
          {canEditProject && (
            <Button
              variant="primary"
              disabled={loading || saving || archived}
              onClick={handleSave}
              className="shrink-0 disabled:opacity-60"
            >
              {saving ? "저장 중..." : "변경사항 저장"}
            </Button>
          )}
        </div>
      </Section>

      <ColumnSettings />

      <LabelSettings />

      <Section title="알림" description="이 프로젝트에서 받을 알림을 설정하세요">
        <div className="flex w-full items-center justify-between">
          <div className="flex flex-col gap-[3px]">
            <p className="text-[13.5px] font-medium text-[#111827]">카드 이동 알림</p>
            <p className="text-[12px] text-[#6b7280]">카드가 다른 컬럼으로 이동하면 알려드려요</p>
          </div>
          <Toggle
            checked={notifSettings.cardMoveNotif}
            onChange={(next) => updateNotifSetting("cardMoveNotif", next)}
            label="카드 이동 알림"
          />
        </div>
        <Divider />
        <div className="flex w-full items-center justify-between">
          <div className="flex flex-col gap-[3px]">
            <p className="text-[13.5px] font-medium text-[#111827]">새 카드 알림</p>
            <p className="text-[12px] text-[#6b7280]">새로운 카드가 만들어지면 알려드려요</p>
          </div>
          <Toggle
            checked={notifSettings.cardCreateNotif}
            onChange={(next) => updateNotifSetting("cardCreateNotif", next)}
            label="새 카드 알림"
          />
        </div>
        <Divider />
        <div className="flex w-full items-center justify-between">
          <div className="flex flex-col gap-[3px]">
            <p className="text-[13.5px] font-medium text-[#111827]">라벨 변경 알림</p>
            <p className="text-[12px] text-[#6b7280]">카드에 라벨이 추가되거나 바뀌면 알려드려요</p>
          </div>
          <Toggle
            checked={notifSettings.labelChangeNotif}
            onChange={(next) => updateNotifSetting("labelChangeNotif", next)}
            label="라벨 변경 알림"
          />
        </div>
        <Divider />
        <div className="flex w-full items-center justify-between">
          <div className="flex flex-col gap-[3px]">
            <p className="text-[13.5px] font-medium text-[#111827]">마감일 알림</p>
            <p className="text-[12px] text-[#6b7280]">마감일이 임박한 카드를 알려드려요</p>
          </div>
          <Toggle
            checked={notifSettings.dueDateNotif}
            onChange={(next) => updateNotifSetting("dueDateNotif", next)}
            label="마감일 알림"
          />
        </div>
      </Section>

      <Section
        title="프로젝트 관리"
        description="프로젝트를 보관하거나 영구적으로 삭제할 수 있어요"
        danger
      >
        <div className="flex w-full items-center justify-between">
          <div className="flex flex-col gap-[3px]">
            <p className="text-[13.5px] font-medium text-[#111827]">
              {archived ? "프로젝트 보관 해제" : "프로젝트 보관"}
            </p>
            <p className="text-[12px] text-[#6b7280]">
              {archived
                ? "지금은 보관 중이라 읽기 전용이에요. 해제하면 다시 수정할 수 있어요"
                : "읽기 전용으로 전환되며 언제든 복구할 수 있어요"}
            </p>
          </div>
          <Button
            variant="secondary"
            disabled={loading || managing || !canEditProject}
            title={canEditProject ? undefined : "소유자/관리자만 할 수 있어요"}
            onClick={handleToggleArchive}
            className="shrink-0 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {archived ? "보관 해제" : "보관하기"}
          </Button>
        </div>
        <Divider />
        <div className="flex w-full items-center justify-between gap-3">
          <div className="flex flex-col gap-[3px]">
            <p className="text-[13.5px] font-medium text-[#111827]">프로젝트 삭제</p>
            <p className="text-[12px] text-[#6b7280]">
              모든 카드와 데이터가 영구적으로 삭제되며 되돌릴 수 없어요
            </p>
          </div>
          <Button
            variant="danger"
            disabled={loading || managing || !canDeleteProject}
            title={canDeleteProject ? undefined : "소유자만 삭제할 수 있어요"}
            onClick={handleDeleteProject}
            className="shrink-0 disabled:cursor-not-allowed disabled:opacity-60"
          >
            프로젝트 삭제
          </Button>
        </div>
        {manageError && <p className="text-[12.5px] text-[#ef4444]">{manageError}</p>}
      </Section>
    </PageShell>
  );
}
