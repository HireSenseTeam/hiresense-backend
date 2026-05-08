package com.hiresense.global.config;

import com.hiresense.question.domain.Question;
import com.hiresense.question.domain.QuestionType;
import com.hiresense.question.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final QuestionRepository questionRepository;

    @Override
    @Transactional
    public void run(String... args) {
        long questionCount = questionRepository.count();
        if (questionCount == 0) {
            log.info("공통 질문 초기화를 시작합니다.");
            List<Question> commonQuestions = List.of(
                    commonQuestion("오늘 AI 면접관과 대화한다고 생각하니 어떤 기분이 드시나요?"),
                    commonQuestion("자기소개를 해주세요."),
                    commonQuestion("본인의 강점과 약점을 말씀해주세요."),
                    commonQuestion("어려운 문제나 갈등 상황을 겪었을 때, 어떻게 해결했는지 구체적으로 말해주세요."),
                    commonQuestion("팀으로 일할 때 가장 중요하다고 생각하는 점은 무엇인가요?"),
                    commonQuestion("5년 뒤 본인의 목표 또는 커리어 비전을 말씀해주세요.")
            );
            questionRepository.saveAll(commonQuestions);
            log.info("공통 질문 초기화 완료: {}개 질문 생성", commonQuestions.size());
        } else {
            log.info("공통 질문이 이미 존재합니다. 초기화를 건너뜁니다. (현재 질문 수: {})", questionCount);
        }
    }

    private Question commonQuestion(String text) {
        return Question.builder()
                .text(text)
                .type(QuestionType.COMMON)
                .build();
    }
}
