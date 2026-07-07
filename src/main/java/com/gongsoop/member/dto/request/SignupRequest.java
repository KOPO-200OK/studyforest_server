package com.gongsoop.member.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record SignupRequest(
        @Size(max = 100, message = "이름은 100자 이하로 입력해주세요")
        String name,

        @Past(message = "유효하지 않은 생년월일입니다")
        LocalDate birthdate,

        @NotBlank(message = "이메일을 입력해주세요")
        @Email(message = "이메일 형식이 올바르지 않습니다")
        String email,

        @NotBlank(message = "비밀번호를 입력해주세요")
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*[^a-zA-Z0-9]).{8,}$",
                message = "비밀번호는 8자 이상이며, 대문자와 특수문자를 각 1개 이상 포함해야 합니다"
        )
        String password
) {
}