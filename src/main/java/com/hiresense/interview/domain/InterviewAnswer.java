package com.hiresense.interview.domain;

import com.hiresense.global.entity.BaseTimeEntity;
import com.hiresense.question.domain.Question;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterviewAnswer extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private InterviewSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private Question question;

    private String answerText;

    private BigDecimal score;
    private String feedback;

    public void setSession(InterviewSession session) {
        this.session = session;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }

    public void setAnswerText(String answerText) {
        this.answerText = answerText;
    }

    public static InterviewAnswer create(InterviewSession session, Question question, String answerText) {
        InterviewAnswer answer = new InterviewAnswer();
        answer.setSession(session);
        answer.setQuestion(question);
        answer.setAnswerText(answerText);
        return answer;
    }
}
