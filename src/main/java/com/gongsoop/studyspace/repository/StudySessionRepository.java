package com.gongsoop.studyspace.repository;

import com.gongsoop.studyspace.entity.StudySession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudySessionRepository extends JpaRepository<StudySession, Long> {
}
