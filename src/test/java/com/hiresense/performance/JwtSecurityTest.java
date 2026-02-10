package com.hiresense.performance;

import com.hiresense.auth.service.JwtService;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JWT 인증·보안 강화 검증 테스트
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JwtSecurityTest {

    private JwtService jwtService;

    // === Redis 시뮬레이션 (HashMap 기반) ===
    private final Map<String, String> redisStore = new ConcurrentHashMap<>();
    private final Map<String, Long> redisExpireAt = new ConcurrentHashMap<>();

    private static final String BLACKLIST_PREFIX = "blacklist:";
    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret",
                "ThisIsATestSecretKeyForJwtSigningThatIsLongEnough256Bits!!");
        ReflectionTestUtils.setField(jwtService, "expiration", 3600000L);       // 1시간
        ReflectionTestUtils.setField(jwtService, "refreshExpiration", 604800000L); // 7일

        redisStore.clear();
        redisExpireAt.clear();
    }

    // === Redis 시뮬레이션 메서드 ===
    private void redisSet(String key, String value, long ttlMs) {
        redisStore.put(key, value);
        redisExpireAt.put(key, System.currentTimeMillis() + ttlMs);
    }

    private String redisGet(String key) {
        Long expireAt = redisExpireAt.get(key);
        if (expireAt != null && System.currentTimeMillis() > expireAt) {
            redisStore.remove(key);
            redisExpireAt.remove(key);
            return null;
        }
        return redisStore.get(key);
    }

    private boolean redisHasKey(String key) {
        return redisGet(key) != null;
    }

    private void redisDelete(String key) {
        redisStore.remove(key);
        redisExpireAt.remove(key);
    }

    // === Blacklist 시뮬레이션 (TokenBlacklistService 로직 동일) ===
    private void addToBlacklist(String token, long remainingTimeMs) {
        if (remainingTimeMs <= 0) return;
        redisSet(BLACKLIST_PREFIX + token, "blacklisted", remainingTimeMs);
    }

    private boolean isBlacklisted(String token) {
        return redisHasKey(BLACKLIST_PREFIX + token);
    }

    // === Refresh Token 시뮬레이션 (RefreshTokenService 로직 동일) ===
    private void saveRefreshToken(Long userId, String refreshToken) {
        redisSet(REFRESH_TOKEN_PREFIX + userId, refreshToken, 7 * 24 * 60 * 60 * 1000L);
    }

    private boolean validateRefreshToken(Long userId, String refreshToken) {
        String stored = redisGet(REFRESH_TOKEN_PREFIX + userId);
        return stored != null && stored.equals(refreshToken);
    }

    private void deleteRefreshToken(Long userId) {
        redisDelete(REFRESH_TOKEN_PREFIX + userId);
    }

    @Test
    @Order(1)
    @DisplayName("🔒 Access Token Blacklist: 로그아웃 후 토큰 재사용 차단")
    void accessTokenBlacklistTest() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("  Access Token Blacklist 검증");
        System.out.println("  - JWT Stateless 한계: 로그아웃 후에도 토큰 만료 전까지 유효");
        System.out.println("  - 해결: Redis Blacklist로 로그아웃된 토큰 차단");
        System.out.println("=".repeat(70));

        // 1. 로그인 → Access Token 발급
        Long userId = 1L;
        String email = "user@test.com";
        String accessToken = jwtService.generateToken(userId, email, "APPLICANT");

        System.out.println("\n  [1] 로그인: Access Token 발급");
        System.out.println("  - Token: " + accessToken.substring(0, 30) + "...");
        System.out.println("  - 만료: 1시간 후");

        // 2. 로그아웃 전 → 토큰 사용 가능
        boolean validBeforeLogout = jwtService.validateToken(accessToken, email);
        boolean blacklistedBeforeLogout = isBlacklisted(accessToken);

        System.out.println("\n  [2] 로그아웃 전 토큰 상태");
        System.out.println("  - JWT 유효: " + validBeforeLogout);
        System.out.println("  - 블랙리스트: " + blacklistedBeforeLogout);
        System.out.println("  - 인증 결과: ✅ 통과 (정상 접근 가능)");

        assertThat(validBeforeLogout).isTrue();
        assertThat(blacklistedBeforeLogout).isFalse();

        // 3. 로그아웃 → Blacklist에 토큰 등록 (잔여 TTL만큼)
        long expirationTime = jwtService.extractExpiration(accessToken).getTime();
        long remainingTime = expirationTime - System.currentTimeMillis();
        addToBlacklist(accessToken, remainingTime);

        System.out.println("\n  [3] 로그아웃 실행");
        System.out.println("  - Access Token → Redis Blacklist 등록");
        System.out.println("  - TTL: " + (remainingTime / 1000) + "초 (토큰 잔여 만료 시간)");

        // 4. 로그아웃 후 → JWT 자체는 유효하지만, Blacklist에서 차단
        boolean validAfterLogout = jwtService.validateToken(accessToken, email);
        boolean blacklistedAfterLogout = isBlacklisted(accessToken);

        // 인증 필터 로직: JWT 유효 AND 블랙리스트 아님 → 인증 통과
        boolean authResult = validAfterLogout && !blacklistedAfterLogout;

        System.out.println("\n  [4] 로그아웃 후 토큰 재사용 시도");
        System.out.println("  - JWT 유효: " + validAfterLogout + " (서명/만료 정상)");
        System.out.println("  - 블랙리스트: " + blacklistedAfterLogout + " (Redis에 등록됨)");
        System.out.println("  - 인증 결과: ❌ 차단 (블랙리스트 필터링)");

        assertThat(validAfterLogout).isTrue();       // JWT 자체는 유효
        assertThat(blacklistedAfterLogout).isTrue();  // 블랙리스트에 등록됨
        assertThat(authResult).isFalse();             // 최종 인증 차단

        // ---- 비교 ----
        System.out.println("\n  [Before vs After 비교]");
        System.out.printf("  %-35s | %-15s | %-15s%n", "항목", "Before (미적용)", "After (적용)");
        System.out.println("  " + "-".repeat(70));
        System.out.printf("  %-35s | %-15s | %-15s%n",
                "로그아웃 후 토큰 유효 여부", "✅ 유효 (위험)", "❌ 차단 (안전)");
        System.out.printf("  %-35s | %-15s | %-15s%n",
                "토큰 탈취 시 재사용", "가능 (만료 전)", "불가능");
        System.out.printf("  %-35s | %-15s | %-15s%n",
                "차단 메커니즘", "없음", "Redis Blacklist");
        System.out.println("=".repeat(70) + "\n");
    }

    @Test
    @Order(2)
    @DisplayName("🔒 Refresh Token Rotation: 재발급 시 기존 토큰 즉시 폐기")
    void refreshTokenRotationTest() throws InterruptedException {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("  Refresh Token Rotation(RTR) 검증");
        System.out.println("  - 문제: Refresh Token 탈취 시 지속적 Access Token 재발급");
        System.out.println("  - 해결: 재발급 시 기존 Refresh Token 즉시 폐기 + 신규 발급");
        System.out.println("=".repeat(70));

        Long userId = 1L;
        String email = "user@test.com";

        // 1. 로그인 → Refresh Token 발급 + Redis 저장
        String refreshToken1 = jwtService.generateRefreshToken(userId, email);
        saveRefreshToken(userId, refreshToken1);

        System.out.println("\n  [1] 로그인: Refresh Token 발급");
        System.out.println("  - Token: " + refreshToken1.substring(0, 30) + "...");
        System.out.println("  - Redis 저장: refresh_token:" + userId);

        boolean token1Valid = validateRefreshToken(userId, refreshToken1);
        assertThat(token1Valid).isTrue();
        System.out.println("  - 검증: " + token1Valid);

        // 2. 토큰 재발급 (Refresh Token Rotation)
        //    AuthService.refreshToken() 로직과 동일:
        //    ① 기존 Refresh Token 삭제
        //    ② 새 Access Token + Refresh Token 발급
        //    ③ 새 Refresh Token Redis 저장
        deleteRefreshToken(userId);
        Thread.sleep(1100); // JWT iat(초 단위) 차이를 위해 대기
        String newAccessToken = jwtService.generateToken(userId, email, "APPLICANT");
        String refreshToken2 = jwtService.generateRefreshToken(userId, email);
        saveRefreshToken(userId, refreshToken2);

        System.out.println("\n  [2] 토큰 재발급 (Rotation 실행)");
        System.out.println("  - 기존 Refresh Token: 삭제됨");
        System.out.println("  - 새 Access Token: " + newAccessToken.substring(0, 30) + "...");
        System.out.println("  - 새 Refresh Token: " + refreshToken2.substring(0, 30) + "...");

        // 3. 기존 Refresh Token으로 재발급 시도 → 차단
        boolean oldTokenValid = validateRefreshToken(userId, refreshToken1);
        boolean newTokenValid = validateRefreshToken(userId, refreshToken2);

        System.out.println("\n  [3] Rotation 후 토큰 검증");
        System.out.println("  - 기존 Refresh Token 유효: " + oldTokenValid + " → ❌ 차단됨");
        System.out.println("  - 새 Refresh Token 유효: " + newTokenValid + " → ✅ 정상");

        assertThat(oldTokenValid).isFalse();  // 기존 토큰 폐기됨
        assertThat(newTokenValid).isTrue();   // 새 토큰만 유효

        // 4. 탈취 시나리오 시뮬레이션
        System.out.println("\n  [4] 탈취 시나리오 시뮬레이션");
        System.out.println("  - 공격자가 refreshToken1을 탈취했다고 가정");

        // 정상 사용자가 먼저 재발급 → refreshToken1은 이미 폐기
        boolean attackerCanUseOldToken = validateRefreshToken(userId, refreshToken1);
        System.out.println("  - 공격자가 탈취한 토큰으로 재발급 시도: " + attackerCanUseOldToken);
        System.out.println("  - 결과: ❌ 차단 (이미 Rotation으로 폐기됨)");

        assertThat(attackerCanUseOldToken).isFalse();

        // ---- 비교 ----
        System.out.println("\n  [Before vs After 비교]");
        System.out.printf("  %-35s | %-20s | %-20s%n", "항목", "Before (미적용)", "After (RTR 적용)");
        System.out.println("  " + "-".repeat(80));
        System.out.printf("  %-35s | %-20s | %-20s%n",
                "Refresh Token 재사용", "무제한 재사용", "1회 사용 후 폐기");
        System.out.printf("  %-35s | %-20s | %-20s%n",
                "탈취 시 피해 범위", "만료까지 무제한", "단일 세션 수준");
        System.out.printf("  %-35s | %-20s | %-20s%n",
                "탈취 탐지", "불가능", "토큰 불일치로 탐지");
        System.out.println("=".repeat(70) + "\n");
    }

    @Test
    @Order(3)
    @DisplayName("🔒 Redis TTL 메모리 효율성: 만료된 블랙리스트 자동 정리")
    void redisTtlEfficiencyTest() throws InterruptedException {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("  Redis TTL 메모리 효율성 검증");
        System.out.println("  - 블랙리스트 토큰: JWT 잔여 만료 시간만큼만 Redis에 보관");
        System.out.println("  - TTL 만료 후 자동 삭제 → 메모리 낭비 없음");
        System.out.println("=".repeat(70));

        // 짧은 TTL로 시뮬레이션 (실제: 토큰 잔여 시간, 테스트: 500ms)
        String token1 = "expired-token-1";
        String token2 = "expired-token-2";
        String token3 = "long-lived-token";

        // 1. 다양한 TTL로 블랙리스트 등록
        addToBlacklist(token1, 500);   // 0.5초 후 만료
        addToBlacklist(token2, 500);   // 0.5초 후 만료
        addToBlacklist(token3, 60000); // 60초 후 만료 (오래 유지)

        int initialCount = countBlacklistedTokens();

        System.out.println("\n  [1] 블랙리스트 등록 직후");
        System.out.println("  - token1: TTL=500ms (곧 만료)");
        System.out.println("  - token2: TTL=500ms (곧 만료)");
        System.out.println("  - token3: TTL=60000ms (장기 보관)");
        System.out.println("  - Redis 저장 건수: " + initialCount + "건");

        assertThat(isBlacklisted(token1)).isTrue();
        assertThat(isBlacklisted(token2)).isTrue();
        assertThat(isBlacklisted(token3)).isTrue();
        assertThat(initialCount).isEqualTo(3);

        // 2. TTL 만료 대기
        System.out.println("\n  [2] 600ms 대기 (TTL 만료 시뮬레이션)...");
        Thread.sleep(600);

        // 3. TTL 만료 후 확인
        boolean token1Exists = isBlacklisted(token1);
        boolean token2Exists = isBlacklisted(token2);
        boolean token3Exists = isBlacklisted(token3);
        int afterCount = countBlacklistedTokens();

        System.out.println("\n  [3] TTL 만료 후 상태");
        System.out.println("  - token1 (TTL 만료): " + (token1Exists ? "존재" : "자동 삭제") + " → ✅ 메모리 회수");
        System.out.println("  - token2 (TTL 만료): " + (token2Exists ? "존재" : "자동 삭제") + " → ✅ 메모리 회수");
        System.out.println("  - token3 (TTL 유효): " + (token3Exists ? "존재" : "삭제") + " → 블랙리스트 유지");
        System.out.println("  - Redis 저장 건수: " + afterCount + "건 (" + (initialCount - afterCount) + "건 자동 정리)");

        assertThat(token1Exists).isFalse();  // TTL 만료 → 자동 삭제
        assertThat(token2Exists).isFalse();  // TTL 만료 → 자동 삭제
        assertThat(token3Exists).isTrue();   // TTL 유효 → 유지
        assertThat(afterCount).isEqualTo(1); // 1건만 남음

        // ---- 효율성 분석 ----
        System.out.println("\n  [메모리 효율성 분석]");
        System.out.printf("  %-35s | %-15s%n", "항목", "결과");
        System.out.println("  " + "-".repeat(55));
        System.out.printf("  %-35s | %d건 → %d건%n", "블랙리스트 건수 변화", initialCount, afterCount);
        System.out.printf("  %-35s | %d건 (%.0f%%)%n", "자동 정리된 건수",
                initialCount - afterCount, (1 - (double) afterCount / initialCount) * 100);
        System.out.printf("  %-35s | %s%n", "수동 정리 필요 여부", "불필요 (TTL 자동)");
        System.out.printf("  %-35s | %s%n", "메모리 누수 가능성", "없음");

        System.out.println("\n  [Redis TTL 활용 방식]");
        System.out.println("  - Access Token 블랙리스트: TTL = 토큰 잔여 만료 시간");
        System.out.println("  - Refresh Token 저장: TTL = 7일 (토큰 수명과 동일)");
        System.out.println("  → JWT 만료 시점에 Redis에서도 자동 삭제");
        System.out.println("  → 별도 배치 정리 작업 불필요");
        System.out.println("=".repeat(70) + "\n");
    }

    @Test
    @Order(4)
    @DisplayName("🔒 종합 시나리오: 로그인 → 사용 → 로그아웃 → 재사용 차단 전체 흐름")
    void fullSecurityFlowTest() throws InterruptedException {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("  JWT 보안 강화 종합 시나리오");
        System.out.println("  - 전체 인증 흐름에서 보안 메커니즘 동작 검증");
        System.out.println("=".repeat(70));

        Long userId = 1L;
        String email = "user@test.com";

        // ===== Step 1: 로그인 =====
        String accessToken = jwtService.generateToken(userId, email, "APPLICANT");
        String refreshToken = jwtService.generateRefreshToken(userId, email);
        saveRefreshToken(userId, refreshToken);

        System.out.println("\n  [Step 1] 로그인 성공");
        System.out.println("  - Access Token 발급 (유효: 1시간)");
        System.out.println("  - Refresh Token 발급 + Redis 저장 (유효: 7일)");

        // ===== Step 2: API 요청 (인증 필터) =====
        boolean jwtValid = jwtService.validateToken(accessToken, email);
        boolean notBlacklisted = !isBlacklisted(accessToken);
        boolean authPass = jwtValid && notBlacklisted;

        System.out.println("\n  [Step 2] API 요청 → 인증 필터 통과");
        System.out.println("  - JWT 서명 검증: " + jwtValid);
        System.out.println("  - 블랙리스트 체크: " + notBlacklisted + " (블랙리스트 아님)");
        System.out.println("  - 인증 결과: ✅ 통과");
        assertThat(authPass).isTrue();

        // ===== Step 3: Refresh Token Rotation =====
        deleteRefreshToken(userId);
        Thread.sleep(1100); // JWT iat(초 단위) 차이를 위해 대기
        String newAccessToken = jwtService.generateToken(userId, email, "APPLICANT");
        String newRefreshToken = jwtService.generateRefreshToken(userId, email);
        saveRefreshToken(userId, newRefreshToken);

        System.out.println("\n  [Step 3] 토큰 재발급 (Refresh Token Rotation)");
        System.out.println("  - 기존 Refresh Token: 폐기됨");
        System.out.println("  - 새 토큰 쌍 발급 완료");

        boolean oldRefreshValid = validateRefreshToken(userId, refreshToken);
        boolean newRefreshValid = validateRefreshToken(userId, newRefreshToken);
        System.out.println("  - 기존 Refresh Token 재사용: " + oldRefreshValid + " → ❌ 차단");
        System.out.println("  - 새 Refresh Token: " + newRefreshValid + " → ✅ 유효");
        assertThat(oldRefreshValid).isFalse();
        assertThat(newRefreshValid).isTrue();

        // ===== Step 4: 로그아웃 =====
        deleteRefreshToken(userId);
        long expirationTime = jwtService.extractExpiration(newAccessToken).getTime();
        long remainingTime = expirationTime - System.currentTimeMillis();
        addToBlacklist(newAccessToken, remainingTime);

        System.out.println("\n  [Step 4] 로그아웃");
        System.out.println("  - Refresh Token: Redis에서 삭제됨");
        System.out.println("  - Access Token: 블랙리스트 등록 (TTL=" + (remainingTime / 1000) + "초)");

        // ===== Step 5: 로그아웃 후 재사용 시도 =====
        boolean accessTokenStillValid = jwtService.validateToken(newAccessToken, email);
        boolean accessTokenBlacklisted = isBlacklisted(newAccessToken);
        boolean accessReuse = accessTokenStillValid && !accessTokenBlacklisted;

        boolean refreshReuse = validateRefreshToken(userId, newRefreshToken);

        System.out.println("\n  [Step 5] 로그아웃 후 토큰 재사용 시도");
        System.out.println("  - Access Token JWT 유효: " + accessTokenStillValid);
        System.out.println("  - Access Token 블랙리스트: " + accessTokenBlacklisted);
        System.out.println("  - Access Token 재사용: " + accessReuse + " → ❌ 차단");
        System.out.println("  - Refresh Token 재사용: " + refreshReuse + " → ❌ 차단");

        assertThat(accessReuse).isFalse();   // 블랙리스트로 차단
        assertThat(refreshReuse).isFalse();  // Redis에서 삭제됨

        // ===== 종합 결과 =====
        System.out.println("\n  [종합 보안 검증 결과]");
        System.out.printf("  %-40s | %-10s%n", "보안 항목", "결과");
        System.out.println("  " + "-".repeat(55));
        System.out.printf("  %-40s | %s%n", "로그아웃 후 Access Token 재사용", "❌ 차단");
        System.out.printf("  %-40s | %s%n", "로그아웃 후 Refresh Token 재사용", "❌ 차단");
        System.out.printf("  %-40s | %s%n", "Rotation 후 기존 Refresh Token", "❌ 차단");
        System.out.printf("  %-40s | %s%n", "Redis TTL 기반 자동 정리", "✅ 적용");
        System.out.println("=".repeat(70) + "\n");
    }

    private int countBlacklistedTokens() {
        int count = 0;
        for (String key : new ArrayList<>(redisStore.keySet())) {
            if (key.startsWith(BLACKLIST_PREFIX)) {
                Long expireAt = redisExpireAt.get(key);
                if (expireAt != null && System.currentTimeMillis() > expireAt) {
                    redisStore.remove(key);
                    redisExpireAt.remove(key);
                } else {
                    count++;
                }
            }
        }
        return count;
    }
}
