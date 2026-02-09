package com.hiresense.performance;

import com.hiresense.interview.domain.InterviewScore;
import com.hiresense.interview.domain.InterviewSession;
import com.hiresense.interview.repository.InterviewScoreRepository;
import com.hiresense.interview.repository.InterviewSessionRepository;
import com.hiresense.jobPosting.domain.JobPosting;
import com.hiresense.resume.domain.Resume;
import com.hiresense.user.domain.User;
import com.hiresense.user.domain.UserRole;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 데이터 정합성 검증 테스트
 */
@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.jpa.show-sql=false",
        "spring.jpa.properties.hibernate.show_sql=false",
        "logging.level.org.hibernate.SQL=OFF",
        "logging.level.org.hibernate.type.descriptor.sql.BasicBinder=OFF",
        "logging.level.org.hibernate.orm.jdbc.bind=OFF"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TransactionConsistencyTest {

    @Autowired
    private InterviewScoreRepository interviewScoreRepository;

    @Autowired
    private InterviewSessionRepository interviewSessionRepository;

    @Autowired
    private EntityManager em;

    private JobPosting jobPosting;
    private final List<InterviewSession> sessions = new ArrayList<>();

    @BeforeEach
    void setUp() {
        User company = User.builder()
                .email("company@test.com")
                .password("password")
                .name("테스트기업")
                .role(UserRole.COMPANY)
                .build();
        em.persist(company);

        jobPosting = JobPosting.builder()
                .companyName("테스트기업")
                .jobTitle("백엔드 개발자")
                .workLocation("서울")
                .recruitmentPeriod("2025-01-01 ~ 2025-12-31")
                .qualifications("Java, Spring")
                .idealCandidate("성실한 개발자")
                .preferredQualifications("AWS 경험")
                .jobDescription("백엔드 개발")
                .user(company)
                .build();
        em.persist(jobPosting);

        // 50명의 지원자 데이터 생성
        for (int i = 0; i < 50; i++) {
            Resume resume = Resume.builder()
                    .name("지원자_" + i)
                    .email("applicant_" + i + "@test.com")
                    .build();
            em.persist(resume);

            InterviewSession session = InterviewSession.create(jobPosting, resume, resume.getEmail());
            em.persist(session);
            sessions.add(session);
        }

        em.flush();
    }

    @Test
    @Order(1)
    @DisplayName("✅ 대량 채점 데이터 정합성: 50건 저장 → 유실/중복 0건")
    void bulkScoringConsistency() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("  대량 채점 데이터 정합성 테스트");
        System.out.println("  - 50명의 지원자에 대해 채점 결과 일괄 저장");
        System.out.println("  - DynamoDB: 트랜잭션 없이 쓰기 → 데이터 유실/덮어쓰기 위험");
        System.out.println("  - RDB+Spring: @Transactional로 원자적 처리 보장");
        System.out.println("=".repeat(70));

        Random random = new Random(42);
        int totalCount = 50;

        // 50건 채점 결과 저장
        for (int i = 0; i < totalCount; i++) {
            InterviewSession session = sessions.get(i);
            BigDecimal score = BigDecimal.valueOf(random.nextDouble() * 100)
                    .setScale(2, RoundingMode.HALF_UP);

            InterviewScore interviewScore = InterviewScore.create(
                    session, score,
                    "면접 코멘트 " + i,
                    "강점 " + i,
                    "약점 " + i,
                    random.nextInt(100),
                    random.nextInt(100),
                    jobPosting.getId(),
                    "applicant_" + i + "@test.com",
                    "지원자_" + i
            );
            interviewScoreRepository.save(interviewScore);
        }

        em.flush();
        em.clear();

        // 검증
        List<InterviewScore> allScores = interviewScoreRepository.findAll();
        List<InterviewScore> jobScores =
                interviewScoreRepository.findByJobPostingIdOrderByOverallScoreDesc(jobPosting.getId());

        System.out.println("\n  [결과]");
        System.out.println("  - 저장 시도: " + totalCount + "건");
        System.out.println("  - DB 저장 건수: " + allScores.size() + "건");
        System.out.println("  - 해당 공고 점수: " + jobScores.size() + "건");

        // 유실 검증
        assertThat(allScores).hasSize(totalCount);
        System.out.println("  - ✅ 데이터 유실: 0건 (50건 모두 정상 저장)");

        // 중복 검증
        Set<String> emails = new HashSet<>();
        for (InterviewScore s : allScores) {
            emails.add(s.getApplicantEmail());
        }
        assertThat(emails).hasSize(totalCount);
        System.out.println("  - ✅ 데이터 중복: 0건");
        System.out.println("=".repeat(70) + "\n");
    }

    @Test
    @Order(2)
    @DisplayName("✅ Unique 제약: 동일 세션 중복 채점 차단 (DynamoDB에서는 덮어쓰기 발생)")
    void uniqueConstraintPreventsDoubleScoring() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("  Unique 제약 검증 테스트");
        System.out.println("  - DynamoDB: 같은 키로 PUT → 기존 데이터 덮어쓰기 (Silent overwrite)");
        System.out.println("  - RDB: Unique 제약으로 중복 삽입 차단 → 예외 발생");
        System.out.println("=".repeat(70));

        InterviewSession session = sessions.get(0);

        // 첫 번째 채점 저장 (성공)
        InterviewScore firstScore = InterviewScore.create(
                session, new BigDecimal("85.00"),
                "첫 번째 채점", "강점1", "약점1", 80, 80,
                jobPosting.getId(), "applicant_0@test.com", "지원자_0"
        );
        interviewScoreRepository.save(firstScore);
        em.flush();

        System.out.println("  - 1차 채점 저장: 성공 (85.00점)");

        // 같은 세션에 두 번째 채점 시도 → Unique 제약 위반
        InterviewScore duplicateScore = InterviewScore.create(
                session, new BigDecimal("90.00"),
                "두 번째 채점 (덮어쓰기 시도)", "강점2", "약점2", 90, 90,
                jobPosting.getId(), "applicant_0@test.com", "지원자_0"
        );

        assertThatThrownBy(() -> {
            interviewScoreRepository.save(duplicateScore);
            em.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);

        System.out.println("  - 2차 채점 시도: ❌ 차단됨 (DataIntegrityViolationException)");
        System.out.println("  - ✅ DynamoDB와 달리 기존 데이터(85.00점)가 보호됨");
        System.out.println("  - ✅ Silent overwrite 원천 방지");
        System.out.println("=".repeat(70) + "\n");
    }

    @Test
    @Order(3)
    @DisplayName("✅ 랭킹 정렬 정확성: 동점자 처리 및 순위 일관성 검증")
    void rankingAccuracyTest() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("  랭킹 정렬 정확성 테스트");
        System.out.println("=".repeat(70));

        // 의도적으로 동점자 포함 데이터 생성
        BigDecimal[] scores = {
                new BigDecimal("95.50"), new BigDecimal("95.50"), // 공동 1위
                new BigDecimal("88.00"),                          // 3위
                new BigDecimal("88.00"), new BigDecimal("88.00"), // 공동 3위
                new BigDecimal("75.30"),                          // 6위
                new BigDecimal("60.00"),                          // 7위
                new BigDecimal("60.00"),                          // 공동 7위
                new BigDecimal("45.00"),                          // 9위
                new BigDecimal("30.00")                           // 10위
        };

        for (int i = 0; i < scores.length; i++) {
            InterviewSession session = sessions.get(i);
            InterviewScore score = InterviewScore.create(
                    session, scores[i],
                    "코멘트", "강점", "약점", 80, 80,
                    jobPosting.getId(),
                    "applicant_" + i + "@test.com",
                    "지원자_" + i
            );
            interviewScoreRepository.save(score);
        }

        em.flush();
        em.clear();

        // 랭킹 조회
        List<InterviewScore> ranked =
                interviewScoreRepository.findByJobPostingIdOrderByOverallScoreDesc(jobPosting.getId());

        // 검증: 내림차순 정렬 확인
        for (int i = 1; i < ranked.size(); i++) {
            assertThat(ranked.get(i).getOverallScore())
                    .isLessThanOrEqualTo(ranked.get(i - 1).getOverallScore());
        }

        System.out.println("  [랭킹 결과]");
        System.out.printf("  %-6s | %-15s | %-10s%n", "순위", "지원자", "점수");
        System.out.println("  " + "-".repeat(40));
        int rank = 1;
        BigDecimal prevScore = null;
        for (int i = 0; i < ranked.size(); i++) {
            InterviewScore s = ranked.get(i);
            if (prevScore != null && s.getOverallScore().compareTo(prevScore) != 0) {
                rank = i + 1;
            }
            System.out.printf("  %-6d | %-15s | %-10s%n",
                    rank, s.getApplicantName(), s.getOverallScore());
            prevScore = s.getOverallScore();
        }

        // 동점자가 같은 순위인지 검증
        assertThat(ranked.get(0).getOverallScore())
                .isEqualTo(ranked.get(1).getOverallScore());
        System.out.println("\n  ✅ 동점자 동일 순위 처리 정상");

        // 총 건수 검증
        assertThat(ranked).hasSize(scores.length);
        System.out.println("  ✅ 데이터 불일치: 0건");
        System.out.println("  ✅ 정렬 정확성 검증 통과");
        System.out.println("=".repeat(70) + "\n");
    }

    @Test
    @Order(4)
    @DisplayName("✅ 연관관계 무결성: Session-Score 1:1 관계 + FK 보장")
    void relationshipIntegrityTest() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("  연관관계 무결성 테스트");
        System.out.println("  - DynamoDB: 외래 키 없이 문자열 참조 → 고아 데이터 발생 가능");
        System.out.println("  - RDB: FK + Unique 제약으로 무결성 보장");
        System.out.println("=".repeat(70));

        // 10명에 대해 점수 생성
        for (int i = 0; i < 10; i++) {
            InterviewSession session = sessions.get(i);
            InterviewScore score = InterviewScore.create(
                    session, BigDecimal.valueOf(80 + i),
                    "코멘트", "강점", "약점", 80, 80,
                    jobPosting.getId(),
                    "applicant_" + i + "@test.com",
                    "지원자_" + i
            );
            interviewScoreRepository.save(score);
        }
        em.flush();
        em.clear();

        // 1:1 관계 검증 - 각 Session에 Score가 정확히 1개
        List<InterviewScore> allScores = interviewScoreRepository.findAll();
        Set<String> sessionIds = new HashSet<>();
        for (InterviewScore score : allScores) {
            String sessionId = score.getSession().getId();
            assertThat(sessionIds.add(sessionId))
                    .as("Session ID '%s'에 중복 Score가 존재합니다", sessionId)
                    .isTrue();
        }

        System.out.println("  ✅ Session:Score = 1:1 관계 검증 통과 (" + allScores.size() + "건)");

        // 고아 데이터 검증 - 모든 Score의 Session이 존재
        for (InterviewScore score : allScores) {
            assertThat(score.getSession()).isNotNull();
            assertThat(score.getSession().getJobPosting()).isNotNull();
        }
        System.out.println("  ✅ 고아 데이터(orphan): 0건");
        System.out.println("  ✅ FK 참조 무결성 검증 통과");
        System.out.println("=".repeat(70) + "\n");
    }
}
