package com.hiresense.auth.service;

import com.hiresense.auth.dto.request.LoginRequest;
import com.hiresense.auth.dto.request.RefreshTokenRequest;
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
    private final RefreshTokenService refreshTokenService;
    private final TokenBlacklistService tokenBlacklistService;

    @Transactional
    public AuthResponse signUp(SignUpRequest request) {
        validateEmailDoesNotExist(request.email());
        UserRole role = parseUserRole(request.role());

        User user = buildUser(request, role);
        User savedUser = userRepository.save(user);
        log.info("회원가입 완료: userId={}, email={}, role={}", savedUser.getId(), savedUser.getEmail(), savedUser.getRole());

        return createAuthResponse(savedUser);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS));

        user.validatePassword(request.password(), passwordEncoder);
        validateUserIsActive(user);

        log.info("로그인 완료: userId={}, email={}, role={}", user.getId(), user.getEmail(), user.getRole());

        return createAuthResponse(user);
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        User user = refreshTokenService.validateAndGetUserFromToken(request.refreshToken());

        refreshTokenService.deleteRefreshToken(user.getId());

        log.info("토큰 재발급 완료: userId={}, email={}, role={}", user.getId(), user.getEmail(), user.getRole());

        return createAuthResponse(user);
    }

    @Transactional
    public void logout(Long userId, String accessToken) {
        refreshTokenService.deleteRefreshToken(userId);
        
        if (accessToken != null && !accessToken.isEmpty()) {
            try {
                long expirationTime = jwtService.extractExpiration(accessToken).getTime();
                long remainingTime = expirationTime - System.currentTimeMillis();
                
                if (remainingTime > 0) {
                    tokenBlacklistService.addToBlacklist(accessToken, remainingTime);
                    log.info("Access Token 블랙리스트 추가 완료: userId={}, remainingTime={}ms", userId, remainingTime);
                }
            } catch (Exception e) {
                log.warn("Access Token 블랙리스트 추가 실패 (토큰 파싱 오류): userId={}, error={}", userId, e.getMessage());
            }
        }
        
        log.info("로그아웃 완료: userId={}", userId);
    }

    public Long getUserIdFromToken(String token) {
        return jwtService.extractUserId(token);
    }

    private void validateEmailDoesNotExist(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "이미 사용 중인 이메일입니다.");
        }
    }

    private UserRole parseUserRole(String role) {
        try {
            return UserRole.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "올바른 역할이 아닙니다. (APPLICANT 또는 COMPANY)");
        }
    }

    private User buildUser(SignUpRequest request, UserRole role) {
        String encodedPassword = passwordEncoder.encode(request.password());
        if (role == UserRole.APPLICANT) {
            return User.createApplicant(
                    request.email(),
                    encodedPassword,
                    request.name()
            );
        } else {
            return User.createCompany(
                    request.email(),
                    encodedPassword,
                    request.name()
            );
        }
    }

    private void validateUserIsActive(User user) {
        if (!user.getActive()) {
            throw new BusinessException(ErrorCode.AUTH_ACCOUNT_DISABLED);
        }
    }

    private AuthResponse createAuthResponse(User user) {
        String accessToken = jwtService.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        String refreshToken = jwtService.generateRefreshToken(
                user.getId(),
                user.getEmail()
        );
        refreshTokenService.saveRefreshToken(user.getId(), refreshToken);

        return new AuthResponse(
                accessToken,
                refreshToken,
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole()
        );
    }
}
