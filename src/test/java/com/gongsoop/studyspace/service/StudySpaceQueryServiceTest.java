package com.gongsoop.studyspace.service;

import com.gongsoop.global.exception.BusinessException;
import com.gongsoop.studyspace.dto.response.SeatStatusResponse;
import com.gongsoop.studyspace.entity.Seat;
import com.gongsoop.studyspace.entity.SeatOccupancy;
import com.gongsoop.studyspace.entity.StudyChannel;
import com.gongsoop.studyspace.entity.StudyRoom;
import com.gongsoop.studyspace.repository.SeatOccupancyRepository;
import com.gongsoop.studyspace.repository.SeatRepository;
import com.gongsoop.studyspace.repository.StudyChannelRepository;
import com.gongsoop.studyspace.repository.StudyRoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StudySpaceQueryServiceTest {

    private StudyRoomRepository studyRoomRepository;
    private StudyChannelRepository studyChannelRepository;
    private SeatRepository seatRepository;
    private SeatOccupancyRepository seatOccupancyRepository;
    private StudySpaceQueryService service;

    @BeforeEach
    void setUp() {
        studyRoomRepository = mock(StudyRoomRepository.class);
        studyChannelRepository = mock(StudyChannelRepository.class);
        seatRepository = mock(SeatRepository.class);
        seatOccupancyRepository = mock(SeatOccupancyRepository.class);
        service = new StudySpaceQueryService(
                studyRoomRepository,
                studyChannelRepository,
                seatRepository,
                seatOccupancyRepository
        );
    }

    @Test
    void returnsSeatStatusesForChannelRoom() {
        StudyRoom room = mock(StudyRoom.class);
        StudyChannel channel = mock(StudyChannel.class);
        Seat occupiedSeat = mock(Seat.class);
        Seat availableSeat = mock(Seat.class);
        SeatOccupancy occupancy = mock(SeatOccupancy.class);

        when(room.getId()).thenReturn(1L);
        when(room.isActive()).thenReturn(true);
        when(channel.isActive()).thenReturn(true);
        when(channel.getStudyRoom()).thenReturn(room);
        when(occupiedSeat.getId()).thenReturn(10L);
        when(occupiedSeat.getSeatNo()).thenReturn(1);
        when(occupiedSeat.isActive()).thenReturn(true);
        when(availableSeat.getId()).thenReturn(11L);
        when(availableSeat.getSeatNo()).thenReturn(2);
        when(availableSeat.isActive()).thenReturn(true);
        when(occupancy.getSeat()).thenReturn(occupiedSeat);
        when(studyChannelRepository.findById(3L)).thenReturn(Optional.of(channel));
        when(seatOccupancyRepository.findAllByStudyChannel_Id(3L)).thenReturn(List.of(occupancy));
        when(seatRepository.findAllByStudyRoom_IdOrderBySeatNoAsc(1L))
                .thenReturn(List.of(occupiedSeat, availableSeat));

        List<SeatStatusResponse> result = service.getSeatStatuses(3L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).occupied()).isTrue();
        assertThat(result.get(1).occupied()).isFalse();
    }

    @Test
    void rejectsInactiveRoomWhenReadingSeats() {
        StudyRoom room = mock(StudyRoom.class);
        StudyChannel channel = mock(StudyChannel.class);

        when(room.isActive()).thenReturn(false);
        when(channel.isActive()).thenReturn(true);
        when(channel.getStudyRoom()).thenReturn(room);
        when(studyChannelRepository.findById(3L)).thenReturn(Optional.of(channel));

        assertThatThrownBy(() -> service.getSeatStatuses(3L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("현재 이용할 수 없는 스터디 공간입니다");
    }
}
