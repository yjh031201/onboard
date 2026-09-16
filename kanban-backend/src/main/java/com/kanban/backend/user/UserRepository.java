package com.kanban.backend.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    /** 아이디(이메일) 찾기용 — 이름+휴대폰번호로 본인 확인. */
    Optional<User> findByNameAndPhone(String name, String phone);

    /** 팀원 초대 시 이메일 또는 휴대폰번호로 이미 가입된 사용자 검색. */
    Optional<User> findByEmailOrPhone(String email, String phone);
}
