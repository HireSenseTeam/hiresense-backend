package com.hiresense.auth.service;

import com.hiresense.auth.dto.request.LoginRequest;
import com.hiresense.auth.dto.request.SignUpRequest;
import com.hiresense.auth.dto.response.AuthResponse;
import com.hiresense.global.error.BusinessException;
import com.hiresense.global.error.ErrorCode;
import com.hiresense.user.domain.User;
import com.hiresense.user.domain.UserRole;
import com.hiresense.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse signUp(SignUpRequest request) {
        // 이메일 중복 확인
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "이미 사용 중인 이메일입니다.");
        }

        // 역할 검증
        UserRole role;
        try {
            role = UserRole.valueOf(request.role().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "올바른 역할이 아닙니다. (APPLICANT 또는 COMPANY)");
        }

        // 사용자 생성
        User user;
        if (role == UserRole.APPLICANT) {
            user = User.createApplicant(
                    request.email(),
                    passwordEncoder.encode(request.password()),
                    request.name()
            );
        } else {
            user = User.createCompany(
                    request.email(),
                    passwordEncoder.encode(request.password()),
                    request.name()
            );
        }

        User savedUser = userRepository.save(user);
        log.info("회원가입 완료: userId={}, email={}, role={}", savedUser.getId(), savedUser.getEmail(), savedUser.getRole());

        // JWT 토큰 생성
        String token = jwtService.generateToken(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getRole().name()
        );

        return new AuthResponse(
                token,
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getName(),
                savedUser.getRole()
        );
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "이메일 또는 비밀번호가 올바르지 않습니다."));

        // 비밀번호 확인
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        // 활성화 확인
        if (!user.getActive()) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "비활성화된 계정입니다.");
        }

        log.info("로그인 완료: userId={}, email={}, role={}", user.getId(), user.getEmail(), user.getRole());

        // JWT 토큰 생성
        String token = jwtService.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        return new AuthResponse(
                token,
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole()
        );
    }
}

