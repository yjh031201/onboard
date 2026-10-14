-- 구글/네이버 소셜 로그인은 email,profile(네이버는 name,email) 스코프만 요청하기 때문에
-- 휴대폰번호를 아예 못 받는다. 그런데 V4에서 phone을 NOT NULL로 만들어둬서, 처음 보는 이메일로
-- 소셜 로그인하면(CustomOAuth2UserService.createUser) phone이 null인 채로 INSERT를 시도하다가
-- "Column 'phone' cannot be null" 제약 위반으로 회원가입 자체가 실패하는 버그가 있었다.
-- 로컬(이메일/비밀번호) 가입은 여전히 phone을 필수로 받으니(AuthService 쪽 검증) 실질적으로는
-- "소셜 로그인 계정만 비어있을 수 있다"가 된다 — unique 제약은 그대로 둬도 MySQL은 NULL끼리는
-- unique 충돌로 안 치므로 문제 없음.
ALTER TABLE users MODIFY COLUMN phone VARCHAR(20) NULL;
