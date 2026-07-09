package com.gongsoop.notice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NoticeCreateRequest(

        @NotBlank(message = "공지사항 제목을 입력해주세요")
        @Size(max = 200, message = "공지사항 제목은 200자 이하로 입력해주세요")
        String title,

        @NotBlank(message = "공지사항 내용을 입력해주세요")
        String content,

        Boolean isPinned,

        Boolean isPublished
) {
}