package com.gongsoop.studyspace.repository;

import com.gongsoop.studyspace.entity.StudySession;
import com.gongsoop.studyspace.entity.StudySessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface StudySessionRepository extends JpaRepository<StudySession, Long> {

    List<StudySession> findAllByStatusAndPauseDeadlineAtBefore(
            StudySessionStatus status, LocalDateTime threshold);

    List<StudySession> findAllByStatusIn(Collection<StudySessionStatus> statuses);
}
