package com.gongsoop.studyspace.repository;

import com.gongsoop.studyspace.entity.SeatOccupancy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeatOccupancyRepository extends JpaRepository<SeatOccupancy, Long> {
    List<SeatOccupancy> findAllByStudyChannel_Id(Long studyChannelId);
}
