package com.gongsoop.studyspace.repository;

import com.gongsoop.studyspace.entity.StudySession;
import com.gongsoop.studyspace.entity.StudySessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StudySessionRepository extends JpaRepository<StudySession, Long> {

    List<StudySession> findAllByStatusAndPauseDeadlineAtBefore(
            StudySessionStatus status, LocalDateTime threshold);

    List<StudySession> findAllByStatusIn(Collection<StudySessionStatus> statuses);

    Optional<StudySession> findByMember_IdAndStatus(Long memberId, StudySessionStatus status);

    /** 회원의 studyDate 구간(포함) 누적 학습 시간 합계(초). RUNNING 세션의 미반영 진행분은 별도 계산이 필요하다. */
    @Query("SELECT COALESCE(SUM(s.accumulatedSeconds), 0) FROM StudySession s "
            + "WHERE s.member.id = :memberId AND s.studyDate BETWEEN :from AND :to")
    long sumAccumulatedSecondsByMemberAndDateRange(
            @Param("memberId") Long memberId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    /** 보존 기한이 지난, 이미 종료된 세션을 일괄 삭제한다(진행 중 세션은 대상에서 제외). */
    @Modifying
    @Query("DELETE FROM StudySession s WHERE s.studyDate < :cutoff AND s.status IN :statuses")
    int deleteByStudyDateBeforeAndStatusIn(
            @Param("cutoff") LocalDate cutoff,
            @Param("statuses") Collection<StudySessionStatus> statuses);
}
