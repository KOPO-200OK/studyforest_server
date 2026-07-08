package com.gongsoop.studyspace.repository;

import com.gongsoop.studyspace.entity.StudyRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudyRoomRepository extends JpaRepository<StudyRoom, Long> {
    List<StudyRoom> findAllByActiveTrueOrderByMapNoAsc();
}
