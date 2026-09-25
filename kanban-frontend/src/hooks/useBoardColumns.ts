import { useEffect, useState } from "react";
import { fetchColumns, type ColumnDef } from "../lib/boardColumns";
import { subscribeTopic } from "../lib/realtime";

/** 보드 컬럼 목록을 불러오고, 이후에는 /topic/columns로 오는 전체 목록(추가/변경/삭제 후)을 그대로 반영한다. */
export function useBoardColumns() {
  const [columns, setColumns] = useState<ColumnDef[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;

    fetchColumns()
      .then((data) => {
        if (!cancelled) setColumns(data);
      })
      .catch(() => {
        /* 실패하면 빈 보드로 보여주고, 다음 /topic/columns 이벤트 때 맞춰진다. */
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    const unsubscribe = subscribeTopic<ColumnDef[]>("/topic/columns", setColumns);

    return () => {
      cancelled = true;
      unsubscribe();
    };
  }, []);

  return { columns, loading };
}
