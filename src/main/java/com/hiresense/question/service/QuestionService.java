package com.hiresense.question.service;

import com.hiresense.question.domain.Question;
import com.hiresense.question.domain.QuestionType;
import com.hiresense.question.repository.QuestionRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;

    @Transactional
    @PostConstruct
    public void initCommonQuestions() {
        if (questionRepository.count() == 0) {
            List<Question> commonQuestions = Arrays.asList(
                    Question.builder().text("자기소개 부탁드립니다.").type(QuestionType.COMMON).build(),
                    Question.builder().text("우리 회사에 지원한 동기가 무엇인가요?").type(QuestionType.COMMON).build(),
                    Question.builder().text("자신의 장점과 단점에 대해 말해주세요.").type(QuestionType.COMMON).build(),
                    Question.builder().text("자신의 직무 경험에 대해 말해주세요.").type(QuestionType.COMMON).build(),
                    Question.builder().text("갈등 상황을 해결한 경험이 있나요?").type(QuestionType.COMMON).build(),
                    Question.builder().text("우리 회사에서 어떤 일을 하고 싶나요?").type(QuestionType.COMMON).build()
            );
            questionRepository.saveAll(commonQuestions);
        }
    }
}
