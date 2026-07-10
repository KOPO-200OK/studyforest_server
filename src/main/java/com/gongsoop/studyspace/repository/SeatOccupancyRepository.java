package com.gongsoop.studyspace.repository;

import com.gongsoop.studyspace.entity.SeatOccupancy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SeatOccupancyRepository extends JpaRepository<SeatOccupancy, Long> {
    List<SeatOccupancy> findAllByStudyChannel_Id(Long studyChannelId);

    boolean existsByMember_Id(Long memberId);

    Optional<SeatOccupancy> findByMember_Id(Long memberId);

    boolean existsByStudyChannel_IdAndSeat_Id(Long studyChannelId, Long seatId);

    Optional<SeatOccupancy> findByStudySession_IdAndMember_Id(Long studySessionId, Long memberId);

    Optional<SeatOccupancy> findByStudySession_Id(Long studySessionId);


    /** 재접속 제한이 지난 점유의 세션 ID(스케줄러 자동 퇴실 후보). */
    @Query("select o.studySession.id from SeatOccupancy o where o.reconnectDeadlineAt < :threshold")
    List<Long> findSessionIdsWithReconnectDeadlineBefore(@Param("threshold") LocalDateTime threshold);

    /** heartbeat가 오래 끊겼는데 아직 끊김 판정 전인 점유의 세션 ID(WS 이벤트 유실 대비). */
    @Query("select o.studySession.id from SeatOccupancy o "
            + "where o.lastSeenAt < :threshold and o.disconnectedAt is null")
    List<Long> findStaleSessionIds(@Param("threshold") LocalDateTime threshold);

    /** 현재 점유 중인 모든 세션 ID(재기동 정리에서 활성 세션과 대조). */
    @Query("select o.studySession.id from SeatOccupancy o")
    List<Long> findAllSessionIds();
}
