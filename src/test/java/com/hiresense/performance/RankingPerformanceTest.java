package com.hiresense.performance;

import com.hiresense.interview.domain.InterviewScore;
import com.hiresense.interview.domain.InterviewSession;
import com.hiresense.interview.repository.InterviewScoreRepository;
import com.hiresense.jobPosting.domain.JobPosting;
import com.hiresense.resume.domain.Resume;
import com.hiresense.user.domain.User;
import com.hiresense.user.domain.UserRole;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * 서버리스(DynamoDB) → Spring+RDB 전환 성능 비교 테스트
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
class RankingPerformanceTest {

    @Autowired
    private InterviewScoreRepository interviewScoreRepository;

    @Autowired
    private EntityManager em;

    private static final int JOB_POSTING_COUNT = 10;
    private static final int[] DATA_SIZES = {100, 500, 1000, 3000};

    private final List<Long> jobPostingIds = new ArrayList<>();
    private Long targetJobPostingId;

    /**
     * 테스트 데이터 생성 헬퍼
     * User → JobPosting → Resume → InterviewSession → InterviewScore 연관관계 세팅
     */
    private void insertTestData(int totalScoreCount) {
        // 1. User (기업) 생성
        User company = User.builder()
                .email("company@test.com")
                .password("password")
                .name("테스트기업")
                .role(UserRole.COMPANY)
                .build();
        em.persist(company);

        // 2. JobPosting 생성 (10개)
        List<JobPosting> jobPostings = new ArrayList<>();
        for (int i = 0; i < JOB_POSTING_COUNT; i++) {
            JobPosting jp = JobPosting.builder()
                    .companyName("테스트기업")
                    .jobTitle("개발자 포지션 " + i)
                    .workLocation("서울")
                    .recruitmentPeriod("2025-01-01 ~ 2025-12-31")
                    .qualifications("Java, Spring")
                    .idealCandidate("성실한 개발자")
                    .preferredQualifications("AWS 경험")
                    .jobDescription("백엔드 개발")
                    .user(company)
                    .build();
            em.persist(jp);
            jobPostings.add(jp);
        }
        em.flush();

        jobPostingIds.clear();
        for (JobPosting jp : jobPostings) {
            jobPostingIds.add(jp.getId());
        }
        targetJobPostingId = jobPostingIds.get(0);

        // 3. Resume + InterviewSession + InterviewScore 생성
        Random random = new Random(42); // 재현 가능한 랜덤
        int scoresPerJob = totalScoreCount / JOB_POSTING_COUNT;

        for (int jpIdx = 0; jpIdx < JOB_POSTING_COUNT; jpIdx++) {
            JobPosting jp = jobPostings.get(jpIdx);

            for (int i = 0; i < scoresPerJob; i++) {
                // Resume
                Resume resume = Resume.builder()
                        .name("지원자_" + jpIdx + "_" + i)
                        .email("applicant_" + jpIdx + "_" + i + "@test.com")
                        .build();
                em.persist(resume);

                // InterviewSession
                InterviewSession session = InterviewSession.create(jp, resume, resume.getEmail());
                em.persist(session);

                // InterviewScore
                BigDecimal score = BigDecimal.valueOf(random.nextDouble() * 100)
                        .setScale(2, RoundingMode.HALF_UP);
                InterviewScore interviewScore = InterviewScore.create(
                        session, score,
                        "전반적으로 우수한 면접 결과", "기술적 이해도 높음", "경험 부족",
                        random.nextInt(100), random.nextInt(100),
                        jp.getId(), resume.getEmail(), resume.getName()
                );
                em.persist(interviewScore);
            }

            // 매 JobPosting마다 flush하여 메모리 관리
            if (jpIdx % 2 == 0) {
                em.flush();
                em.clear();
                // clear 후 다시 참조 필요한 엔티티를 다시 로드
                jobPostings = new ArrayList<>();
                for (Long id : jobPostingIds) {
                    jobPostings.add(em.find(JobPosting.class, id));
                }
            }
        }

        em.flush();
        em.clear(); // 1차 캐시 초기화 → 실제 DB 쿼리 성능 측정
    }

