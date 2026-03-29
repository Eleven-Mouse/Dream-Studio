package blog.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * 通用响应模型，兼容 code/message/data/timestamp/requestId
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private Integer code;
    private String message;
    private T data;
    private long timestamp;
    private String requestId;

    public static <T> ApiResponse<T> success(T data) {
        return success(200, "success", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return success(200, message, data);
    }

    public static <T> ApiResponse<T> success(int code, String message, T data) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .data(data)
                .timestamp(Instant.now().toEpochMilli())
                .requestId(UUID.randomUUID().toString())
                .build();
    }

    public static <T> ApiResponse<T> failure(int code, String message) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .timestamp(Instant.now().toEpochMilli())
                .requestId(UUID.randomUUID().toString())
                .build();
    }

    public static <T> ApiResponse<T> failure(String message) {
        return failure(500, message);
    }
}
