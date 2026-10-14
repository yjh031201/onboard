-- 일정(schedules)과 파일(project_files)을 프로젝트별로 나눈다.
-- 예전에는 프로젝트 구분이 없어서 A 프로젝트에 올린 일정·파일이 B 프로젝트 멤버에게도 보였다.
--
-- 기존 데이터는 만든 사람이 (수락한) 멤버로 속한 프로젝트 중 가장 먼저 만든 프로젝트로 옮긴다.
-- 그런 프로젝트가 없으면(탈퇴 등) 가장 오래된 프로젝트로 옮긴다 — V13에서 기존 데이터를 프로젝트 #1로 옮긴 것과 같은 기준.

-- 1) 일정
ALTER TABLE schedules ADD COLUMN project_id BIGINT NULL AFTER id;

UPDATE schedules s
SET s.project_id = COALESCE(
    (SELECT MIN(pm.project_id) FROM project_members pm
     WHERE pm.user_id = s.created_by AND pm.status = 'ACCEPTED'),
    (SELECT MIN(p.id) FROM projects p)
);

-- 프로젝트가 하나도 없는 경우에만 남는다 — 어느 프로젝트에도 보여줄 수 없으니 지운다.
DELETE FROM schedules WHERE project_id IS NULL;

ALTER TABLE schedules MODIFY project_id BIGINT NOT NULL;
ALTER TABLE schedules ADD CONSTRAINT fk_schedules_project FOREIGN KEY (project_id) REFERENCES projects (id);
CREATE INDEX idx_schedules_project_date ON schedules (project_id, start_date);

-- 2) 파일
ALTER TABLE project_files ADD COLUMN project_id BIGINT NULL AFTER id;

UPDATE project_files f
SET f.project_id = COALESCE(
    (SELECT MIN(pm.project_id) FROM project_members pm
     WHERE pm.user_id = f.uploaded_by AND pm.status = 'ACCEPTED'),
    (SELECT MIN(p.id) FROM projects p)
);

DELETE FROM project_files WHERE project_id IS NULL;

ALTER TABLE project_files MODIFY project_id BIGINT NOT NULL;
ALTER TABLE project_files ADD CONSTRAINT fk_project_files_project FOREIGN KEY (project_id) REFERENCES projects (id);
CREATE INDEX idx_project_files_project_created ON project_files (project_id, created_at);
