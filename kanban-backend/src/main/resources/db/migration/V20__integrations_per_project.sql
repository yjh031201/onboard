-- 연동(GitHub/Slack/Google Drive)을 서버 전체 1개에서 "프로젝트마다 1개"로 바꾼다.
-- 예전에는 provider당 한 행뿐이라, 다른 프로젝트 관리자가 연결하면 앞 프로젝트의 연결을 덮어썼다.

ALTER TABLE integrations ADD COLUMN project_id BIGINT NULL AFTER id;

-- 기존 연결은 연결한 사람이 OWNER/ADMIN으로 속한 프로젝트 중 가장 먼저 만든 프로젝트로 옮긴다.
UPDATE integrations i
SET i.project_id = (
    SELECT MIN(pm.project_id)
    FROM project_members pm
    WHERE pm.user_id = i.connected_by_id
      AND pm.status = 'ACCEPTED'
      AND pm.role IN ('OWNER', 'ADMIN')
);

-- 옮길 프로젝트가 없는 연결(연결한 사람이 이제 어느 프로젝트의 관리자도 아님)은 지운다 — 다시 연결하면 된다.
DELETE FROM integrations WHERE project_id IS NULL;

ALTER TABLE integrations DROP INDEX uk_integrations_provider;
ALTER TABLE integrations MODIFY project_id BIGINT NOT NULL;
ALTER TABLE integrations ADD CONSTRAINT uk_integrations_project_provider UNIQUE (project_id, provider);
ALTER TABLE integrations ADD CONSTRAINT fk_integrations_project FOREIGN KEY (project_id) REFERENCES projects (id);
