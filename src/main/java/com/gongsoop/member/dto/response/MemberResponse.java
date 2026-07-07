package com.gongsoop.member.dto.response;

import com.gongsoop.member.entity.Member;

import java.time.LocalDate;

public record MemberResponse(
        Long memberId,
        String name,
        LocalDate birthdate,
        String email
) {

    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getName(),
                member.getBirthdate(),
                member.getEmail()
        );
    }
}