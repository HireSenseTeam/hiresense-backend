package com.hiresense.user.domain;

import com.hiresense.global.entity.BaseTimeEntity;
import com.hiresense.global.error.BusinessException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users")
public class User extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(nullable = false)
    private Boolean active = true;

    @Builder
    private User(String email, String password, String name, UserRole role) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.role = role;
        this.active = true;
    }

    public static User createApplicant(String email, String password, String name) {
        return User.builder()
                .email(email)
                .password(password)
                .name(name)
                .role(UserRole.APPLICANT)
                .build();
    }

    public static User createCompany(String email, String password, String name) {
        return User.builder()
                .email(email)
                .password(password)
                .name(name)
                .role(UserRole.COMPANY)
                .build();
    }

    public void validatePassword(String rawPassword, org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        if (!passwordEncoder.matches(rawPassword, this.password)) {
            throw new BusinessException(com.hiresense.global.error.ErrorCode.AUTH_INVALID_CREDENTIALS);
        }
    }
}
