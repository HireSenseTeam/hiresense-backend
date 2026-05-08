package com.hiresense.interview.repository;

import com.hiresense.interview.domain.InterviewQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, Long> {
    List<InterviewQuestion> findByInterviewSessionIdOrderByQuestionOrder(String sessionId);
    
    @Query("SELECT COUNT(iq) FROM InterviewQuestion iq WHERE iq.interviewSession.id = :sessionId")
    int countByInterviewSessionId(@Param("sessionId") String sessionId);

    @Query("""
            SELECT iq.interviewSession.id, COUNT(iq)
            FROM InterviewQuestion iq
            WHERE iq.interviewSession.id IN :sessionIds
            GROUP BY iq.interviewSession.id
            """)
    List<Object[]> countByInterviewSessionIds(@Param("sessionIds") List<String> sessionIds);
}
