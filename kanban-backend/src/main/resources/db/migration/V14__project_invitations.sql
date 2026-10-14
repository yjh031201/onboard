-- 프로젝트 멤버 초대에 수락/거절 절차를 추가한다.
-- 기존에는 초대 = 즉시 멤버 추가였는데, 이제는 초대를 보내면 PENDING으로 생기고
-- 초대받은 사람이 수락해야 실제 멤버(ACCEPTED)가 된다. 기존 행(이미 다 실제 멤버였던 것들)은
-- 전부 ACCEPTED로 백필한다.

ALTER TABLE project_members
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACCEPTED';

CREATE INDEX idx_project_members_user_status ON project_members (user_id, status);
