-- 일정 모달(카테고리/시작·종료 시간/색상 태그)에 맞춰 schedules 테이블 확장.
-- Schedule.java의 category/startTime/endTime/color 필드와 맞춘다.
-- 시간은 선택 입력이라 NULL 허용(하루 종일 일정).

ALTER TABLE schedules ADD COLUMN category   VARCHAR(20) NOT NULL DEFAULT 'OTHER' AFTER content;
ALTER TABLE schedules ADD COLUMN start_time TIME        NULL AFTER schedule_date;
ALTER TABLE schedules ADD COLUMN end_time   TIME        NULL AFTER start_time;
ALTER TABLE schedules ADD COLUMN color      VARCHAR(7)  NULL AFTER end_time;