    @Test
    @Order(1)
    @DisplayName("📊 랭킹 조회 성능 비교: RDB(WHERE+ORDER BY) vs DynamoDB 시뮬레이션(Full Scan+필터+정렬)")
    void compareRankingQueryPerformance() {
        System.out.println("\n" + "=".repeat(90));
        System.out.println("  랭킹 조회 성능 비교: RDB vs DynamoDB 시뮬레이션");
        System.out.println("  - RDB: SELECT ... WHERE job_posting_id = ? ORDER BY overall_score DESC");
        System.out.println("  - DynamoDB: Scan 전체 → 앱에서 필터링 → 앱에서 정렬");
        System.out.println("=".repeat(90));
        System.out.printf("  %-12s | %-15s | %-15s | %-10s | %-15s%n",
                "데이터 규모", "RDB 쿼리(ms)", "DynamoDB 시뮬(ms)", "성능 배율", "조회 대상 건수");
        System.out.println("-".repeat(90));

        for (int dataSize : DATA_SIZES) {
            // 매 반복마다 DB 초기화 (H2 FK 제약 무시)
            em.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();
            em.createNativeQuery("DELETE FROM interview_score").executeUpdate();
            em.createNativeQuery("DELETE FROM interview_session").executeUpdate();
            em.createNativeQuery("DELETE FROM resume").executeUpdate();
            em.createNativeQuery("DELETE FROM job_posting").executeUpdate();
            em.createNativeQuery("DELETE FROM users").executeUpdate();
            em.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();
            em.flush();
            em.clear();

            insertTestData(dataSize);

            int warmupRuns = 3;
            int measureRuns = 10;

            // 워밍업
            for (int w = 0; w < warmupRuns; w++) {
                interviewScoreRepository.findByJobPostingIdOrderByOverallScoreDesc(targetJobPostingId);
                em.clear();
                interviewScoreRepository.findAll();
                em.clear();
            }

            // ---- RDB 방식: WHERE + ORDER BY ----
            long rdbTotalNs = 0;
            int rdbResultSize = 0;
            for (int r = 0; r < measureRuns; r++) {
                em.clear();
                long start = System.nanoTime();
                List<InterviewScore> result =
                        interviewScoreRepository.findByJobPostingIdOrderByOverallScoreDesc(targetJobPostingId);
                rdbTotalNs += System.nanoTime() - start;
                rdbResultSize = result.size();
            }
            double rdbAvgMs = (rdbTotalNs / (double) measureRuns) / 1_000_000.0;

            // ---- DynamoDB 시뮬레이션: Full Scan → 필터 → 정렬 ----
            long dynamoTotalNs = 0;
            for (int r = 0; r < measureRuns; r++) {
                em.clear();
                long start = System.nanoTime();
                List<InterviewScore> allScores = interviewScoreRepository.findAll(); // Full Scan
                List<InterviewScore> filtered = allScores.stream()
                        .filter(s -> s.getJobPostingId().equals(targetJobPostingId))  // 앱 레벨 필터
                        .sorted(Comparator.comparing(InterviewScore::getOverallScore).reversed()) // 앱 레벨 정렬
                        .collect(Collectors.toList());
                dynamoTotalNs += System.nanoTime() - start;
            }
            double dynamoAvgMs = (dynamoTotalNs / (double) measureRuns) / 1_000_000.0;

            double ratio = dynamoAvgMs / rdbAvgMs;

            System.out.printf("  %-12s | %-15.2f | %-15.2f | %-10.1fx | %-15d%n",
                    dataSize + "건", rdbAvgMs, dynamoAvgMs, ratio, rdbResultSize);
        }

        System.out.println("=".repeat(90));
        System.out.println("  * 10개 채용공고에 데이터 분산, 1개 공고의 랭킹 조회 기준 측정");
        System.out.println("  * DynamoDB 시뮬: 네트워크 지연 미포함(실제 DynamoDB는 추가 지연 발생)");
        System.out.println("=".repeat(90) + "\n");
    }

