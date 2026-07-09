package com.gongsoop.notice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NoticeUpdateRequest(

        @NotBlank(message = "공지사항 제목을 입력해주세요")
        @Size(max = 200, message = "공지사항 제목은 200자 이하로 입력해주세요")
        String title,

        @NotBlank(message = "공지사항 내용을 입력해주세요")
        String content,

        @NotNull(message = "상단 고정 여부를 입력해주세요")
        Boolean isPinned,

        @NotNull(message = "공개 여부를 입력해주세요")
        Boolean isPublished
) {
}