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
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * N+1 문제 최적화 검증 테스트
 */
@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.jpa.show-sql=false",
        "spring.jpa.properties.hibernate.show_sql=false",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "logging.level.org.hibernate.SQL=OFF",
        "logging.level.org.hibernate.type.descriptor.sql.BasicBinder=OFF",
        "logging.level.org.hibernate.orm.jdbc.bind=OFF",
        "logging.level.org.hibernate.stat=OFF"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NPlus1OptimizationTest {

    @Autowired
    private InterviewSessionRepository interviewSessionRepository;

    @Autowired
    private InterviewScoreRepository interviewScoreRepository;

    @Autowired
    private EntityManager em;

    private Statistics hibernateStats;
    private static final int SESSION_COUNT = 20;
    private String testApplicantEmail;
    private Long jobPostingId;

    @BeforeEach
    void setUp() {
        // Hibernate 통계 활성화
        SessionFactory sessionFactory = em.getEntityManagerFactory().unwrap(SessionFactory.class);
        hibernateStats = sessionFactory.getStatistics();
        hibernateStats.setStatisticsEnabled(true);

        // 테스트 데이터 생성
        User company = User.builder()
                .email("company@test.com")
                .password("password")
                .name("테스트기업")
                .role(UserRole.COMPANY)
                .build();
        em.persist(company);

        JobPosting jobPosting = JobPosting.builder()
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
        em.flush();
        jobPostingId = jobPosting.getId();

        testApplicantEmail = "applicant@test.com";
        Random random = new Random(42);

        // 20개 세션 + 점수 생성 (같은 지원자, 다른 이력서)
        for (int i = 0; i < SESSION_COUNT; i++) {
            Resume resume = Resume.builder()
                    .name("지원자_" + i)
                    .email(i == 0 ? testApplicantEmail : "applicant_" + i + "@test.com")
                    .build();
            em.persist(resume);

            InterviewSession session = InterviewSession.create(jobPosting, resume,
                    i == 0 ? testApplicantEmail : "applicant_" + i + "@test.com");
            em.persist(session);

            BigDecimal score = BigDecimal.valueOf(random.nextDouble() * 100)
                    .setScale(2, RoundingMode.HALF_UP);
            InterviewScore interviewScore = InterviewScore.create(
                    session, score, "코멘트", "강점", "약점",
                    random.nextInt(100), random.nextInt(100),
                    jobPosting.getId(),
                    session.getApplicantEmail(),
                    resume.getName()
            );
            em.persist(interviewScore);
        }

        em.flush();
        em.clear();
    }

    @Test
    @Order(1)
    @DisplayName("📊 N+1 문제: @EntityGraph 미적용 vs 적용 쿼리 수 비교")
    void compareEntityGraphQueryCount() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("  N+1 문제 최적화 테스트");
        System.out.println("  - 20개 면접 세션 조회 시 실행되는 SQL 쿼리 수 비교");
        System.out.println("=".repeat(70));

        // ---- 1. @EntityGraph 미적용: JPQL로 세션만 조회 (N+1 발생) ----
        hibernateStats.clear();
        em.clear();

        List<InterviewSession> sessionsWithoutGraph = em.createQuery(
                "SELECT s FROM InterviewSession s WHERE s.jobPosting.id = :jobId",
                InterviewSession.class
        ).setParameter("jobId", jobPostingId).getResultList();

        // 연관 엔티티 접근 → Lazy Loading 으로 추가 쿼리 발생
        for (InterviewSession session : sessionsWithoutGraph) {
            session.getJobPosting().getCompanyName();  // JobPosting Lazy 로딩
            session.getResume().getName();              // Resume Lazy 로딩
        }

        long withoutGraphQueries = hibernateStats.getPrepareStatementCount();

        System.out.println("\n  [Before] @EntityGraph 미적용 (N+1 발생)");
        System.out.println("  - 조회 대상: " + sessionsWithoutGraph.size() + "개 세션");
        System.out.println("  - 실행된 쿼리 수: " + withoutGraphQueries + "개");
        System.out.println("  - 구조: 1(세션 조회) + " + sessionsWithoutGraph.size()
                + "(JobPosting) + " + sessionsWithoutGraph.size() + "(Resume)");

        // ---- 2. @EntityGraph 적용: Join Fetch로 한 번에 조회 ----
        hibernateStats.clear();
        em.clear();

        List<InterviewSession> sessionsWithGraph =
                interviewSessionRepository.findByJobPostingId(jobPostingId);

        // 연관 엔티티 접근 → 이미 로딩됨, 추가 쿼리 없음
        for (InterviewSession session : sessionsWithGraph) {
            session.getJobPosting().getCompanyName();
            session.getResume().getName();
        }

        long withGraphQueries = hibernateStats.getPrepareStatementCount();

        System.out.println("\n  [After] @EntityGraph 적용 (Join Fetch)");
        System.out.println("  - 조회 대상: " + sessionsWithGraph.size() + "개 세션");
        System.out.println("  - 실행된 쿼리 수: " + withGraphQueries + "개");
        System.out.println("  - 구조: 1(세션+JobPosting+Resume 조인 쿼리)");

        // ---- 비교 결과 ----
        System.out.println("\n  [비교 결과]");
        System.out.printf("  %-30s | %-10s%n", "항목", "쿼리 수");
        System.out.println("  " + "-".repeat(45));
        System.out.printf("  %-30s | %d개%n", "Before (N+1 미최적화)", withoutGraphQueries);
        System.out.printf("  %-30s | %d개%n", "After (@EntityGraph 적용)", withGraphQueries);
        System.out.printf("  %-30s | %.0f%% 감소%n", "쿼리 감소율",
                (1 - (double) withGraphQueries / withoutGraphQueries) * 100);
        System.out.println("=".repeat(70) + "\n");

        assertThat(withGraphQueries).isLessThanOrEqualTo(2);
        assertThat(withoutGraphQueries).isGreaterThan(withGraphQueries);
    }

    @Test
    @Order(2)
    @DisplayName("📊 반정규화 효과: 랭킹 조회 시 조인+N+1 vs 단일 테이블 쿼리 수 비교")
    void compareDenormalizationQueryCount() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("  반정규화 최적화 테스트");
        System.out.println("  - 반정규화 미적용 시: Score → Session → JobPosting/Resume 조인 필요");
        System.out.println("  - 반정규화 적용 시: InterviewScore 단일 테이블로 랭킹 조회");
        System.out.println("=".repeat(70));

        // ---- 1. 정규화 방식: Score 조회 후 연관 엔티티 Lazy Loading (N+1 발생) ----
        hibernateStats.clear();
        em.clear();

        // 반정규화 필드가 없다고 가정: Score만 조회 후, 랭킹에 필요한 정보를 연관 엔티티에서 가져옴
        List<InterviewScore> scoresNormalized = em.createQuery(
                "SELECT sc FROM InterviewScore sc ORDER BY sc.overallScore DESC",
                InterviewScore.class
        ).getResultList();

        // 반정규화 없이 랭킹 정보를 얻으려면 Session → JobPosting, Resume에 접근해야 함
        for (InterviewScore score : scoresNormalized) {
            score.getSession().getApplicantEmail();        // Session Lazy 로딩
            score.getSession().getJobPosting().getJobTitle(); // JobPosting Lazy 로딩
            score.getSession().getResume().getName();       // Resume Lazy 로딩
        }

        long normalizedQueries = hibernateStats.getPrepareStatementCount();

        System.out.println("\n  [Before] 정규화 방식 (Score → Session → JobPosting/Resume)");
        System.out.println("  - 실행된 쿼리 수: " + normalizedQueries + "개");
        System.out.println("  - 구조: 1(Score 조회) + N(Session) + N(JobPosting) + N(Resume)");

        // ---- 2. 반정규화 방식: InterviewScore 단일 테이블 ----
        hibernateStats.clear();
        em.clear();

        List<InterviewScore> scores =
                interviewScoreRepository.findByJobPostingIdOrderByOverallScoreDesc(jobPostingId);

        // jobPostingId, applicantEmail, applicantName이 이미 InterviewScore에 있으므로 추가 조회 불필요
        for (InterviewScore score : scores) {
            score.getJobPostingId();
            score.getApplicantEmail();
            score.getApplicantName();
            score.getOverallScore();
        }

        long denormalizedQueries = hibernateStats.getPrepareStatementCount();

        System.out.println("\n  [After] 반정규화 방식 (InterviewScore 단일 테이블)");
        System.out.println("  - 실행된 쿼리 수: " + denormalizedQueries + "개");
        System.out.println("  - 구조: 1(단일 테이블 WHERE + ORDER BY)");

        // ---- 비교 결과 ----
        System.out.println("\n  [비교 결과]");
        System.out.printf("  %-35s | %-10s%n", "항목", "쿼리 수");
        System.out.println("  " + "-".repeat(50));
        System.out.printf("  %-35s | %d개%n", "Before (정규화, N+1 발생)", normalizedQueries);
        System.out.printf("  %-35s | %d개%n", "After (반정규화, 단일 테이블)", denormalizedQueries);
        System.out.printf("  %-35s | %.0f%% 감소%n", "쿼리 감소율",
                (1 - (double) denormalizedQueries / normalizedQueries) * 100);

        System.out.println("\n  [반정규화된 필드]");
        System.out.println("  - InterviewScore.jobPostingId   ← Session.jobPosting.id 에서 복제");
        System.out.println("  - InterviewScore.applicantEmail ← Session.applicantEmail 에서 복제");
        System.out.println("  - InterviewScore.applicantName  ← Resume.name 에서 복제");
        System.out.println("  → 랭킹 조회 시 JOIN 없이 단일 테이블만으로 처리");
        System.out.println("=".repeat(70) + "\n");

        assertThat(denormalizedQueries).isEqualTo(1);
        assertThat(normalizedQueries).isGreaterThan(denormalizedQueries);
    }

    @Test
    @Order(3)
    @DisplayName("📊 최적화 종합 요약: 쿼리 수 감소 효과")
    void optimizationSummary() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("  N+1 최적화 종합 요약");
        System.out.println("  - 20개 세션 기준 쿼리 수 비교");
        System.out.println("=".repeat(70));

        // ---- 1. 세션 조회: N+1 발생 ----
        hibernateStats.clear();
        em.clear();

        List<InterviewSession> sessionsNPlus1 = em.createQuery(
                "SELECT s FROM InterviewSession s WHERE s.jobPosting.id = :jobId",
                InterviewSession.class
        ).setParameter("jobId", jobPostingId).getResultList();

        for (InterviewSession s : sessionsNPlus1) {
            s.getJobPosting().getCompanyName();
            s.getResume().getName();
        }

        long nPlus1Queries = hibernateStats.getPrepareStatementCount();

        // ---- 2. 세션 조회: @EntityGraph ----
        hibernateStats.clear();
        em.clear();

        List<InterviewSession> sessionsGraph =
                interviewSessionRepository.findByJobPostingId(jobPostingId);

        for (InterviewSession s : sessionsGraph) {
            s.getJobPosting().getCompanyName();
            s.getResume().getName();
        }

        long graphQueries = hibernateStats.getPrepareStatementCount();

        // ---- 3. 랭킹 조회: 반정규화 ----
        hibernateStats.clear();
        em.clear();

        List<InterviewScore> scores =
                interviewScoreRepository.findByJobPostingIdOrderByOverallScoreDesc(jobPostingId);

        for (InterviewScore score : scores) {
            score.getJobPostingId();
            score.getApplicantEmail();
            score.getApplicantName();
        }

        long denormalizedQueries = hibernateStats.getPrepareStatementCount();

        // ---- 종합 결과 ----
        System.out.printf("\n  %-40s | %-10s | %-15s%n", "시나리오", "쿼리 수", "최적화 방법");
        System.out.println("  " + "-".repeat(70));
        System.out.printf("  %-40s | %d개%s | %s%n",
                "세션 조회 (N+1 미최적화)", nPlus1Queries,
                nPlus1Queries >= 10 ? "  " : " ", "없음 (Lazy Loading)");
        System.out.printf("  %-40s | %d개%s | %s%n",
                "세션 조회 (@EntityGraph 적용)", graphQueries,
                graphQueries >= 10 ? "  " : "   ", "@EntityGraph");
        System.out.printf("  %-40s | %d개%s | %s%n",
                "랭킹 조회 (반정규화 단일 테이블)", denormalizedQueries,
                denormalizedQueries >= 10 ? "  " : "   ", "반정규화");
        System.out.println("  " + "-".repeat(70));
        System.out.printf("  %-40s | %.0f%% 감소%n", "세션 조회 쿼리 감소율",
                (1 - (double) graphQueries / nPlus1Queries) * 100);
        System.out.printf("  %-40s | %.0f%% 감소%n", "랭킹 조회 쿼리 감소율",
                (1 - (double) denormalizedQueries / nPlus1Queries) * 100);

        System.out.println("\n  [적용 기술]");
        System.out.println("  - @EntityGraph: Lazy → Eager Join Fetch (1+N → 1 쿼리)");
        System.out.println("  - 반정규화: 랭킹 조회 시 JOIN 제거 (단일 테이블 조회)");
        System.out.println("  - 데이터 규모 증가 시 쿼리 수 차이가 선형적으로 확대됨");
        System.out.println("=".repeat(70) + "\n");

        // 검증
        assertThat(graphQueries).isLessThanOrEqualTo(2);
        assertThat(denormalizedQueries).isEqualTo(1);
        assertThat(nPlus1Queries).isGreaterThan(graphQueries);
        assertThat(nPlus1Queries).isGreaterThan(denormalizedQueries);
    }
}
