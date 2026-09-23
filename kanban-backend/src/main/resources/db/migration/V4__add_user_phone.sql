-- 아이디/비밀번호 찾기 기능(이름+휴대폰번호로 본인 확인)에 필요한 컬럼.
-- User.java의 phone 필드(@Column(nullable = false, unique = true, length = 20))와 맞춘다.
-- 소셜 로그인 전용 계정(V2)도 회원가입 시 phone을 함께 입력받으므로 NOT NULL로 둔다.

ALTER TABLE users ADD COLUMN phone VARCHAR(20) NOT NULL AFTER name;
ALTER TABLE users ADD UNIQUE KEY uk_users_phone (phone);
