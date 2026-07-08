package com.gongsoop.studyspace.repository;

import com.gongsoop.studyspace.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findAllByStudyRoom_IdOrderBySeatNoAsc(Long studyRoomId);
}
