package com.bloodconnect.dto;

import java.time.Instant;
import java.util.List;

public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private List<String> errors;
    private String timestamp;

    public ApiResponse() {
        this.timestamp = Instant.now().toString();
    }

    public ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.timestamp = Instant.now().toString();
    }

    public ApiResponse(boolean success, String message, List<String> errors) {
        this.success = success;
        this.message = message;
        this.errors = errors;
        this.timestamp = Instant.now().toString();
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    public static <T> ApiResponse<T> error(String message, List<String> errors) {
        return new ApiResponse<>(false, message, errors);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, (List<String>) null);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
