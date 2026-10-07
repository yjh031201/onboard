-- 설정 페이지 "프로젝트 보관" — 보관 중에는 그 프로젝트가 읽기 전용이 된다 (ArchiveGuardInterceptor).
-- 멀티 프로젝트 구조라 project_settings가 아니라 projects에 붙인다. archived_at은 보관한 시각, 해제하면 다시 NULL.
ALTER TABLE projects
    ADD COLUMN archived    BOOLEAN  NOT NULL DEFAULT FALSE,
    ADD COLUMN archived_at DATETIME NULL;
