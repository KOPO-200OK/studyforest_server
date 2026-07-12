package com.gongsoop.member.dto.response;

import com.gongsoop.member.entity.Member;

import java.time.LocalDate;

public record MemberResponse(
        Long memberId,
        String name,
        String nickname,
        LocalDate birthdate,
        String email,
        String userRole,
        Integer characterId
) {
    public static MemberResponse from(
            Member member
    ) {
        return new MemberResponse(
                member.getId(),
                member.getName(),
                member.getNickname(),
                member.getBirthdate(),
                member.getEmail(),
                member.getUserRole().name(),
                member.getCharacterId()
        );
    }
}