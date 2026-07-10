package com.gongsoop.studyspace.repository;

import com.gongsoop.studyspace.entity.StudyZone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudyZoneRepository extends JpaRepository<StudyZone, Long> {

    List<StudyZone> findAllByStudyRoom_IdAndActiveTrueOrderByIdAsc(Long studyRoomId);
}