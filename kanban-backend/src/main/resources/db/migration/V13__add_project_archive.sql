-- 설정 페이지 "프로젝트 보관" — 보관 중에는 프로젝트가 읽기 전용이 된다 (ArchiveGuardInterceptor).
-- archived_at은 보관한 시각. 보관을 풀면 다시 NULL.
ALTER TABLE project_settings
    ADD COLUMN archived    BOOLEAN  NOT NULL DEFAULT FALSE,
    ADD COLUMN archived_at DATETIME NULL;
