package com.gongsoop.studyspace.repository;

import com.gongsoop.studyspace.entity.SeatOccupancy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SeatOccupancyRepository extends JpaRepository<SeatOccupancy, Long> {
    List<SeatOccupancy> findAllByStudyChannel_Id(Long studyChannelId);

    boolean existsByMember_Id(Long memberId);

    boolean existsByStudyChannel_IdAndSeat_Id(Long studyChannelId, Long seatId);

    Optional<SeatOccupancy> findByStudySession_IdAndMember_Id(Long studySessionId, Long memberId);
}
