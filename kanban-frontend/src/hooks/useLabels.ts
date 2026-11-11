import { useEffect, useState } from "react";
import { fetchLabels, type LabelDef } from "../lib/labels";
import { onRealtimeReconnect, subscribeTopic } from "../lib/realtime";

/**
 * 라벨 목록을 불러오고, 이후에는 /topic/projects/{projectId}/labels로 오는 전체 목록(추가/편집/삭제 후)을 그대로 반영한다.
 * 실시간 연결이 끊겼다 다시 붙으면 다시 불러온다.
 */
export function useLabels(projectId: number) {
  const [labels, setLabels] = useState<LabelDef[]>([]);

  useEffect(() => {
    let cancelled = false;

    const load = () =>
      fetchLabels(projectId)
        .then((data) => {
          if (!cancelled) setLabels(data);
        })
        .catch(() => {
          /* 실패하면 라벨 없이 보여주고, 다음 /topic/labels 이벤트 때 맞춰진다. */
        });
    void load();

    const unsubscribe = subscribeTopic<LabelDef[]>(`/topic/projects/${projectId}/labels`, setLabels);
    const unsubscribeReconnect = onRealtimeReconnect(() => void load());

    return () => {
      cancelled = true;
      unsubscribe();
      unsubscribeReconnect();
    };
  }, [projectId]);

  return labels;
}
