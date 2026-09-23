-- 일정을 하루 단위에서 기간(시작일~종료일) 단위로 확장.
-- 기존 schedule_date는 시작일(start_date)이 되고, 종료일(end_date)은 시작일과 같게 채운다.
-- Schedule.java의 startDate/endDate 필드와 맞춘다. start_time은 시작일, end_time은 종료일 기준.

ALTER TABLE schedules RENAME COLUMN schedule_date TO start_date;
ALTER TABLE schedules ADD COLUMN end_date DATE NULL AFTER start_date;
UPDATE schedules SET end_date = start_date;
ALTER TABLE schedules MODIFY COLUMN end_date DATE NOT NULL;
