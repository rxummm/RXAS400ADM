package com.rxas400adm.common.response;

import lombok.Data;

/**
 * 统一返回结构：{ "code": 0, "message": "success", "data": {} }
 */
@Data
public class ApiResponse<T> {

    /** 0 表示成功，非 0 表示业务错误码 */
    private int code;
    private String message;
    private T data;

    public static <T> ApiResponse<T> success() {
        return success(null);
    }

    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> r = new ApiResponse<>();
        r.code = 0;
        r.message = "success";
        r.data = data;
        return r;
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        ApiResponse<T> r = new ApiResponse<>();
        r.code = code;
        r.message = message;
        return r;
    }

    public static <T> ApiResponse<T> error(String message) {
        return error(500, message);
    }
}