    @Test
    @Order(2)
    @DisplayName("📊 복합 필터링 성능 비교: RDB(JPQL) vs DynamoDB 시뮬레이션(Full Scan+다중 필터)")
    void compareComplexFilterPerformance() {
        System.out.println("\n" + "=".repeat(90));
        System.out.println("  복합 필터링 성능 비교");
        System.out.println("  - RDB: JPQL WHERE 다중 조건 (jobPostingId + score >= 70 + idealCandidateFit >= 50)");
        System.out.println("  - DynamoDB: Scan 전체 → 앱에서 다중 필터링");
        System.out.println("=".repeat(90));
        System.out.printf("  %-12s | %-15s | %-15s | %-10s%n",
                "데이터 규모", "RDB 쿼리(ms)", "DynamoDB 시뮬(ms)", "성능 배율");
        System.out.println("-".repeat(90));

        for (int dataSize : DATA_SIZES) {
            em.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();
            em.createNativeQuery("DELETE FROM interview_score").executeUpdate();
            em.createNativeQuery("DELETE FROM interview_session").executeUpdate();
            em.createNativeQuery("DELETE FROM resume").executeUpdate();
            em.createNativeQuery("DELETE FROM job_posting").executeUpdate();
            em.createNativeQuery("DELETE FROM users").executeUpdate();
            em.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();
            em.flush();
            em.clear();

            insertTestData(dataSize);

            BigDecimal minScore = BigDecimal.valueOf(70);
            int minFit = 50;

            int warmupRuns = 3;
            int measureRuns = 10;

            // 워밍업
            for (int w = 0; w < warmupRuns; w++) {
                em.createQuery(
                        "SELECT s FROM InterviewScore s WHERE s.jobPostingId = :jobId " +
                                "AND s.overallScore >= :minScore AND s.idealCandidateFit >= :minFit " +
                                "ORDER BY s.overallScore DESC", InterviewScore.class)
                        .setParameter("jobId", targetJobPostingId)
                        .setParameter("minScore", minScore)
                        .setParameter("minFit", minFit)
                        .getResultList();
                em.clear();
                interviewScoreRepository.findAll();
                em.clear();
            }

            // ---- RDB 방식: JPQL 복합 필터 ----
            long rdbTotalNs = 0;
            for (int r = 0; r < measureRuns; r++) {
                em.clear();
                long start = System.nanoTime();
                em.createQuery(
                        "SELECT s FROM InterviewScore s WHERE s.jobPostingId = :jobId " +
                                "AND s.overallScore >= :minScore AND s.idealCandidateFit >= :minFit " +
                                "ORDER BY s.overallScore DESC", InterviewScore.class)
                        .setParameter("jobId", targetJobPostingId)
                        .setParameter("minScore", minScore)
                        .setParameter("minFit", minFit)
                        .getResultList();
                rdbTotalNs += System.nanoTime() - start;
            }
            double rdbAvgMs = (rdbTotalNs / (double) measureRuns) / 1_000_000.0;

            // ---- DynamoDB 시뮬레이션: Full Scan + 다중 필터 ----
            long dynamoTotalNs = 0;
            for (int r = 0; r < measureRuns; r++) {
                em.clear();
                long start = System.nanoTime();
                List<InterviewScore> all = interviewScoreRepository.findAll();
                List<InterviewScore> filtered = all.stream()
                        .filter(s -> s.getJobPostingId().equals(targetJobPostingId))
                        .filter(s -> s.getOverallScore().compareTo(minScore) >= 0)
                        .filter(s -> s.getIdealCandidateFit() >= minFit)
                        .sorted(Comparator.comparing(InterviewScore::getOverallScore).reversed())
                        .collect(Collectors.toList());
                dynamoTotalNs += System.nanoTime() - start;
            }
            double dynamoAvgMs = (dynamoTotalNs / (double) measureRuns) / 1_000_000.0;

            double ratio = dynamoAvgMs / rdbAvgMs;

            System.out.printf("  %-12s | %-15.2f | %-15.2f | %-10.1fx%n",
                    dataSize + "건", rdbAvgMs, dynamoAvgMs, ratio);
        }

        System.out.println("=".repeat(90));
        System.out.println("  * 필터 조건: 점수 >= 70 AND 이상적 후보자 적합도 >= 50");
        System.out.println("=".repeat(90) + "\n");
    }
}
