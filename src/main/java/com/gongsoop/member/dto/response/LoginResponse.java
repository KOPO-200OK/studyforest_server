package com.gongsoop.member.dto.response;

public record LoginResponse(
        String tokenType,
        String accessToken,
        String refreshToken,
        MemberResponse member
) {
}