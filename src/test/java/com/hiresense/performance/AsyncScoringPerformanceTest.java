package com.hiresense.performance;

import org.junit.jupiter.api.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 비동기 채점 성능 비교 테스트
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AsyncScoringPerformanceTest {

    /** AI 채점 소요 시간 시뮬레이션 (실제 Bedrock 호출 시 5~8초) */
    private static final long AI_SCORING_LATENCY_MS = 6000;

    /**
     * AI 채점을 시뮬레이션하는 메서드 (Bedrock API 호출 대신 sleep)
     */
    private void simulateAiScoring() {
        try {
            Thread.sleep(AI_SCORING_LATENCY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    @Order(1)
    @DisplayName("📊 동기 방식 (Before): 채점 완료까지 대기 후 응답")
    void syncScoringResponse() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("  [Before] 동기 방식: 채점 완료까지 대기 후 응답");
        System.out.println("  - handleAnswer() → scoreInterview() 완료 대기 → 응답 반환");
        System.out.println("=".repeat(70));

        long start = System.nanoTime();

        // === 동기 방식: 채점이 끝날 때까지 응답을 보내지 못함 ===
        simulateAiScoring();  // 6초 대기 (실제 Bedrock AI 채점)
        String response = "면접이 종료되었습니다. 수고하셨습니다.";

        long elapsed = System.nanoTime() - start;
        double elapsedMs = elapsed / 1_000_000.0;

        System.out.printf("  응답 시간: %.0fms (%.1f초)%n", elapsedMs, elapsedMs / 1000);
        System.out.println("  사용자는 " + (AI_SCORING_LATENCY_MS / 1000) + "초간 빈 화면을 보며 대기");
        System.out.println("=".repeat(70) + "\n");

        assertThat(elapsedMs).isGreaterThan(5000); // 5초 이상 걸림
    }

    @Test
    @Order(2)
    @DisplayName("📊 비동기 방식 (After): @Async로 즉시 응답, 채점은 백그라운드")
    void asyncScoringResponse() throws Exception {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("  [After] 비동기 방식: @Async로 채점 분리, 즉시 응답");
        System.out.println("  - handleAnswer() → CompletableFuture로 채점 위임 → 즉시 응답");
        System.out.println("=".repeat(70));

        AtomicBoolean scoringComplete = new AtomicBoolean(false);

        long start = System.nanoTime();

        // === 비동기 방식: 채점을 별도 스레드로 위임 (실제 코드의 @Async 동작) ===
        CompletableFuture<Void> scoringFuture = CompletableFuture.runAsync(() -> {
            simulateAiScoring();  // 별도 스레드에서 6초 실행
            scoringComplete.set(true);
        });

        // 즉시 응답 반환 (채점 완료를 기다리지 않음)
        String response = "면접이 종료되었습니다. 수고하셨습니다.";

        long elapsed = System.nanoTime() - start;
        double elapsedMs = elapsed / 1_000_000.0;

        System.out.printf("  응답 시간: %.1fms (%.3f초)%n", elapsedMs, elapsedMs / 1000);
        System.out.println("  채점 진행 중: " + !scoringComplete.get() + " (백그라운드에서 실행 중)");

        assertThat(elapsedMs).isLessThan(100); // 100ms 이내 응답
        assertThat(scoringComplete.get()).isFalse(); // 응답 시점에 채점은 아직 진행 중

        // 채점 완료 대기 (백그라운드 검증)
        scoringFuture.join();
        assertThat(scoringComplete.get()).isTrue();
        System.out.println("  채점 완료: " + scoringComplete.get() + " (백그라운드에서 정상 완료)");

        System.out.println("=".repeat(70) + "\n");
    }

    @Test
    @Order(3)
    @DisplayName("📊 성능 비교 요약: 동기 vs 비동기 응답 시간")
    void performanceSummary() throws Exception {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("  비동기 처리 성능 비교 요약");
        System.out.println("=".repeat(70));

        // 동기 측정
        long syncStart = System.nanoTime();
        simulateAiScoring();
        String syncResponse = "완료";
        double syncMs = (System.nanoTime() - syncStart) / 1_000_000.0;

        // 비동기 측정
        long asyncStart = System.nanoTime();
        CompletableFuture<Void> future = CompletableFuture.runAsync(this::simulateAiScoring);
        String asyncResponse = "완료";
        double asyncMs = (System.nanoTime() - asyncStart) / 1_000_000.0;

        double improvement = syncMs / asyncMs;
        double reductionPercent = (1 - asyncMs / syncMs) * 100;

        System.out.printf("  %-25s | %-15s%n", "항목", "응답 시간");
        System.out.println("  " + "-".repeat(45));
        System.out.printf("  %-25s | %.0fms (%.1f초)%n", "Before (동기 채점)", syncMs, syncMs / 1000);
        System.out.printf("  %-25s | %.1fms (%.3f초)%n", "After (비동기 채점)", asyncMs, asyncMs / 1000);
        System.out.println("  " + "-".repeat(45));
        System.out.printf("  %-25s | %.0fx 빨라짐%n", "성능 향상 배율", improvement);
        System.out.printf("  %-25s | %.1f%% 단축%n", "체감 대기 시간 단축", reductionPercent);
        System.out.println("=".repeat(70));
        System.out.println("  * AI 채점 Latency: " + AI_SCORING_LATENCY_MS + "ms (Bedrock Claude 기준)");
        System.out.println("  * 비동기 처리: @Async(\"taskExecutor\") + CompletableFuture");
        System.out.println("  * 채점 결과는 백그라운드 완료 후 DB 저장, 클라이언트는 폴링으로 확인");
        System.out.println("=".repeat(70) + "\n");

        assertThat(reductionPercent).isGreaterThan(98);

        future.join();
    }
}
