package com.gongsoop.studyspace.service;

import com.gongsoop.global.exception.BusinessException;
import com.gongsoop.studyspace.dto.response.SeatStatusResponse;
import com.gongsoop.studyspace.dto.response.StudyChannelResponse;
import com.gongsoop.studyspace.dto.response.StudyRoomResponse;
import com.gongsoop.studyspace.entity.SeatOccupancy;
import com.gongsoop.studyspace.entity.StudyChannel;
import com.gongsoop.studyspace.repository.SeatOccupancyRepository;
import com.gongsoop.studyspace.repository.SeatRepository;
import com.gongsoop.studyspace.repository.StudyChannelRepository;
import com.gongsoop.studyspace.repository.StudyRoomRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class StudySpaceQueryService {

    private final StudyRoomRepository studyRoomRepository;
    private final StudyChannelRepository studyChannelRepository;
    private final SeatRepository seatRepository;
    private final SeatOccupancyRepository seatOccupancyRepository;

    public StudySpaceQueryService(
            StudyRoomRepository studyRoomRepository,
            StudyChannelRepository studyChannelRepository,
            SeatRepository seatRepository,
            SeatOccupancyRepository seatOccupancyRepository
    ) {
        this.studyRoomRepository = studyRoomRepository;
        this.studyChannelRepository = studyChannelRepository;
        this.seatRepository = seatRepository;
        this.seatOccupancyRepository = seatOccupancyRepository;
    }

    public List<StudyRoomResponse> getActiveRooms() {
        return studyRoomRepository.findAllByActiveTrueOrderByMapNoAsc()
                .stream()
                .map(StudyRoomResponse::from)
                .toList();
    }

    public List<StudyChannelResponse> getActiveChannels(Long studyRoomId) {
        requireActiveRoom(studyRoomId);

        return studyChannelRepository
                .findAllByStudyRoom_IdAndActiveTrueOrderByChannelNoAsc(studyRoomId)
                .stream()
                .map(StudyChannelResponse::from)
                .toList();
    }

    public List<SeatStatusResponse> getSeatStatuses(Long studyChannelId) {
        StudyChannel channel = studyChannelRepository.findById(studyChannelId)
                .filter(StudyChannel::isActive)
                .orElseThrow(() -> new BusinessException(
                        "STUDY_CHANNEL_NOT_FOUND",
                        "이용 가능한 스터디 채널을 찾을 수 없습니다",
                        HttpStatus.NOT_FOUND
                ));

        if (!channel.getStudyRoom().isActive()) {
            throw new BusinessException(
                    "STUDY_ROOM_INACTIVE",
                    "현재 이용할 수 없는 스터디 공간입니다",
                    HttpStatus.CONFLICT
            );
        }

        Set<Long> occupiedSeatIds = seatOccupancyRepository.findAllByStudyChannel_Id(studyChannelId)
                .stream()
                .map(SeatOccupancy::getSeat)
                .map(seat -> seat.getId())
                .collect(Collectors.toSet());

        return seatRepository.findAllByStudyRoom_IdOrderBySeatNoAsc(channel.getStudyRoom().getId())
                .stream()
                .map(seat -> SeatStatusResponse.of(seat, occupiedSeatIds.contains(seat.getId())))
                .toList();
    }

    private void requireActiveRoom(Long studyRoomId) {
        studyRoomRepository.findById(studyRoomId)
                .filter(room -> room.isActive())
                .orElseThrow(() -> new BusinessException(
                        "STUDY_ROOM_NOT_FOUND",
                        "이용 가능한 스터디 공간을 찾을 수 없습니다",
                        HttpStatus.NOT_FOUND
                ));
    }
}
