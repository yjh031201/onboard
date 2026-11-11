// 카드 앞면의 댓글 표시 — "💬 3"에 안 읽은 댓글이 있으면 "+1" 배지를 붙인다.
// 칸반 페이지와 대시보드 칸반 위젯이 같이 쓴다. 상세 창을 열어 읽으면 배지가 사라진다.

import { unreadCommentCount, useCommentReads } from "../../lib/commentReads";
import type { TaskCard } from "../../types/dashboard";

export default function CommentBadge({ projectId, task }: { projectId: number; task: TaskCard }) {
  const reads = useCommentReads(projectId);
  if (task.commentCount === 0) return null;
  const unread = unreadCommentCount(reads, task);

  return (
    <span
      className="flex items-center gap-1 text-[11px] text-[#6b7280]"
      title={unread > 0 ? `댓글 ${task.commentCount}개 · 새 댓글 ${unread}개` : `댓글 ${task.commentCount}개`}
    >
      💬 {task.commentCount}
      {unread > 0 && (
        <span className="rounded-full bg-[#6366f1] px-1.5 py-px text-[10px] font-semibold leading-[14px] text-white">
          +{unread}
        </span>
      )}
    </span>
  );
}
