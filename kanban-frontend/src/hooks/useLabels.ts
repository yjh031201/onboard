import { useEffect, useState } from "react";
import { fetchLabels, type LabelDef } from "../lib/labels";
import { subscribeTopic } from "../lib/realtime";

/** 라벨 목록을 불러오고, 이후에는 /topic/labels로 오는 전체 목록(추가/편집/삭제 후)을 그대로 반영한다. */
export function useLabels() {
  const [labels, setLabels] = useState<LabelDef[]>([]);

  useEffect(() => {
    let cancelled = false;

    fetchLabels()
      .then((data) => {
        if (!cancelled) setLabels(data);
      })
      .catch(() => {
        /* 실패하면 라벨 없이 보여주고, 다음 /topic/labels 이벤트 때 맞춰진다. */
      });

    const unsubscribe = subscribeTopic<LabelDef[]>("/topic/labels", setLabels);

    return () => {
      cancelled = true;
      unsubscribe();
    };
  }, []);

  return labels;
}
