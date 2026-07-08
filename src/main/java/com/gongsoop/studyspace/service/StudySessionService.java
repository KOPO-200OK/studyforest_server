package com.gongsoop.studyspace.service;

import com.gongsoop.global.exception.BusinessException;
import com.gongsoop.member.entity.Member;
import com.gongsoop.member.repository.MemberRepository;
import com.gongsoop.studyspace.dto.request.OccupySeatRequest;
import com.gongsoop.studyspace.dto.response.StudySessionResponse;
import com.gongsoop.studyspace.entity.*;
import com.gongsoop.studyspace.repository.SeatOccupancyRepository;
import com.gongsoop.studyspace.repository.SeatRepository;
import com.gongsoop.studyspace.repository.StudyChannelRepository;
import com.gongsoop.studyspace.repository.StudySessionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class StudySessionService {

    private static final String DEFAULT_SUBJECT = "한국사";

    private final MemberRepository memberRepository;
    private final StudyChannelRepository studyChannelRepository;
    private final SeatRepository seatRepository;
    private final StudySessionRepository studySessionRepository;
    private final SeatOccupancyRepository seatOccupancyRepository;
    private final Clock clock;
    
    @Autowired
    public StudySessionService(
            MemberRepository memberRepository,
            StudyChannelRepository studyChannelRepository,
            SeatRepository seatRepository,
            StudySessionRepository studySessionRepository,
            SeatOccupancyRepository seatOccupancyRepository
    ) {
        this(memberRepository, studyChannelRepository, seatRepository,
                studySessionRepository, seatOccupancyRepository, Clock.systemDefaultZone());
    }

    StudySessionService(
            MemberRepository memberRepository,
            StudyChannelRepository studyChannelRepository,
            SeatRepository seatRepository,
            StudySessionRepository studySessionRepository,
            SeatOccupancyRepository seatOccupancyRepository,
            Clock clock
    ) {
        this.memberRepository = memberRepository;
        this.studyChannelRepository = studyChannelRepository;
        this.seatRepository = seatRepository;
        this.studySessionRepository = studySessionRepository;
        this.seatOccupancyRepository = seatOccupancyRepository;
        this.clock = clock;
    }

    @Transactional
    public StudySessionResponse occupySeat(
            Long studyChannelId,
            Long seatId,
            OccupySeatRequest request,
            String email
    ) {
        Member member = getCurrentMember(email);
        StudyChannel channel = getActiveChannel(studyChannelId);
        Seat seat = seatRepository.findByIdForUpdate(seatId)
                .filter(Seat::isActive)
                .orElseThrow(() -> new BusinessException(
                        "SEAT_NOT_FOUND", "이용 가능한 좌석을 찾을 수 없습니다", HttpStatus.NOT_FOUND));

        if (!channel.getStudyRoom().getId().equals(seat.getStudyRoom().getId())) {
            throw new BusinessException(
                    "SEAT_NOT_IN_CHANNEL_ROOM", "선택한 채널에 속하지 않은 좌석입니다", HttpStatus.BAD_REQUEST);
        }
        if (seatOccupancyRepository.existsByMember_Id(member.getId())) {
            throw new BusinessException(
                    "MEMBER_ALREADY_SEATED", "이미 이용 중인 좌석이 있습니다", HttpStatus.CONFLICT);
        }
        if (seatOccupancyRepository.existsByStudyChannel_IdAndSeat_Id(studyChannelId, seatId)) {
            throw new BusinessException(
                    "SEAT_ALREADY_OCCUPIED", "이미 사용 중인 좌석입니다", HttpStatus.CONFLICT);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        String subject = normalizeSubject(request == null ? null : request.subject());
        StudySession session = studySessionRepository.save(
                StudySession.start(member, channel, seat, subject, now));
        seatOccupancyRepository.save(SeatOccupancy.occupy(channel, seat, member, session, now));

        return StudySessionResponse.from(session);
    }

    @Transactional
    public StudySessionResponse leaveSeat(Long studySessionId, String email) {
        Member member = getCurrentMember(email);
        SeatOccupancy occupancy = seatOccupancyRepository
                .findByStudySession_IdAndMember_Id(studySessionId, member.getId())
                .orElseThrow(() -> new BusinessException(
                        "ACTIVE_OCCUPANCY_NOT_FOUND", "현재 이용 중인 좌석을 찾을 수 없습니다", HttpStatus.NOT_FOUND));

        StudySession session = occupancy.getStudySession();
        session.completeByUser(LocalDateTime.now(clock));
        seatOccupancyRepository.delete(occupancy);

        return StudySessionResponse.from(session);
    }

    private StudyChannel getActiveChannel(Long studyChannelId) {
        StudyChannel channel = studyChannelRepository.findById(studyChannelId)
                .filter(StudyChannel::isActive)
                .orElseThrow(() -> new BusinessException(
                        "STUDY_CHANNEL_NOT_FOUND", "이용 가능한 스터디 채널을 찾을 수 없습니다", HttpStatus.NOT_FOUND));
        if (!channel.getStudyRoom().isActive()) {
            throw new BusinessException(
                    "STUDY_ROOM_INACTIVE", "현재 이용할 수 없는 스터디 공간입니다", HttpStatus.CONFLICT);
        }
        return channel;
    }

    private Member getCurrentMember(String email) {
        if (email == null || email.isBlank()) {
            throw new BusinessException("UNAUTHORIZED", "로그인이 필요합니다", HttpStatus.UNAUTHORIZED);
        }
        return memberRepository.findByEmail(email)
                .filter(member -> !member.isDeleted())
                .orElseThrow(() -> new BusinessException(
                        "MEMBER_NOT_FOUND", "회원 정보를 찾을 수 없습니다", HttpStatus.NOT_FOUND));
    }

    private String normalizeSubject(String subject) {
        return subject == null || subject.isBlank() ? DEFAULT_SUBJECT : subject.trim();
    }
}
