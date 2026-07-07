package com.gongsoop.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        T data,
        String message,
        String code
) {

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, data, message, null);
    }

    public static ApiResponse<Void> success(String message) {
        return new ApiResponse<>(true, null, message, null);
    }

    public static <T> ApiResponse<T> failure(String code, String message, T data) {
        return new ApiResponse<>(false, data, message, code);
    }

    public static ApiResponse<Void> failure(String code, String message) {
        return new ApiResponse<>(false, null, message, code);
    }

    public static ApiResponse<Void> failure(String message) {
        return failure("BAD_REQUEST", message);
    }
}
