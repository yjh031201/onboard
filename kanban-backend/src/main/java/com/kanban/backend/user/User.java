package com.kanban.backend.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 의도적으로 setter를 두지 않음 — 필드를 외부에서 마음대로 바꾸지 못하게 캡슐화.
 * id/createdAt은 JPA/DB가 관리하고, email/password/name/role은 생성자로만 값이 정해짐.
 * 나중에 "이름 변경", "권한 변경" 같은 기능이 필요해지면 changeName(), changeRole() 처럼
 * 의도가 드러나는 전용 메서드를 추가할 것 (무분별한 setXxx() 금지).
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 100)
    private String name;

<<<<<<< HEAD
=======
    @Column(nullable = false, unique = true, length = 20)
    private String phone;

>>>>>>> origin/feature/teamsetting
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.MEMBER;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

<<<<<<< HEAD
    public User(String email, String password, String name) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.role = UserRole.MEMBER;
    }

=======
    public User(String email, String password, String name, String phone) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.phone = phone;
        this.role = UserRole.MEMBER;
    }

    /** 비밀번호 재설정 전용 — 반드시 이미 인코딩된(BCrypt) 값을 넘길 것. */
    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    /** 개인설정에서 이름/휴대폰번호 수정할 때 사용. */
    public void changeProfile(String name, String phone) {
        this.name = name;
        this.phone = phone;
    }

    /** 팀원 권한 변경 시 사용 — OWNER/ADMIN만 호출 가능하도록 UserService에서 제한. */
    public void changeRole(UserRole role) {
        this.role = role;
    }

>>>>>>> origin/feature/teamsetting
    @jakarta.persistence.PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
