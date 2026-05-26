package kr.devslab.kit.admin;

import java.time.Instant;
import java.util.List;

public record ApiError(int status, String error, String message, List<String> details, Instant timestamp) {

    public static ApiError of(int status, String error, String message) {
        return new ApiError(status, error, message, List.of(), Instant.now());
    }

    public static ApiError of(int status, String error, String message, List<String> details) {
        return new ApiError(status, error, message, details, Instant.now());
    }
}
