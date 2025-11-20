package com.hiresense.interview.domain;

import com.hiresense.question.domain.Question;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InterviewQuestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private InterviewSession interviewSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private Question question;

    private int questionOrder;

    public void setInterviewSession(InterviewSession interviewSession) {
        this.interviewSession = interviewSession;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }

    public void setQuestionOrder(int questionOrder) {
        this.questionOrder = questionOrder;
    }

    public static InterviewQuestion create(InterviewSession session, Question question, int order) {
        InterviewQuestion interviewQuestion = new InterviewQuestion();
        interviewQuestion.setInterviewSession(session);
        interviewQuestion.setQuestion(question);
        interviewQuestion.setQuestionOrder(order);
        return interviewQuestion;
    }
}
