package com.gongsoop.voice.service;

import com.gongsoop.global.exception.BusinessException;
import com.gongsoop.member.entity.Member;
import com.gongsoop.member.repository.MemberRepository;
import com.gongsoop.studyspace.entity.Seat;
import com.gongsoop.studyspace.entity.SeatOccupancy;
import com.gongsoop.studyspace.entity.StudyZone;
import com.gongsoop.studyspace.repository.SeatOccupancyRepository;
import com.gongsoop.studyspace.repository.StudyZoneRepository;
import com.gongsoop.voice.dto.message.VoiceParticipantEvent;
import com.gongsoop.voice.dto.response.AvailableVoiceRoomResponse;
import com.gongsoop.voice.dto.response.VoiceJoinResponse;
import com.gongsoop.voice.dto.response.VoiceParticipantResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class VoiceRoomService {

    private static final int OFFICE_MAP_NO = 4;

    private final MemberRepository memberRepository;
    private final SeatOccupancyRepository seatOccupancyRepository;
    private final StudyZoneRepository studyZoneRepository;
    private final StringRedisTemplate redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    public VoiceRoomService(
            MemberRepository memberRepository,
            SeatOccupancyRepository seatOccupancyRepository,
            StudyZoneRepository studyZoneRepository,
            StringRedisTemplate redisTemplate,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.memberRepository = memberRepository;
        this.seatOccupancyRepository = seatOccupancyRepository;
        this.studyZoneRepository = studyZoneRepository;
        this.redisTemplate = redisTemplate;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional(readOnly = true)
    public AvailableVoiceRoomResponse getMyAvailableVoiceRoom(String email) {
        SeatOccupancy occupancy = getMyOccupancy(email);
        Seat seat = occupancy.getSeat();
        StudyZone zone = seat.getStudyZone();

        if (zone == null) {
            return null;
        }

        if (!Integer.valueOf(OFFICE_MAP_NO).equals(seat.getStudyRoom().getMapNo())) {
            return null;
        }

        if (!zone.isActive() || !zone.isVoiceEnabled()) {
            return null;
        }

        return new AvailableVoiceRoomResponse(
                zone.getId(),
                zone.getZoneCode(),
                zone.getZoneName(),
                seat.getStudyRoom().getId(),
                seat.getStudyRoom().getMapNo(),
                occupancy.getStudyChannel().getId(),
                occupancy.getStudyChannel().getChannelNo(),
                seat.getId(),
                seat.getSeatNo(),
                true
        );
    }

    @Transactional(readOnly = true)
    public VoiceJoinResponse join(Long studyZoneId, String email) {
        Member member = getCurrentMember(email);
        StudyZone zone = validateJoinableZone(studyZoneId, member);

        String key = participantsKey(studyZoneId);
        redisTemplate.opsForSet().add(key, email);

        VoiceParticipantEvent event = VoiceParticipantEvent.joined(
                studyZoneId,
                email,
                member.getNickname()
        );

        messagingTemplate.convertAndSend(
                "/topic/voice/zones/" + studyZoneId + "/participants",
                event
        );

        return new VoiceJoinResponse(
                studyZoneId,
                zone.getZoneName(),
                getParticipants(studyZoneId, email)
        );
    }

    @Transactional(readOnly = true)
    public void leave(Long studyZoneId, String email) {
        Member member = getCurrentMember(email);

        redisTemplate.opsForSet().remove(participantsKey(studyZoneId), email);

        VoiceParticipantEvent event = VoiceParticipantEvent.left(
                studyZoneId,
                email,
                member.getNickname()
        );

        messagingTemplate.convertAndSend(
                "/topic/voice/zones/" + studyZoneId + "/participants",
                event
        );
    }

    @Transactional(readOnly = true)
    public void validateSignal(Long studyZoneId, String email) {
        Member member = getCurrentMember(email);
        validateJoinableZone(studyZoneId, member);
    }

    @Transactional(readOnly = true)
    public List<VoiceParticipantResponse> getParticipants(Long studyZoneId, String excludeEmail) {
        var emails = redisTemplate.opsForSet().members(participantsKey(studyZoneId));
        if (emails == null || emails.isEmpty()) {
            return List.of();
        }

        List<VoiceParticipantResponse> result = new ArrayList<>();

        for (String email : emails) {
            if (excludeEmail != null && excludeEmail.equals(email)) {
                continue;
            }

            memberRepository.findByEmail(email)
                    .filter(member -> !member.isDeleted())
                    .ifPresent(member -> result.add(
                            new VoiceParticipantResponse(member.getEmail(), member.getNickname())
                    ));
        }

        return result;
    }

    private StudyZone validateJoinableZone(Long studyZoneId, Member member) {
        SeatOccupancy occupancy = seatOccupancyRepository.findByMember_Id(member.getId())
                .orElseThrow(() -> new BusinessException(
                        "VOICE_SEAT_REQUIRED",
                        "오피스 좌석에 착석한 사용자만 음성채팅을 사용할 수 있습니다",
                        HttpStatus.CONFLICT
                ));

        Seat seat = occupancy.getSeat();

        if (!Integer.valueOf(OFFICE_MAP_NO).equals(seat.getStudyRoom().getMapNo())) {
            throw new BusinessException(
                    "VOICE_ONLY_OFFICE",
                    "오피스 맵에서만 음성채팅을 사용할 수 있습니다",
                    HttpStatus.CONFLICT
            );
        }

        StudyZone seatZone = seat.getStudyZone();

        if (seatZone == null || !seatZone.getId().equals(studyZoneId)) {
            throw new BusinessException(
                    "VOICE_ZONE_MISMATCH",
                    "자신이 착석한 구역의 음성방만 사용할 수 있습니다",
                    HttpStatus.FORBIDDEN
            );
        }

        StudyZone zone = studyZoneRepository.findById(studyZoneId)
                .filter(StudyZone::isActive)
                .orElseThrow(() -> new BusinessException(
                        "VOICE_ZONE_NOT_FOUND",
                        "사용 가능한 음성 구역을 찾을 수 없습니다",
                        HttpStatus.NOT_FOUND
                ));

        if (!zone.isVoiceEnabled()) {
            throw new BusinessException(
                    "VOICE_DISABLED",
                    "현재 이 구역은 음성채팅을 사용할 수 없습니다",
                    HttpStatus.CONFLICT
            );
        }

        return zone;
    }

    private SeatOccupancy getMyOccupancy(String email) {
        Member member = getCurrentMember(email);

        return seatOccupancyRepository.findByMember_Id(member.getId())
                .orElseThrow(() -> new BusinessException(
                        "ACTIVE_OCCUPANCY_NOT_FOUND",
                        "현재 착석 중인 좌석을 찾을 수 없습니다",
                        HttpStatus.NOT_FOUND
                ));
    }

    private Member getCurrentMember(String email) {
        if (email == null || email.isBlank()) {
            throw new BusinessException(
                    "UNAUTHORIZED",
                    "로그인이 필요합니다",
                    HttpStatus.UNAUTHORIZED
            );
        }

        return memberRepository.findByEmail(email)
                .filter(member -> !member.isDeleted())
                .orElseThrow(() -> new BusinessException(
                        "MEMBER_NOT_FOUND",
                        "회원 정보를 찾을 수 없습니다",
                        HttpStatus.NOT_FOUND
                ));
    }

    private String participantsKey(Long studyZoneId) {
        return "voice:zone:" + studyZoneId + ":participants";
    }
}