import { useEffect, useState } from "react";
import { fetchIntegrations } from "../lib/integrations";

/** 칸반에서 쓸 이 프로젝트의 연동 상태 — GitHub는 저장소까지 지정돼 있어야 이슈를 쓸 수 있다. */
export interface ConnectedIntegrations {
  /** 연결된 저장소 "owner/repo". GitHub 미연결이거나 저장소 미지정이면 null. */
  githubRepo: string | null;
  drive: boolean;
}

export function useConnectedIntegrations(projectId: number): ConnectedIntegrations {
  const [state, setState] = useState<ConnectedIntegrations>({ githubRepo: null, drive: false });

  useEffect(() => {
    let cancelled = false;
    // 다른 프로젝트로 옮겨 오면 앞 프로젝트의 연동 상태가 남지 않게 비우고 다시 불러온다 (연동은 프로젝트마다 따로다).
    setState({ githubRepo: null, drive: false });
    fetchIntegrations(projectId)
      .then((list) => {
        if (cancelled) return;
        const github = list.find((item) => item.provider === "GITHUB");
        const drive = list.find((item) => item.provider === "GOOGLE_DRIVE");
        setState({
          githubRepo: github?.connected && github.detail ? github.detail : null,
          drive: drive?.connected ?? false,
        });
      })
      .catch(() => {
        // 연동 정보를 못 받아도 칸반은 그대로 쓸 수 있다 — 연동 버튼만 안 보인다.
      });
    return () => {
      cancelled = true;
    };
  }, [projectId]);

  return state;
}
