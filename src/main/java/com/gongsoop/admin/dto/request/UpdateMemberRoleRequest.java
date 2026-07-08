package com.gongsoop.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateMemberRoleRequest(
        @NotBlank(message = "회원 권한을 입력해주세요")
        @Pattern(regexp = "USER|ADMIN", message = "권한은 USER 또는 ADMIN만 가능합니다")
        String userRole
) {
}