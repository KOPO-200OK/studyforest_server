package com.gongsoop.studyspace.repository;

import com.gongsoop.studyspace.entity.StudyChannel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudyChannelRepository extends JpaRepository<StudyChannel, Long> {
    List<StudyChannel> findAllByStudyRoom_IdAndActiveTrueOrderByChannelNoAsc(Long studyRoomId);
}
