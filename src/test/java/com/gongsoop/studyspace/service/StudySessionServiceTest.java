package com.gongsoop.studyspace.service;

import com.gongsoop.global.exception.BusinessException;
import com.gongsoop.member.entity.Member;
import com.gongsoop.member.repository.MemberRepository;
import com.gongsoop.studyspace.dto.request.OccupySeatRequest;
import com.gongsoop.studyspace.dto.response.StudySessionResponse;
import com.gongsoop.studyspace.entity.*;
import com.gongsoop.studyspace.config.StudySpaceRealtimeProperties;
import com.gongsoop.studyspace.repository.SeatOccupancyRepository;
import com.gongsoop.studyspace.repository.SeatRepository;
import com.gongsoop.studyspace.repository.StudyChannelRepository;
import com.gongsoop.studyspace.repository.StudySessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StudySessionServiceTest {

    private MemberRepository memberRepository;
    private StudyChannelRepository channelRepository;
    private SeatRepository seatRepository;
    private StudySessionRepository sessionRepository;
    private SeatOccupancyRepository occupancyRepository;
    private SeatHoldService seatHoldService;
    private PresenceService presenceService;
    private ApplicationEventPublisher eventPublisher;
    private StudySpaceRealtimeProperties properties;
    private Clock clock;
    private StudySessionService service;

    @BeforeEach
    void setUp() {
        memberRepository = mock(MemberRepository.class);
        channelRepository = mock(StudyChannelRepository.class);
        seatRepository = mock(SeatRepository.class);
        sessionRepository = mock(StudySessionRepository.class);
        occupancyRepository = mock(SeatOccupancyRepository.class);
        seatHoldService = mock(SeatHoldService.class);
        presenceService = mock(PresenceService.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        properties = new StudySpaceRealtimeProperties();
        // 선점 락은 기본적으로 성공한다고 가정(경합 테스트가 아닌 한).
        when(seatHoldService.tryHold(any(), any(), any())).thenReturn(Optional.of("token:7"));
        clock = Clock.fixed(Instant.parse("2026-07-08T06:05:00Z"), ZoneId.of("Asia/Seoul"));
        service = new StudySessionService(
                memberRepository, channelRepository, seatRepository,
                sessionRepository, occupancyRepository, seatHoldService,
                presenceService, eventPublisher, properties, clock);
    }

    @Test
    void createsRunningSessionAndOccupancyInOneServiceCall() {
        Member member = mock(Member.class);
        StudyRoom room = mock(StudyRoom.class);
        StudyChannel channel = mock(StudyChannel.class);
        Seat seat = mock(Seat.class);
        when(member.getId()).thenReturn(7L);
        when(room.getId()).thenReturn(1L);
        when(room.isActive()).thenReturn(true);
        when(channel.getId()).thenReturn(3L);
        when(channel.isActive()).thenReturn(true);
        when(channel.getStudyRoom()).thenReturn(room);
        when(seat.getId()).thenReturn(10L);
        when(seat.isActive()).thenReturn(true);
        when(seat.getStudyRoom()).thenReturn(room);
        when(memberRepository.findByEmail("user@test.com")).thenReturn(Optional.of(member));
        when(channelRepository.findById(3L)).thenReturn(Optional.of(channel));
        when(seatRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(seat));
        when(sessionRepository.save(any(StudySession.class))).thenAnswer(invocation -> {
            StudySession session = invocation.getArgument(0);
            ReflectionTestUtils.setField(session, "id", 30L);
            return session;
        });

        StudySessionResponse response = service.occupySeat(
                3L, 10L, new OccupySeatRequest(null), "user@test.com");

        assertThat(response.studySessionId()).isEqualTo(30L);
        assertThat(response.status()).isEqualTo(StudySessionStatus.RUNNING);
        assertThat(response.accumulatedSeconds()).isZero();
        verify(occupancyRepository).save(any(SeatOccupancy.class));
    }

    @Test
    void rejectsMemberWhoAlreadyOccupiesSeat() {
        Member member = mock(Member.class);
        StudyRoom room = mock(StudyRoom.class);
        StudyChannel channel = mock(StudyChannel.class);
        Seat seat = mock(Seat.class);
        when(member.getId()).thenReturn(7L);
        when(room.getId()).thenReturn(1L);
        when(room.isActive()).thenReturn(true);
        when(channel.isActive()).thenReturn(true);
        when(channel.getStudyRoom()).thenReturn(room);
        when(seat.isActive()).thenReturn(true);
        when(seat.getStudyRoom()).thenReturn(room);
        when(memberRepository.findByEmail("user@test.com")).thenReturn(Optional.of(member));
        when(channelRepository.findById(3L)).thenReturn(Optional.of(channel));
        when(seatRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(seat));
        when(occupancyRepository.existsByMember_Id(7L)).thenReturn(true);

        assertThatThrownBy(() -> service.occupySeat(
                3L, 10L, new OccupySeatRequest("한국사"), "user@test.com"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("이미 이용 중인 좌석이 있습니다");
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void completesSessionAndReleasesOccupancy() {
        Member member = mock(Member.class);
        StudyRoom room = mock(StudyRoom.class);
        StudyChannel channel = mock(StudyChannel.class);
        Seat seat = mock(Seat.class);
        when(member.getId()).thenReturn(7L);
        when(channel.getId()).thenReturn(3L);
        when(seat.getId()).thenReturn(10L);
        StudySession session = StudySession.start(
                member, channel, seat, "한국사", LocalDateTime.of(2026, 7, 8, 15, 0));
        ReflectionTestUtils.setField(session, "id", 30L);
        SeatOccupancy occupancy = SeatOccupancy.occupy(
                channel, seat, member, session, LocalDateTime.of(2026, 7, 8, 15, 0));
        when(memberRepository.findByEmail("user@test.com")).thenReturn(Optional.of(member));
        when(occupancyRepository.findByStudySession_IdAndMember_Id(30L, 7L))
                .thenReturn(Optional.of(occupancy));

        StudySessionResponse response = service.leaveSeat(30L, "user@test.com");

        assertThat(response.status()).isEqualTo(StudySessionStatus.COMPLETED);
        assertThat(response.accumulatedSeconds()).isEqualTo(300L);
        assertThat(session.getEndReason()).isEqualTo(StudySessionEndReason.USER_EXIT);
        verify(occupancyRepository).delete(occupancy);
    }
}
