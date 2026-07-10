package com.gongsoop.inquiry.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InquiryUpdateRequest(

        @NotBlank(message = "문의 제목을 입력해주세요")
        @Size(max = 200, message = "문의 제목은 200자 이하로 입력해주세요")
        String title,

        @NotBlank(message = "문의 내용을 입력해주세요")
        String content,

        Boolean isSecret
) {
}
