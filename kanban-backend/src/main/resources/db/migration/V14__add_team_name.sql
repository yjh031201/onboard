-- 팀원 페이지 "팀 이름". 아직 아무도 바꾸지 않았으면 NULL이고 화면이 기본 이름을 보여준다.
ALTER TABLE project_settings
    ADD COLUMN team_name VARCHAR(100) NULL;
