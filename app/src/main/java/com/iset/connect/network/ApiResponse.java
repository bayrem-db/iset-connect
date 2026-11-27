package com.iset.connect.network;

import java.util.Map;

/** Standard JSON envelope returned by the PHP API. */
public class ApiResponse<T> {
    public boolean success;
    public String message;
    public T data;
    public Map<String, String> errors;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message == null ? "" : message; }
    public T getData() { return data; }

    /** First validation error message, if the API supplied one. */
    public String firstError() {
        if (errors == null || errors.isEmpty()) {
            return getMessage();
        }
        return errors.values().iterator().next();
    }
}
