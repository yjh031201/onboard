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

    // 소셜 로그인 전용 계정은 비밀번호가 없다.
    @Column(length = 255)
    private String password;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.MEMBER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuthProvider provider = AuthProvider.LOCAL;

    @Column(name = "provider_id", length = 255)
    private String providerId;

    /** "게스트로 이용"으로 처음 가입한 계정이면 true — 프로젝트 생성은 막히고, 초대받아 멤버로만 참여 가능. */
    @Column(name = "is_guest", nullable = false)
    private boolean guest = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public User(String email, String password, String name, String phone) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.phone = phone;
        this.role = UserRole.MEMBER;
        this.provider = AuthProvider.LOCAL;
    }

    /** 구글/네이버 등 소셜 로그인으로 처음 가입하는 경우 — 비밀번호 없이 생성된다. */
    public User(String email, String name, AuthProvider provider, String providerId) {
        this.email = email;
        this.password = null;
        this.name = name;
        this.role = UserRole.MEMBER;
        this.provider = provider;
        this.providerId = providerId;
    }

    /** 기존 이메일/비밀번호 계정에 소셜 로그인 수단을 연결(link)할 때 사용. */
    public void linkProvider(AuthProvider provider, String providerId) {
        this.provider = provider;
        this.providerId = providerId;
    }

    /** "게스트로 이용" 진입점으로 신규 계정이 만들어질 때만 호출할 것 — 이미 있던 정회원 계정은 격하시키지 않는다. */
    public void markAsGuest() {
        this.guest = true;
    }

    // isGuest()는 클래스의 @Getter가 boolean 필드 guest에 대해 자동 생성해준다 (수동 정의하면 중복 메서드 에러).

    /** 비밀번호 재설정 전용 — 반드시 이미 인코딩된(BCrypt) 값을 넘길 것. */
    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    /** 개인설정에서 이름/휴대폰번호 수정할 때 사용. */
    public void changeProfile(String name, String phone) {
        this.name = name;
        this.phone = phone;
    }

    @jakarta.persistence.PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}