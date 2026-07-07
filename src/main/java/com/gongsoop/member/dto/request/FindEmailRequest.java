package com.gongsoop.member.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;

public record FindEmailRequest(
        @NotBlank(message = "이름을 입력해주세요")
        String name,

        @NotNull(message = "생년월일을 입력해주세요")
        @Past(message = "유효하지 않은 생년월일입니다")
        LocalDate birthdate
) {
}
