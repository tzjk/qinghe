package com.qinghe.life.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    private Integer code;
    private String message;
    private T data;

    public static <T> Result<T> success(T data) {
        return new Result<T>(200, "success", data);
    }

    public static Result<Void> success() {
        return new Result<Void>(200, "success", null);
    }

    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<T>(code, message, null);
    }
}
